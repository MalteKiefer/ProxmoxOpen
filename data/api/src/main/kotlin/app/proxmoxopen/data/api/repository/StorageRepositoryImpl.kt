package app.proxmoxopen.data.api.repository

import app.proxmoxopen.data.api.mapper.toStorageInfo
import app.proxmoxopen.data.api.session.ProxmoxSessionManager
import app.proxmoxopen.domain.model.Credentials
import app.proxmoxopen.domain.model.Realm
import app.proxmoxopen.domain.model.StorageInfo
import app.proxmoxopen.domain.repository.ServerRepository
import app.proxmoxopen.domain.repository.StorageRepository
import app.proxmoxopen.domain.result.ApiError
import app.proxmoxopen.domain.result.ApiResult
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
