package de.kiefer_networks.proxmoxopen.domain.model

data class Server(
    val id: Long,
    val name: String,
    val host: String,
    val port: Int,
    val realm: Realm,
    val username: String?,
    val tokenId: String?,
    val fingerprintSha256: String,
    val createdAt: Long,
    val lastConnectedAt: Long?,
) {
    val baseUrl: String
        get() = "https://$host:$port"
}
