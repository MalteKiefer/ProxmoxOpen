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
import java.net.URLDecoder
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
 *
 * All requests must include a per-instance random session-secret prefix as the first
 * path segment, e.g. `/<sessionSecret>/console.html`. This prevents other apps that
 * happen to discover the loopback port from accessing the proxy.
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

    /**
     * Per-instance random secret that must appear as the first path segment of every
     * incoming HTTP/WS request. Generated once at construction with SecureRandom.
     * 32 random bytes => 64 lowercase hex characters.
     *
     * NEVER log this value.
     */
    val sessionSecret: String = run {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        val sb = StringBuilder(64)
        for (b in bytes) {
            val v = b.toInt() and 0xFF
            sb.append(HEX[v ushr 4])
            sb.append(HEX[v and 0x0F])
        }
        sb.toString()
    }

    private val secretPrefix: String = "/$sessionSecret"

    /** Static allowlist for terminal (xterm) HTTP asset serving. */
    private val terminalAllowed = setOf(
        "/", "/terminal.html", "/xterm.css",
        "/xterm.min.js", "/xterm-addon-fit.min.js", "/LICENSE",
    )

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

        // Parse request line: METHOD <path> HTTP/<ver>
        val firstLine = request.lineSequence().firstOrNull().orEmpty()
        val parts = firstLine.split(" ")
        val rawPath = if (parts.size >= 2) parts[1] else "/"
        val pathOnly = rawPath.substringBefore("?")

        // F-001: enforce session-secret prefix on EVERY request (HTTP + WS upgrade).
        // The secret must appear as the first path segment, followed by '/' or end-of-string.
        if (!(pathOnly == secretPrefix || pathOnly.startsWith("$secretPrefix/"))) {
            try {
                output.write("HTTP/1.1 401 Unauthorized\r\nConnection: close\r\n\r\n".toByteArray())
                output.flush()
            } catch (_: Exception) {}
            try { client.close() } catch (_: Exception) {}
            return
        }

        // Strip the session-secret prefix; downstream code only sees the asset/WS path.
        val strippedPath = pathOnly.removePrefix(secretPrefix).ifEmpty { "/" }

        // F-016: URL-decode and validate.
        val decodedPath = try {
            URLDecoder.decode(strippedPath, "UTF-8")
        } catch (_: IllegalArgumentException) {
            try {
                output.write("HTTP/1.1 400 Bad Request\r\nConnection: close\r\n\r\n".toByteArray())
                output.flush()
            } catch (_: Exception) {}
            try { client.close() } catch (_: Exception) {}
            return
        }

        if (!decodedPath.startsWith("/") ||
            decodedPath.contains("..") ||
            decodedPath.contains("\\") ||
            decodedPath.contains(" ")
        ) {
            try {
                output.write("HTTP/1.1 400 Bad Request\r\nConnection: close\r\n\r\n".toByteArray())
                output.flush()
            } catch (_: Exception) {}
            try { client.close() } catch (_: Exception) {}
            return
        }

        val isUpgrade = request.contains("Upgrade: websocket", ignoreCase = true)

        if (isUpgrade) {
            // For WS: rebuild request line with the stripped path before forwarding logic.
            // The upstream rewriter ignores the incoming path anyway, but we also need to make
            // sure no session secret leaks upstream.
            val rebuiltRequest = rebuildRequestWithPath(request, decodedPath)
            handleWebSocketUpgrade(client, input, output, rebuiltRequest)
        } else {
            // F-016: enforce static asset allowlist for HTTP serving.
            if (!isAssetPathAllowed(decodedPath)) {
                val body = "404 Not Found"
                val headers = "HTTP/1.1 404 Not Found\r\n" +
                    "Content-Length: ${body.length}\r\n" +
                    "Connection: close\r\n" +
                    "\r\n"
                try {
                    output.write(headers.toByteArray())
                    output.write(body.toByteArray())
                    output.flush()
                } catch (_: Exception) {}
                try { client.close() } catch (_: Exception) {}
                return
            }
            handleHttpRequest(client, output, decodedPath)
        }
    }

    private fun isAssetPathAllowed(decodedPath: String): Boolean {
        return if (isTerminal) {
            decodedPath in terminalAllowed
        } else {
            // noVNC: allow index ("/" or "/console.html"), LICENSE.txt, and core/vendor subtrees.
            decodedPath == "/" ||
                decodedPath == "/console.html" ||
                decodedPath == "/LICENSE.txt" ||
                decodedPath.startsWith("/core/") ||
                decodedPath.startsWith("/vendor/")
        }
    }

    /**
     * Build a new HTTP request string with the request line's path replaced by `newPath`.
     * Headers are preserved verbatim.
     */
    private fun rebuildRequestWithPath(originalRequest: String, newPath: String): String {
        val lines = originalRequest.split("\r\n")
        if (lines.isEmpty()) return originalRequest
        val firstLine = lines[0]
        val parts = firstLine.split(" ")
        val method = parts.getOrNull(0) ?: "GET"
        val version = parts.getOrNull(2) ?: "HTTP/1.1"
        val sb = StringBuilder()
        sb.append("$method $newPath $version\r\n")
        for (i in 1 until lines.size) {
            sb.append(lines[i])
            if (i < lines.size - 1) sb.append("\r\n")
        }
        return sb.toString()
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

    private fun handleHttpRequest(client: Socket, output: OutputStream, decodedPath: String) {
        // decodedPath has already been validated (no traversal, allowlisted) by handleConnection.
        val assetPath = when {
            decodedPath == "/" || decodedPath == "/$indexFile" -> "$assetDir/$indexFile"
            decodedPath.startsWith("/") -> "$assetDir${decodedPath}"
            else -> "$assetDir/$decodedPath"
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

        // Validate PVE ticket (cookie) against strict header-safe charset before sending upstream.
        // Proxmox tickets look like: PVE:user@realm:HEXHASH::BASE64PAYLOAD
        // Accept only printable ASCII, excluding ';' ',' CR LF SPACE and control chars
        // to prevent cookie/header-injection and attribute smuggling.
        if (!isHeaderSafeTicket(cookie)) {
            val preview = cookie.take(4)
            Timber.w("PVE ticket failed header-safe validation (len=%d, prefix=%s); aborting upstream", cookie.length, preview)
            try { client.close() } catch (_: Exception) {}
            activeSockets.remove(client)
            return
        }

        // Apply the same validation to any incoming CSRFPreventionToken header from the
        // client upgrade request; if present and malformed, refuse to forward.
        val csrfLine = originalRequest.lineSequence()
            .firstOrNull { it.startsWith("CSRFPreventionToken:", ignoreCase = true) }
        if (csrfLine != null) {
            val csrf = csrfLine.substringAfter(":").trim()
            if (!isHeaderSafeTicket(csrf)) {
                Timber.w("CSRF token failed header-safe validation (len=%d, prefix=%s); aborting upstream", csrf.length, csrf.take(4))
                try { client.close() } catch (_: Exception) {}
                activeSockets.remove(client)
                return
            }
        }

        val uri = URI(targetUrl)
        val host = uri.host
        val port = if (uri.port > 0) uri.port else 443
        val pathAndQuery = if (uri.rawQuery != null) "${uri.rawPath}?${uri.rawQuery}" else uri.rawPath

        // Rewrite the HTTP upgrade request for Proxmox.
        // The session secret is a local-loopback construct; it MUST NOT be forwarded upstream.
        // We rebuild from scratch using the upstream `pathAndQuery`.
        val rewrittenRequest = buildString {
            val lines = originalRequest.trimEnd().split("\r\n")
            // Replace first line with upstream path (drops any local /<secret>/... prefix)
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

    /**
     * Header-safe validator for PVE tickets / CSRF tokens.
     * Accept only printable ASCII (0x21..0x7E) and explicitly exclude ';' ',' which
     * could smuggle extra cookies/attributes. Rejects empty strings and anything with
     * CR, LF, SPACE, TAB, or other control characters.
     */
    private fun isHeaderSafeTicket(value: String): Boolean {
        if (value.isEmpty()) return false
        for (c in value) {
            val code = c.code
            if (code < 0x21 || code > 0x7E) return false
            if (c == ';' || c == ',') return false
        }
        return true
    }

    private companion object {
        private val HEX = charArrayOf('0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f')
    }
}
