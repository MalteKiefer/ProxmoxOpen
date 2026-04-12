package de.kiefer_networks.proxmoxopen.data.api.repository

import de.kiefer_networks.proxmoxopen.data.api.session.ProxmoxSessionManager
import de.kiefer_networks.proxmoxopen.domain.model.Credentials
import de.kiefer_networks.proxmoxopen.domain.model.GuestType
import de.kiefer_networks.proxmoxopen.domain.model.PowerAction
import de.kiefer_networks.proxmoxopen.domain.model.Realm
import de.kiefer_networks.proxmoxopen.domain.repository.PowerRepository
import de.kiefer_networks.proxmoxopen.domain.repository.ServerRepository
import de.kiefer_networks.proxmoxopen.domain.result.ApiResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PowerRepositoryImpl @Inject constructor(
    private val sessions: ProxmoxSessionManager,
    private val servers: ServerRepository,
) : PowerRepository {

    override suspend fun execute(
        serverId: Long,
        node: String,
        vmid: Int,
        type: GuestType,
        action: PowerAction,
    ): ApiResult<String> = runCatchingApi {
        val server = servers.getById(serverId) ?: error("server $serverId missing")
        val credentials: Credentials? = if (server.realm == Realm.PVE_TOKEN) {
            val secret = servers.getTokenSecret(serverId)
                ?: error("token secret missing")
            Credentials.ApiToken(
                username = server.username ?: "root",
                realm = server.realm,
                tokenId = server.tokenId ?: "",
                tokenSecret = secret,
            )
        } else {
            null
        }
        sessions.apiClient(server, credentials)
            .powerAction(node, type.apiPath, vmid, action.apiPath)
    }
}
