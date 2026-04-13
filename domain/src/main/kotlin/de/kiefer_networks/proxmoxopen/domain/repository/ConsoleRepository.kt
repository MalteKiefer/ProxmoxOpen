package de.kiefer_networks.proxmoxopen.domain.repository

data class ConsoleProxyInfo(
    val upstreamUrl: String,
    val cookie: String,
    val fingerprint: String?,
    val username: String,
    val vncTicket: String,
)

interface ConsoleRepository {
    suspend fun createConsoleProxy(serverId: Long, node: String, vmid: Int, type: String): ConsoleProxyInfo
}
