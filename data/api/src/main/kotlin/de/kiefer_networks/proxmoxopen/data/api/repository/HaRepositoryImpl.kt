package de.kiefer_networks.proxmoxopen.data.api.repository

import de.kiefer_networks.proxmoxopen.data.api.ProxmoxApiClient
import de.kiefer_networks.proxmoxopen.data.api.dto.HaGroupDto
import de.kiefer_networks.proxmoxopen.data.api.dto.HaResourceDto
import de.kiefer_networks.proxmoxopen.data.api.dto.HaStatusDto
import de.kiefer_networks.proxmoxopen.data.api.session.ProxmoxSessionManager
import de.kiefer_networks.proxmoxopen.domain.model.Credentials
import de.kiefer_networks.proxmoxopen.domain.model.HaClusterStatus
import de.kiefer_networks.proxmoxopen.domain.model.HaGroup
import de.kiefer_networks.proxmoxopen.domain.model.HaMember
import de.kiefer_networks.proxmoxopen.domain.model.HaResource
import de.kiefer_networks.proxmoxopen.domain.model.Realm
import de.kiefer_networks.proxmoxopen.domain.repository.HaRepository
import de.kiefer_networks.proxmoxopen.domain.repository.ServerRepository
import de.kiefer_networks.proxmoxopen.domain.result.ApiError
import de.kiefer_networks.proxmoxopen.domain.result.ApiResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HaRepositoryImpl @Inject constructor(
    private val sessions: ProxmoxSessionManager,
    private val servers: ServerRepository,
) : HaRepository {

    override suspend fun getStatus(serverId: Long): ApiResult<HaClusterStatus> =
        call(serverId) { api -> api.getHaStatus().toClusterStatus() }

    override suspend fun getResources(serverId: Long): ApiResult<List<HaResource>> =
        call(serverId) { api -> api.getHaResources().map { it.toDomain() } }

    override suspend fun getGroups(serverId: Long): ApiResult<List<HaGroup>> =
        call(serverId) { api -> api.getHaGroups().map { it.toDomain() } }

    private suspend fun <T> call(
        serverId: Long,
        block: suspend (ProxmoxApiClient) -> T,
    ): ApiResult<T> = runCatchingApi {
        val server = servers.getById(serverId)
            ?: error("server $serverId missing")
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

// --- mappers (file-local) ----------------------------------------------------

private fun List<HaStatusDto>.toClusterStatus(): HaClusterStatus {
    val quorumEntry = firstOrNull { it.type.equals("quorum", ignoreCase = true) }
    val masterEntry = firstOrNull { it.type.equals("master", ignoreCase = true) }
    val members = map { dto ->
        HaMember(
            id = dto.id ?: dto.sid ?: dto.service ?: "",
            type = dto.type ?: "",
            node = dto.node,
            status = dto.status,
            state = dto.state,
            timestamp = dto.timestamp,
        )
    }
    return HaClusterStatus(
        quorate = quorumEntry?.quorate?.let { it == 1 },
        master = masterEntry?.node ?: masterEntry?.id,
        members = members,
    )
}

private fun HaResourceDto.toDomain(): HaResource = HaResource(
    sid = sid,
    type = type ?: sid.substringBefore(':', missingDelimiterValue = "unknown"),
    state = state ?: "unknown",
    group = group?.takeIf { it.isNotBlank() },
    comment = comment?.takeIf { it.isNotBlank() },
    maxRelocate = max_relocate,
    maxRestart = max_restart,
)

private fun HaGroupDto.toDomain(): HaGroup = HaGroup(
    group = group,
    nodes = nodes
        ?.split(',')
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        ?: emptyList(),
    comment = comment?.takeIf { it.isNotBlank() },
    restricted = restricted == 1,
    nofailback = nofailback == 1,
    type = type,
)
