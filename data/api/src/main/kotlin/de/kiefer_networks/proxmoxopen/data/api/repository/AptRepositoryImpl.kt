package de.kiefer_networks.proxmoxopen.data.api.repository

import de.kiefer_networks.proxmoxopen.data.api.ProxmoxApiClient
import de.kiefer_networks.proxmoxopen.data.api.session.ProxmoxSessionManager
import de.kiefer_networks.proxmoxopen.domain.model.AptUpdate
import de.kiefer_networks.proxmoxopen.domain.model.Credentials
import de.kiefer_networks.proxmoxopen.domain.model.Realm
import de.kiefer_networks.proxmoxopen.domain.repository.AptRepository
import de.kiefer_networks.proxmoxopen.domain.repository.ServerRepository
import de.kiefer_networks.proxmoxopen.domain.result.ApiError
import de.kiefer_networks.proxmoxopen.domain.result.ApiResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AptRepositoryImpl @Inject constructor(
    private val sessions: ProxmoxSessionManager,
    private val servers: ServerRepository,
) : AptRepository {

    override suspend fun listUpdates(
        serverId: Long,
        node: String,
    ): ApiResult<List<AptUpdate>> = execute(serverId) { api ->
        api.listAptUpdates(node)
            .filter { !it.packageName.isNullOrBlank() }
            .map { dto ->
                AptUpdate(
                    packageName = dto.packageName.orEmpty(),
                    currentVersion = dto.oldVersion,
                    candidateVersion = dto.version,
                    origin = dto.origin,
                    priority = dto.priority,
                    title = dto.title,
                    description = dto.description,
                    section = dto.section,
                )
            }
    }

    override suspend fun refresh(serverId: Long, node: String): ApiResult<String> =
        execute(serverId) { api -> api.refreshApt(node) }

    override suspend fun upgradeAll(serverId: Long, node: String): ApiResult<String> =
        execute(serverId) { api -> api.upgradeApt(node) }

    private suspend fun <T> execute(
        serverId: Long,
        block: suspend (ProxmoxApiClient) -> T,
    ): ApiResult<T> = runCatchingApi {
        val server = servers.getById(serverId)
            ?: return@runCatchingApi throw IllegalStateException("server $serverId missing")
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
        block(sessions.apiClient(server, credentials))
    }
}
