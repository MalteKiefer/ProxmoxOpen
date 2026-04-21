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
                // For token-based auth there is no ticket; we must probe the server
                // with an endpoint that genuinely requires valid authentication and
                // fails (401/403) on bad credentials. A 2xx with empty payload is
                // treated as an auth anomaly: a working Proxmox server always
                // reports at least the local node.
                val client = sessions.apiClient(server, credentials)
                val nodes = client.getNodes()
                if (nodes.isEmpty()) {
                    ApiResult.Failure(
                        ApiError.Auth("Token accepted but server returned no nodes; likely insufficient privileges"),
                    )
                } else {
                    // Second, resource-scoped call — Proxmox enforces auth per-path,
                    // so this catches revoked tokens that still pass /nodes listing.
                    client.getNodeStatus(nodes.first().node)
                    val dummy = AuthSession(
                        ticket = "",
                        csrfToken = "",
                        expiresAt = System.currentTimeMillis() + TOKEN_TTL_MILLIS,
                    )
                    sessions.putSession(server.id, dummy)
                    ApiResult.Success(dummy)
                }
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

    /**
     * Re-authenticates a PAM/PVE (user+password) session. For API-token servers
     * this is a no-op because tokens do not expire on a TTL and [login] already
     * stores a sentinel session with [TOKEN_TTL_MILLIS]. Callers typically invoke
     * this from a session watchdog when the ticket is <10min from expiry.
     *
     * Credentials are not persisted by this layer; the caller (e.g. a session
     * manager holding in-memory credentials for the lifetime of the unlocked
     * app) must supply them.
     */
    suspend fun refreshTicket(
        server: Server,
        credentials: Credentials?,
    ): ApiResult<AuthSession> {
        // Token auth: no ticket to refresh. Return the existing sentinel if present,
        // otherwise fail so the caller re-logs in with the token.
        if (credentials is Credentials.ApiToken || credentials == null) {
            val existing = sessions.getSession(server.id)
            return if (existing != null) {
                ApiResult.Success(existing)
            } else {
                ApiResult.Failure(ApiError.Auth("No session to refresh; login required"))
            }
        }
        // Password login re-issues a fresh ticket+CSRF pair.
        return login(server, credentials)
    }

    /**
     * Returns the current session when it is still safely valid (>[REFRESH_SKEW_MILLIS]
     * remaining) or transparently refreshes it otherwise. If no session exists or
     * credentials are needed but unavailable, returns [ApiError.Auth] so the UI can
     * route the user to the login screen.
     *
     * [credentials] may be null; when null the method cannot perform an interactive
     * re-auth and will only return a still-valid cached session.
     */
    suspend fun ensureValidSession(
        server: Server,
        credentials: Credentials? = null,
    ): ApiResult<AuthSession> {
        val current = sessions.getSession(server.id)
        val now = System.currentTimeMillis()
        if (current != null && current.expiresAt - now > REFRESH_SKEW_MILLIS) {
            return ApiResult.Success(current)
        }
        return refreshTicket(server, credentials)
    }

    /** Convenience overload keyed by [Server.id] when the caller already has the Server. */
    suspend fun ensureValidSession(serverId: Long): ApiResult<AuthSession> {
        val current = sessions.getSession(serverId)
            ?: return ApiResult.Failure(ApiError.Auth("No session for server $serverId"))
        val now = System.currentTimeMillis()
        return if (current.expiresAt - now > REFRESH_SKEW_MILLIS) {
            ApiResult.Success(current)
        } else {
            ApiResult.Failure(
                ApiError.Auth("Session expiring; credentials required to refresh"),
            )
        }
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

        /**
         * Refresh the PVE ticket once less than 10 minutes of its 2h lifetime
         * remain. Chosen to be well above typical request durations yet tight
         * enough that the server still accepts the renewal.
         */
        private const val REFRESH_SKEW_MILLIS = 10L * 60 * 1000
    }
}

private fun ByteArray.sha256Hex(): String =
    MessageDigest.getInstance("SHA-256").digest(this).toHex()

private fun ByteArray.toHex(): String = BigInteger(1, this).toString(16).padStart(size * 2, '0')

// Tiny helper to keep AuthRepositoryImpl from importing HttpClient directly when not needed.
@Suppress("unused") private typealias _Client = HttpClient
