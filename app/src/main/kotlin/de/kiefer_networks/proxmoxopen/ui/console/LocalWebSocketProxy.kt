package de.kiefer_networks.proxmoxopen.ui.console

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.io.IOException
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

/**
 * Local WebSocket proxy that bridges:
 *   WebView (ws://localhost:{port}) ←→ Proxmox (wss://{host}:{port}, self-signed OK)
 *
 * The proxy accepts a raw WebSocket connection on localhost (no TLS),
 * and forwards all frames bidirectionally to the Proxmox WebSocket
 * endpoint using OkHttp (which supports TOFU TLS pinning).
 */
class LocalWebSocketProxy(
    private val targetUrl: String,
    private val cookie: String,
    private val fingerprint: String?,
) {
    private var serverSocket: ServerSocket? = null
    private var running = AtomicBoolean(false)
    private var upstreamWs: WebSocket? = null
    private var clientSocket: java.net.Socket? = null
    var localPort: Int = 0
        private set

    fun start(): Int {
        serverSocket = ServerSocket(0, 1, java.net.InetAddress.getByName("127.0.0.1"))
        localPort = serverSocket!!.localPort
        running.set(true)

        Thread({
            try {
                val client = serverSocket!!.accept()
                clientSocket = client
                handleClient(client)
            } catch (e: IOException) {
                if (running.get()) e.printStackTrace()
            }
        }, "WsProxy-Accept").start()

        return localPort
    }

    fun stop() {
        running.set(false)
        upstreamWs?.close(1000, "proxy stopped")
        try { clientSocket?.close() } catch (_: Exception) {}
        try { serverSocket?.close() } catch (_: Exception) {}
    }

    private fun handleClient(client: java.net.Socket) {
        val input = client.getInputStream()
        val output = client.getOutputStream()

        // 1. Read WebSocket upgrade request from WebView
        val requestBytes = ByteArray(4096)
        val len = input.read(requestBytes)
        if (len <= 0) return
        val request = String(requestBytes, 0, len)

        // Extract Sec-WebSocket-Key from the request
        val keyMatch = Regex("Sec-WebSocket-Key: (.+)").find(request)
        val wsKey = keyMatch?.groupValues?.get(1)?.trim() ?: return

        // 2. Send WebSocket upgrade response to WebView
        val acceptKey = computeAcceptKey(wsKey)
        val response = "HTTP/1.1 101 Switching Protocols\r\n" +
            "Upgrade: websocket\r\n" +
            "Connection: Upgrade\r\n" +
            "Sec-WebSocket-Accept: $acceptKey\r\n" +
            "\r\n"
        output.write(response.toByteArray())
        output.flush()

        // 3. Connect upstream to Proxmox via OkHttp (TOFU TLS)
        val trustManager = de.kiefer_networks.proxmoxopen.data.api.tls.TofuTrustManager(fingerprint)
        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, arrayOf<X509TrustManager>(trustManager), SecureRandom())
        }
        val okClient = OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustManager)
            .hostnameVerifier { _, _ -> true }
            .pingInterval(10, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .build()

        val upstreamRequest = Request.Builder()
            .url(targetUrl)
            .addHeader("Cookie", "PVEAuthCookie=$cookie")
            .build()

        upstreamWs = okClient.newWebSocket(upstreamRequest, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                // Forward upstream text frame → local client
                try {
                    sendWsFrame(output, text.toByteArray(Charsets.UTF_8), opcode = 0x1)
                } catch (_: Exception) {}
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                try {
                    sendWsFrame(output, bytes.toByteArray(), opcode = 0x2)
                } catch (_: Exception) {}
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                try { client.close() } catch (_: Exception) {}
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                try { client.close() } catch (_: Exception) {}
            }
        })

        // 4. Read frames from local client → forward upstream
        Thread({
            try {
                while (running.get() && !client.isClosed) {
                    val frame = readWsFrame(input) ?: break
                    when (frame.opcode) {
                        0x1 -> upstreamWs?.send(String(frame.payload, Charsets.UTF_8))
                        0x2 -> upstreamWs?.send(ByteString.of(*frame.payload))
                        0x8 -> { upstreamWs?.close(1000, "client closed"); break }
                        0x9 -> upstreamWs?.send(ByteString.of(*frame.payload)) // ping
                    }
                }
            } catch (_: Exception) {
            } finally {
                upstreamWs?.close(1000, "client disconnected")
            }
        }, "WsProxy-Accept").start()
    }

    // --- WebSocket framing helpers ---

    private data class WsFrame(val opcode: Int, val payload: ByteArray)

    private fun readWsFrame(input: java.io.InputStream): WsFrame? {
        val b0 = input.read(); if (b0 < 0) return null
        val b1 = input.read(); if (b1 < 0) return null

        val opcode = b0 and 0x0F
        val masked = (b1 and 0x80) != 0
        var payloadLen = (b1 and 0x7F).toLong()

        if (payloadLen == 126L) {
            val b2 = input.read(); val b3 = input.read()
            if (b2 < 0 || b3 < 0) return null
            payloadLen = ((b2 shl 8) or b3).toLong()
        } else if (payloadLen == 127L) {
            val buf = ByteArray(8)
            var read = 0; while (read < 8) { val r = input.read(buf, read, 8 - read); if (r < 0) return null; read += r }
            payloadLen = ByteBuffer.wrap(buf).long
        }

        val mask = if (masked) {
            val m = ByteArray(4)
            var read = 0; while (read < 4) { val r = input.read(m, read, 4 - read); if (r < 0) return null; read += r }
            m
        } else null

        val payload = ByteArray(payloadLen.toInt())
        var read = 0
        while (read < payloadLen) {
            val r = input.read(payload, read, (payloadLen - read).toInt())
            if (r < 0) return null
            read += r
        }

        if (mask != null) {
            for (i in payload.indices) payload[i] = (payload[i].toInt() xor mask[i % 4].toInt()).toByte()
        }

        return WsFrame(opcode, payload)
    }

    private fun sendWsFrame(output: java.io.OutputStream, data: ByteArray, opcode: Int) {
        synchronized(output) {
            output.write(0x80 or opcode) // FIN + opcode
            if (data.size < 126) {
                output.write(data.size)
            } else if (data.size < 65536) {
                output.write(126)
                output.write(data.size shr 8)
                output.write(data.size and 0xFF)
            } else {
                output.write(127)
                val buf = ByteBuffer.allocate(8).putLong(data.size.toLong()).array()
                output.write(buf)
            }
            output.write(data)
            output.flush()
        }
    }

    private fun computeAcceptKey(key: String): String {
        val magic = key + "258EAFA5-E914-47DA-95CA-5AB5F986B130"
        val sha1 = MessageDigest.getInstance("SHA-1").digest(magic.toByteArray())
        return Base64.getEncoder().encodeToString(sha1)
    }
}
