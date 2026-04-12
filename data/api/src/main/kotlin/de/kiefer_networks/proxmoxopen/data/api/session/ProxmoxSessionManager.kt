package de.kiefer_networks.proxmoxopen.data.api.session

import de.kiefer_networks.proxmoxopen.data.api.ProxmoxApiClient
import de.kiefer_networks.proxmoxopen.data.api.ProxmoxClientFactory
import de.kiefer_networks.proxmoxopen.data.api.ServerConnection
import de.kiefer_networks.proxmoxopen.domain.model.AuthSession
import de.kiefer_networks.proxmoxopen.domain.model.Credentials
import de.kiefer_networks.proxmoxopen.domain.model.Realm
import de.kiefer_networks.proxmoxopen.domain.model.Server
import io.ktor.client.HttpClient
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Per-server HTTP client and session cache. Clients are built lazily on first request
 * and kept for the process lifetime. Sessions (ticket + CSRF) live in memory only.
 */
@Singleton
class ProxmoxSessionManager @Inject constructor(
    private val clientFactory: ProxmoxClientFactory,
) {
    private data class Entry(
        val client: HttpClient,
        val baseUrl: String,
        val fingerprint: String,
    )

    private val clients = ConcurrentHashMap<Long, Entry>()
    private val sessions = ConcurrentHashMap<Long, AuthSession>()

    fun clientFor(server: Server): HttpClient = entryFor(server).client

    fun apiClient(server: Server, credentials: Credentials?): ProxmoxApiClient {
        val entry = entryFor(server)
        val auth = when {
            server.realm == Realm.PVE_TOKEN && credentials is Credentials.ApiToken ->
                ProxmoxApiClient.Authentication.ApiToken(credentials.headerValue)
            server.realm == Realm.PVE_TOKEN -> {
                // token-based server — caller must provide credentials each call
                error("PVE_TOKEN server requires ApiToken credentials")
            }
            else -> {
                val session = sessions[server.id]
                    ?: error("No session for server ${server.id}; login first")
                ProxmoxApiClient.Authentication.Ticket(session.ticket, session.csrfToken)
            }
        }
        return ProxmoxApiClient(entry.client, entry.baseUrl, auth)
    }

    /** For unauthenticated probing (no auth header). */
    fun probeApiClient(server: Server): ProxmoxApiClient {
        val entry = entryFor(server)
        return ProxmoxApiClient(
            http = entry.client,
            baseUrl = entry.baseUrl,
            authHeader = ProxmoxApiClient.Authentication.ApiToken(""),
        )
    }

    fun putSession(serverId: Long, session: AuthSession) {
        sessions[serverId] = session
    }

    fun getSession(serverId: Long): AuthSession? = sessions[serverId]

    fun clearSession(serverId: Long) {
        sessions.remove(serverId)
    }

    fun invalidateClient(serverId: Long) {
        clients.remove(serverId)?.client?.close()
        sessions.remove(serverId)
    }

    private fun entryFor(server: Server): Entry {
        val existing = clients[server.id]
        if (existing != null && existing.fingerprint == server.fingerprintSha256) return existing
        existing?.client?.close()
        val client = clientFactory.create(
            ServerConnection(
                baseUrl = server.baseUrl,
                fingerprintSha256 = server.fingerprintSha256,
            ),
        )
        val newEntry = Entry(client, server.baseUrl, server.fingerprintSha256)
        clients[server.id] = newEntry
        return newEntry
    }
}
