package de.kiefer_networks.proxmoxopen.data.api.repository

import de.kiefer_networks.proxmoxopen.data.api.session.ProxmoxSessionManager
import de.kiefer_networks.proxmoxopen.domain.model.Credentials
import de.kiefer_networks.proxmoxopen.domain.model.Realm
import de.kiefer_networks.proxmoxopen.domain.repository.ConsoleProxyInfo
import de.kiefer_networks.proxmoxopen.domain.repository.ConsoleRepository
import de.kiefer_networks.proxmoxopen.domain.repository.ServerRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConsoleRepositoryImpl @Inject constructor(
    private val sessions: ProxmoxSessionManager,
    private val servers: ServerRepository,
) : ConsoleRepository {

    override suspend fun createConsoleProxy(
        serverId: Long,
        node: String,
        vmid: Int,
        type: String,
    ): ConsoleProxyInfo {
        val server = servers.getById(serverId)
            ?: error("server $serverId missing")
        val session = sessions.getSession(serverId)
            ?: error("Not authenticated")

        val credentials: Credentials? = if (server.realm == Realm.PVE_TOKEN) {
            val secret = servers.getTokenSecret(serverId)
            if (secret != null) Credentials.ApiToken(
                username = server.username ?: "root",
                realm = server.realm,
                tokenId = server.tokenId ?: "",
                tokenSecret = secret,
            ) else null
        } else null

        val apiClient = sessions.apiClient(server, credentials)
        val proxyTicket = when (type) {
            "node" -> apiClient.createNodeTermProxy(node)
            "lxc" -> apiClient.createLxcTermProxy(node, vmid)
            "qemu" -> apiClient.createVncProxy(node, "qemu", vmid)
            else -> apiClient.createLxcTermProxy(node, vmid)
        }

        val wsPath = when (type) {
            "node" -> "/api2/json/nodes/$node/vncwebsocket"
            "lxc" -> "/api2/json/nodes/$node/lxc/$vmid/vncwebsocket"
            "qemu" -> "/api2/json/nodes/$node/qemu/$vmid/vncwebsocket"
            else -> "/api2/json/nodes/$node/lxc/$vmid/vncwebsocket"
        }

        val encodedTicket = java.net.URLEncoder.encode(proxyTicket.ticket, "UTF-8")
            .replace("+", "%20")
        val upstreamUrl = "wss://${server.host}:${server.port}$wsPath?port=${proxyTicket.port}&vncticket=$encodedTicket"

        val username = (server.username ?: "root") + "@" + server.realm.apiKey

        return ConsoleProxyInfo(
            upstreamUrl = upstreamUrl,
            cookie = session.ticket,
            fingerprint = server.fingerprintSha256,
            username = username,
            vncTicket = proxyTicket.ticket,
        )
    }
}
