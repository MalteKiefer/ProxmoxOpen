package de.kiefer_networks.proxmoxopen.data.api.repository

import de.kiefer_networks.proxmoxopen.data.api.ProxmoxClientFactory
import de.kiefer_networks.proxmoxopen.data.api.ProxmoxHttpException
import de.kiefer_networks.proxmoxopen.data.api.ServerConnection
import de.kiefer_networks.proxmoxopen.data.api.session.ProxmoxSessionManager
import de.kiefer_networks.proxmoxopen.data.api.tls.TofuTrustManager
import de.kiefer_networks.proxmoxopen.domain.model.AuthSession
import de.kiefer_networks.proxmoxopen.domain.model.Credentials
import de.kiefer_networks.proxmoxopen.domain.model.Realm
import de.kiefer_networks.proxmoxopen.domain.model.Server
import de.kiefer_networks.proxmoxopen.domain.repository.AuthRepository
import de.kiefer_networks.proxmoxopen.domain.repository.ServerProbe
import de.kiefer_networks.proxmoxopen.domain.result.ApiError
import de.kiefer_networks.proxmoxopen.domain.result.ApiResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.utils.io.errors.IOException
import java.math.BigInteger
import java.security.MessageDigest
import java.security.cert.X509Certificate
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val sessions: ProxmoxSessionManager,
    private val clientFactory: ProxmoxClientFactory,
) : AuthRepository {

    override suspend fun login(
        server: Server,
        credentials: Credentials,
    ): ApiResult<AuthSession> = try {
        when (credentials) {
            is Credentials.ApiToken -> {
                // For token-based auth there is no ticket; we probe the server
                // and return a dummy session just to signal success.
                val client = sessions.apiClient(server, credentials)
                client.getNodes() // smoke test
                val dummy = AuthSession(
                    ticket = "",
                    csrfToken = "",
                    expiresAt = System.currentTimeMillis() + TOKEN_TTL_MILLIS,
                )
                sessions.putSession(server.id, dummy)
                ApiResult.Success(dummy)
            }
            is Credentials.UserPassword -> {
                val client = clientFactory.create(
                    ServerConnection(server.baseUrl, server.fingerprintSha256),
                )
                try {
                    val proxmoxClient = de.kiefer_networks.proxmoxopen.data.api.ProxmoxApiClient(
                        http = client,
                        baseUrl = server.baseUrl,
                        authHeader = de.kiefer_networks.proxmoxopen.data.api.ProxmoxApiClient.Authentication
                            .ApiToken(""),
                    )
                    val ticket = proxmoxClient.createTicket(
                        username = credentials.username,
                        password = credentials.password,
                        realm = credentials.realm.apiKey,
                        totp = credentials.totp,
                    )
                    val session = AuthSession(
                        ticket = ticket.ticket,
                        csrfToken = ticket.csrfToken,
                        expiresAt = System.currentTimeMillis() + TICKET_TTL_MILLIS,
                    )
                    sessions.putSession(server.id, session)
                    ApiResult.Success(session)
                } finally {
                    client.close()
                }
            }
        }
    } catch (e: ProxmoxHttpException) {
        when (e.code) {
            401, 403 -> ApiResult.Failure(ApiError.Auth(e.message ?: "auth failed"))
            else -> ApiResult.Failure(ApiError.Http(e.code, e.message ?: "http error"))
        }
    } catch (e: IOException) {
        ApiResult.Failure(ApiError.Network(e.message ?: "network error"))
    }

    override suspend fun currentSession(serverId: Long): AuthSession? =
        sessions.getSession(serverId)

    override suspend fun logout(serverId: Long) {
        sessions.clearSession(serverId)
    }

    override suspend fun probeFingerprint(host: String, port: Int): ApiResult<ServerProbe> =
        withContext(Dispatchers.IO) {
            try {
                val probe = TofuTrustManager(null)
                val ctx = SSLContext.getInstance("TLS").apply {
                    init(null, arrayOf<X509TrustManager>(probe), java.security.SecureRandom())
                }
                val socket = ctx.socketFactory.createSocket(host, port) as javax.net.ssl.SSLSocket
                socket.use {
                    it.startHandshake()
                    val chain = it.session.peerCertificates
                    val leaf = chain[0] as X509Certificate
                    val sha256 = leaf.encoded.sha256Hex()
                    ApiResult.Success(
                        ServerProbe(
                            host = host,
                            port = port,
                            subject = leaf.subjectX500Principal.name,
                            issuer = leaf.issuerX500Principal.name,
                            validFrom = leaf.notBefore.time,
                            validTo = leaf.notAfter.time,
                            sha256Fingerprint = sha256,
                        ),
                    )
                }
            } catch (e: IOException) {
                ApiResult.Failure(ApiError.Network(e.message ?: "cannot reach $host:$port"))
            } catch (e: java.security.GeneralSecurityException) {
                ApiResult.Failure(ApiError.Tls(e.message ?: "TLS error"))
            }
        }

    companion object {
        private const val TICKET_TTL_MILLIS = 2L * 60 * 60 * 1000
        private const val TOKEN_TTL_MILLIS = Long.MAX_VALUE
    }
}

private fun ByteArray.sha256Hex(): String =
    MessageDigest.getInstance("SHA-256").digest(this).toHex()

private fun ByteArray.toHex(): String = BigInteger(1, this).toString(16).padStart(size * 2, '0')

// Tiny helper to keep AuthRepositoryImpl from importing HttpClient directly when not needed.
@Suppress("unused") private typealias _Client = HttpClient
