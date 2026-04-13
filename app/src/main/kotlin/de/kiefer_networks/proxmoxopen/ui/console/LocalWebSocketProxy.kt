package de.kiefer_networks.proxmoxopen.ui.console

import android.content.Context
import timber.log.Timber
import java.io.BufferedOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.net.URI
import java.security.SecureRandom
import java.util.concurrent.atomic.AtomicBoolean
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

/**
 * Transparent TCP proxy that bridges WebView to Proxmox WebSocket.
 *
 * Instead of implementing WebSocket framing, this proxy:
 * 1. Serves HTML/JS/CSS assets over HTTP
 * 2. For WebSocket upgrades: rewrites the HTTP upgrade request (adds auth, changes path),
 *    forwards it to Proxmox over TLS, and then bridges raw bytes bidirectionally.
 *
 * The WebSocket handshake happens end-to-end between Chrome and Proxmox.
 */
class LocalWebSocketProxy(
    private val targetUrl: String,
    private val cookie: String,
    private val fingerprint: String?,
    private val context: Context,
    private val assetDir: String,
    private val indexFile: String,
    private val isTerminal: Boolean = true,
) {
    @Volatile private var serverSocket: ServerSocket? = null
    private var running = AtomicBoolean(false)
    private val activeSockets = java.util.Collections.synchronizedList(mutableListOf<Socket>())
    var localPort: Int = 0
        private set

    fun start(): Int {
        serverSocket = ServerSocket(0, 5, java.net.InetAddress.getByName("127.0.0.1"))
        localPort = serverSocket!!.localPort
        running.set(true)

        Thread({
            try {
                while (running.get()) {
                    val client = serverSocket!!.accept()
                    Thread({
                        try {
                            handleConnection(client)
                        } catch (e: Exception) {
                            if (running.get()) Timber.e("handler error: %s", e.localizedMessage)
                        }
                    }, "WsProxy-Handler").start()
                }
            } catch (e: IOException) {
                if (running.get()) Timber.e("accept error: %s", e.localizedMessage)
            }
        }, "WsProxy-Accept").start()

        return localPort
    }

    fun stop() {
        running.set(false)
        synchronized(activeSockets) {
            activeSockets.forEach { try { it.close() } catch (_: Exception) {} }
            activeSockets.clear()
        }
        try { serverSocket?.close() } catch (_: Exception) {}
    }

    private fun handleConnection(client: Socket) {
        val input = client.getInputStream()
        val output = client.getOutputStream()

        // Read HTTP request headers (byte-by-byte to not over-read)
        val headerBytes = readUntilHeaderEnd(input) ?: run { client.close(); return }
        val request = String(headerBytes)

        val isUpgrade = request.contains("Upgrade: websocket", ignoreCase = true)

        if (isUpgrade) {
            handleWebSocketUpgrade(client, input, output, request)
        } else {
            val firstLine = request.lineSequence().firstOrNull() ?: ""
            handleHttpRequest(client, output, firstLine)
        }
    }

    /** Read bytes until \r\n\r\n (end of HTTP headers), without consuming body/frame data */
    private fun readUntilHeaderEnd(input: InputStream): ByteArray? {
        val buf = java.io.ByteArrayOutputStream(1024)
        var state = 0 // 0=normal, 1=\r, 2=\r\n, 3=\r\n\r
        while (true) {
            val b = input.read()
            if (b < 0) return null
            buf.write(b)
            if (buf.size() > 16384) return null
            state = when {
                b == '\r'.code && (state == 0 || state == 2) -> state + 1
                b == '\n'.code && state == 1 -> 2
                b == '\n'.code && state == 3 -> 4
                else -> 0
            }
            if (state == 4) break
        }
        return buf.toByteArray()
    }

    private fun handleHttpRequest(client: Socket, output: OutputStream, requestLine: String) {
        val parts = requestLine.split(" ")
        val rawPath = if (parts.size >= 2) parts[1] else "/"
        val path = rawPath.substringBefore("?")
        if (path.contains("..")) {
            val body = "403 Forbidden"
            val headers = "HTTP/1.1 403 Forbidden\r\nContent-Length: ${body.length}\r\nConnection: close\r\n\r\n"
            output.write(headers.toByteArray())
            output.write(body.toByteArray())
            output.flush()
            client.close()
            return
        }

        val assetPath = when {
            path == "/" || path == "/$indexFile" -> "$assetDir/$indexFile"
            path.startsWith("/") -> "$assetDir${path}"
            else -> "$assetDir/$path"
        }

        try {
            val data = context.assets.open(assetPath).use { it.readBytes() }
            val contentType = when {
                assetPath.endsWith(".html") -> "text/html; charset=utf-8"
                assetPath.endsWith(".js") -> "application/javascript; charset=utf-8"
                assetPath.endsWith(".css") -> "text/css; charset=utf-8"
                assetPath.endsWith(".png") -> "image/png"
                assetPath.endsWith(".svg") -> "image/svg+xml"
                else -> "application/octet-stream"
            }
            val headers = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: $contentType\r\n" +
                "Content-Length: ${data.size}\r\n" +
                "Connection: close\r\n" +
                "\r\n"
            output.write(headers.toByteArray())
            output.write(data)
            output.flush()
        } catch (e: Exception) {
            val body = "404 Not Found"
            val headers = "HTTP/1.1 404 Not Found\r\n" +
                "Content-Length: ${body.length}\r\n" +
                "Connection: close\r\n" +
                "\r\n"
            output.write(headers.toByteArray())
            output.write(body.toByteArray())
            output.flush()
        } finally {
            try { client.close() } catch (_: Exception) {}
        }
    }

    private fun handleWebSocketUpgrade(client: Socket, clientIn: InputStream, clientOut: OutputStream, originalRequest: String) {
        activeSockets.add(client)

        // Validate cookie value to prevent header injection
        if (cookie.contains("\r") || cookie.contains("\n")) {
            Timber.e("Cookie contains invalid characters, rejecting")
            client.close()
            activeSockets.remove(client)
            return
        }

        val uri = URI(targetUrl)
        val host = uri.host
        val port = if (uri.port > 0) uri.port else 443
        val pathAndQuery = if (uri.rawQuery != null) "${uri.rawPath}?${uri.rawQuery}" else uri.rawPath

        // Rewrite the HTTP upgrade request for Proxmox
        val rewrittenRequest = buildString {
            val lines = originalRequest.trimEnd().split("\r\n")
            // Replace first line with upstream path
            append("GET $pathAndQuery HTTP/1.1\r\n")
            // Replace Host, add Cookie, keep other headers
            var hasCookie = false
            for (i in 1 until lines.size) {
                val line = lines[i]
                when {
                    line.startsWith("Host:", ignoreCase = true) -> append("Host: $host:$port\r\n")
                    line.startsWith("Origin:", ignoreCase = true) -> continue // drop Origin
                    line.startsWith("Cookie:", ignoreCase = true) -> {
                        append("Cookie: PVEAuthCookie=<redacted>\r\n".replace("<redacted>", cookie))
                        hasCookie = true
                    }
                    else -> append("$line\r\n")
                }
            }
            if (!hasCookie) append("Cookie: PVEAuthCookie=$cookie\r\n")
            // Proxmox checks Referer for xtermjs=1 to decide text vs RFB mode
            if (isTerminal) append("Referer: https://$host:$port/?xtermjs=1\r\n")
            append("\r\n")
        }

        // Connect upstream TLS socket
        val trustManager = de.kiefer_networks.proxmoxopen.data.api.tls.TofuTrustManager(fingerprint)
        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, arrayOf<X509TrustManager>(trustManager), SecureRandom())
        }
        val rawSocket = sslContext.socketFactory.createSocket(host, port) as javax.net.ssl.SSLSocket
        rawSocket.soTimeout = 120_000
        activeSockets.add(rawSocket)

        val upIn = rawSocket.getInputStream()
        val upOut = BufferedOutputStream(rawSocket.getOutputStream())

        // Send rewritten upgrade to Proxmox
        upOut.write(rewrittenRequest.toByteArray())
        upOut.flush()

        // Read Proxmox's HTTP response headers and forward to client
        val responseHeaders = readUntilHeaderEnd(upIn)
        if (responseHeaders == null) {
            Timber.e("No response from upstream")
            client.close(); rawSocket.close(); return
        }
        val responseStr = String(responseHeaders)

        // Forward the response to client as-is
        clientOut.write(responseHeaders)
        clientOut.flush()

        Timber.d("Upstream response status: %s", responseStr.lineSequence().firstOrNull())
        if (!responseStr.contains("101")) {
            Timber.e("Upstream upgrade failed (non-101 response)")
            client.close(); rawSocket.close(); return
        }

        // Bidirectional raw byte forwarding (no WebSocket frame parsing)
        val upToClient = Thread({
            try {
                val buf = ByteArray(8192)
                while (running.get()) {
                    val n = upIn.read(buf)
                    if (n < 0) break
                    clientOut.write(buf, 0, n)
                    clientOut.flush()
                }
            } catch (e: SocketTimeoutException) {
                Timber.e("up->client socket timeout")
            } catch (e: Exception) {
                if (running.get()) Timber.e("up->client error: %s", e.localizedMessage)
            } finally {
                try { client.close() } catch (_: Exception) {}
                try { rawSocket.close() } catch (_: Exception) {}
                activeSockets.remove(client)
                activeSockets.remove(rawSocket)
            }
        }, "WsProxy-UpToClient")

        val clientToUp = Thread({
            try {
                val buf = ByteArray(8192)
                while (running.get()) {
                    val n = clientIn.read(buf)
                    if (n < 0) break
                    upOut.write(buf, 0, n)
                    upOut.flush()
                }
            } catch (e: SocketTimeoutException) {
                Timber.e("client->up socket timeout")
            } catch (e: Exception) {
                if (running.get()) Timber.e("client->up error: %s", e.localizedMessage)
            } finally {
                try { rawSocket.close() } catch (_: Exception) {}
                try { client.close() } catch (_: Exception) {}
                activeSockets.remove(rawSocket)
                activeSockets.remove(client)
            }
        }, "WsProxy-ClientToUp")

        upToClient.start()
        clientToUp.start()
    }
}
