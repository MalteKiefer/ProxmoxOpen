package de.kiefer_networks.proxmoxopen.data.api.repository

import de.kiefer_networks.proxmoxopen.data.api.ProxmoxApiClient
import de.kiefer_networks.proxmoxopen.data.api.dto.NodeDiskDto
import de.kiefer_networks.proxmoxopen.data.api.dto.SmartDto
import de.kiefer_networks.proxmoxopen.data.api.session.ProxmoxSessionManager
import de.kiefer_networks.proxmoxopen.domain.model.Credentials
import de.kiefer_networks.proxmoxopen.domain.model.DiskHealth
import de.kiefer_networks.proxmoxopen.domain.model.DiskInfo
import de.kiefer_networks.proxmoxopen.domain.model.DiskType
import de.kiefer_networks.proxmoxopen.domain.model.Realm
import de.kiefer_networks.proxmoxopen.domain.model.SmartAttributeEntry
import de.kiefer_networks.proxmoxopen.domain.model.SmartReport
import de.kiefer_networks.proxmoxopen.domain.repository.NodeDiskRepository
import de.kiefer_networks.proxmoxopen.domain.repository.ServerRepository
import de.kiefer_networks.proxmoxopen.domain.result.ApiError
import de.kiefer_networks.proxmoxopen.domain.result.ApiResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NodeDiskRepositoryImpl @Inject constructor(
    private val sessions: ProxmoxSessionManager,
    private val servers: ServerRepository,
) : NodeDiskRepository {

    override suspend fun listDisks(serverId: Long, node: String): ApiResult<List<DiskInfo>> =
        execute(serverId) { api -> api.listNodeDisks(node).map { it.toDomain() } }

    override suspend fun getSmart(
        serverId: Long,
        node: String,
        devpath: String,
    ): ApiResult<SmartReport> = execute(serverId) { api ->
        api.getDiskSmart(node, devpath).toDomain()
    }

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

private fun NodeDiskDto.toDomain(): DiskInfo = DiskInfo(
    devpath = devpath,
    model = model?.trim()?.ifBlank { null },
    serial = serial?.trim()?.ifBlank { null },
    vendor = vendor?.trim()?.ifBlank { null },
    size = size ?: 0L,
    type = DiskType.fromApi(type),
    health = DiskHealth.fromApi(health),
    wearoutPercent = wearoutPercent(),
    rpm = rpm,
    wwn = wwn,
    used = used,
    mounted = (mounted ?: 0) == 1,
    gpt = (gpt ?: 0) == 1,
    osdId = osdId,
)

private fun SmartDto.toDomain(): SmartReport = SmartReport(
    health = DiskHealth.fromApi(health),
    type = type,
    text = text,
    attributes = attributePairs().map {
        SmartAttributeEntry(name = it.name, value = it.value, rawValue = it.rawValue)
    },
)
