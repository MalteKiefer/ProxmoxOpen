package app.proxmoxopen.data.api

import app.proxmoxopen.data.api.tls.TofuTrustManager
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import java.security.SecureRandom
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager
import kotlinx.serialization.json.Json
import timber.log.Timber

data class ServerConnection(
    val baseUrl: String,
    val fingerprintSha256: String?,
)

/**
 * Builds a Ktor [HttpClient] pinned to the server's certificate via [TofuTrustManager].
 * If [ServerConnection.fingerprintSha256] is null, the client operates in probe mode
 * and any cert is accepted; the observed fingerprint can then be read from the trust manager.
 */
class ProxmoxClientFactory(
    private val json: Json = DefaultJson,
) {
    fun create(
        connection: ServerConnection,
        onTrustManager: ((TofuTrustManager) -> Unit)? = null,
    ): HttpClient {
        val trustManager = TofuTrustManager(connection.fingerprintSha256)
        onTrustManager?.invoke(trustManager)
        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, arrayOf<X509TrustManager>(trustManager), SecureRandom())
        }
        return HttpClient(OkHttp) {
            expectSuccess = false
            engine {
                config {
                    sslSocketFactory(sslContext.socketFactory, trustManager)
                    // We pin via fingerprint; hostname mismatch is tolerable for
                    // self-signed Proxmox setups with IP-only access.
                    hostnameVerifier { _, _ -> true }
                    retryOnConnectionFailure(false)
                }
            }
            install(ContentNegotiation) { json(json) }
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 15_000
                socketTimeoutMillis = 30_000
            }
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        val sanitized = message
                            .replace(Regex("Authorization: [^\\r\\n]+"), "Authorization: ***")
                            .replace(Regex("PVEAPIToken=[^\\r\\n]+"), "PVEAPIToken=***")
                        Timber.tag("PxoHttp").v(sanitized)
                    }
                }
                level = LogLevel.HEADERS
            }
        }
    }

    companion object {
        val DefaultJson: Json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = false
        }
    }
}
