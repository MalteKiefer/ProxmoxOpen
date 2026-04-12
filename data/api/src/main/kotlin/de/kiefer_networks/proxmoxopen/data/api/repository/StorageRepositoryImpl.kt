package de.kiefer_networks.proxmoxopen.data.api.repository

import de.kiefer_networks.proxmoxopen.data.api.mapper.toStorageInfo
import de.kiefer_networks.proxmoxopen.data.api.session.ProxmoxSessionManager
import de.kiefer_networks.proxmoxopen.domain.model.Credentials
import de.kiefer_networks.proxmoxopen.domain.model.Realm
import de.kiefer_networks.proxmoxopen.domain.model.StorageInfo
import de.kiefer_networks.proxmoxopen.domain.repository.ServerRepository
import de.kiefer_networks.proxmoxopen.domain.repository.StorageRepository
import de.kiefer_networks.proxmoxopen.domain.result.ApiError
import de.kiefer_networks.proxmoxopen.domain.result.ApiResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageRepositoryImpl @Inject constructor(
    private val sessions: ProxmoxSessionManager,
    private val servers: ServerRepository,
) : StorageRepository {

    override suspend fun listStorages(serverId: Long, node: String): ApiResult<List<StorageInfo>> =
        runCatchingApi {
            val server = servers.getById(serverId)
                ?: throw IllegalStateException("server $serverId missing")
            val credentials = when (server.realm) {
                Realm.PVE_TOKEN -> {
                    val secret = servers.getTokenSecret(serverId)
                        ?: return ApiResult.Failure(ApiError.Auth("token missing"))
                    Credentials.ApiToken(
                        username = server.username ?: "root",
                        realm = server.realm,
                        tokenId = server.tokenId ?: "",
                        tokenSecret = secret,
                    )
                }
                else -> null
            }
            sessions.apiClient(server, credentials).listStorages(node).map { it.toStorageInfo() }
        }
}
