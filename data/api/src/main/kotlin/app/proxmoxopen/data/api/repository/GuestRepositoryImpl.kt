package app.proxmoxopen.data.api.repository

import app.proxmoxopen.data.api.ProxmoxApiClient
import app.proxmoxopen.data.api.mapper.toGuestConfig
import app.proxmoxopen.data.api.mapper.toDomain
import app.proxmoxopen.data.api.mapper.toGuestOrNull
import app.proxmoxopen.data.api.session.ProxmoxSessionManager
import app.proxmoxopen.domain.model.Credentials
import app.proxmoxopen.domain.model.Guest
import app.proxmoxopen.domain.model.GuestConfig
import app.proxmoxopen.domain.model.GuestType
import app.proxmoxopen.domain.model.Realm
import app.proxmoxopen.domain.model.RrdPoint
import app.proxmoxopen.domain.model.RrdTimeframe
import app.proxmoxopen.domain.repository.GuestRepository
import app.proxmoxopen.domain.repository.ServerRepository
import app.proxmoxopen.domain.result.ApiError
import app.proxmoxopen.domain.result.ApiResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GuestRepositoryImpl @Inject constructor(
    private val sessions: ProxmoxSessionManager,
    private val servers: ServerRepository,
) : GuestRepository {

    override suspend fun listGuests(serverId: Long): ApiResult<List<Guest>> = call(serverId) { api ->
        api.listClusterResources().mapNotNull { it.toGuestOrNull() }
    }

    override suspend fun getGuest(
        serverId: Long,
        node: String,
        vmid: Int,
        type: GuestType,
    ): ApiResult<Guest> = call(serverId) { api ->
        api.getGuestStatus(node, type.apiPath, vmid).toDomain(node, type)
    }

    override suspend fun getGuestConfig(
        serverId: Long,
        node: String,
        vmid: Int,
        type: GuestType,
    ): ApiResult<GuestConfig> = call(serverId) { api ->
        api.getGuestConfig(node, type.apiPath, vmid).toGuestConfig()
    }

    override suspend fun setGuestConfig(
        serverId: Long,
        node: String,
        vmid: Int,
        type: GuestType,
        params: Map<String, String>,
    ): ApiResult<Unit> = call(serverId) { api ->
        api.setGuestConfig(node, type.apiPath, vmid, params)
    }

    override suspend fun getGuestRrd(
        serverId: Long,
        node: String,
        vmid: Int,
        type: GuestType,
        timeframe: RrdTimeframe,
    ): ApiResult<List<RrdPoint>> = call(serverId) { api ->
        api.getGuestRrd(node, type.apiPath, vmid, timeframe.apiKey).map { it.toDomain() }
    }

    private suspend fun <T> call(
        serverId: Long,
        block: suspend (ProxmoxApiClient) -> T,
    ): ApiResult<T> = runCatchingApi {
        val server = servers.getById(serverId)
            ?: error("server $serverId missing")
        val credentials = tokenCredentialsIfNeeded(serverId, server.realm, server.username, server.tokenId)
        block(sessions.apiClient(server, credentials))
    }

    private suspend fun tokenCredentialsIfNeeded(
        serverId: Long,
        realm: Realm,
        username: String?,
        tokenId: String?,
    ): Credentials? {
        if (realm != Realm.PVE_TOKEN) return null
        val secret = servers.getTokenSecret(serverId) ?: return null
        return Credentials.ApiToken(
            username = username ?: "root",
            realm = realm,
            tokenId = tokenId ?: "",
            tokenSecret = secret,
        )
    }
}

@Suppress("unused") private val _unusedPing = ApiError.Unknown("ping")
