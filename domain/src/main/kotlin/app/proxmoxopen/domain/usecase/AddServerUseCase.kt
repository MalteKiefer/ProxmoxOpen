package app.proxmoxopen.domain.usecase

import app.proxmoxopen.domain.model.Realm
import app.proxmoxopen.domain.model.Server
import app.proxmoxopen.domain.repository.ServerRepository
import javax.inject.Inject

class AddServerUseCase @Inject constructor(
    private val servers: ServerRepository,
) {
    suspend operator fun invoke(
        name: String,
        host: String,
        port: Int,
        realm: Realm,
        username: String?,
        tokenId: String?,
        fingerprintSha256: String,
        tokenSecret: String?,
        password: String?,
    ): Long {
        val server = Server(
            id = 0,
            name = name,
            host = host,
            port = port,
            realm = realm,
            username = username,
            tokenId = tokenId,
            fingerprintSha256 = fingerprintSha256,
            createdAt = System.currentTimeMillis(),
            lastConnectedAt = null,
        )
        return servers.add(server, tokenSecret, password)
    }
}
