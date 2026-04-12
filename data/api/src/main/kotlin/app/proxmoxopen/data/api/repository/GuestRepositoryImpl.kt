package app.proxmoxopen.data.api.repository

import app.proxmoxopen.data.api.ProxmoxApiClient
import app.proxmoxopen.data.api.mapper.toBackup
import app.proxmoxopen.data.api.mapper.toContainerStatus
import app.proxmoxopen.data.api.mapper.toVmStatus
import app.proxmoxopen.data.api.mapper.toVmConfig
import app.proxmoxopen.data.api.mapper.toGuestConfig
import app.proxmoxopen.data.api.mapper.toDomain
import app.proxmoxopen.data.api.mapper.toSnapshot
import app.proxmoxopen.data.api.mapper.toGuestOrNull
import app.proxmoxopen.data.api.session.ProxmoxSessionManager
import app.proxmoxopen.domain.model.Credentials
import app.proxmoxopen.domain.model.Backup
import app.proxmoxopen.domain.model.ContainerStatus
import app.proxmoxopen.domain.model.Guest
import app.proxmoxopen.domain.model.GuestConfig
import app.proxmoxopen.domain.model.GuestType
import app.proxmoxopen.domain.model.Snapshot
import app.proxmoxopen.domain.model.VmConfig
import app.proxmoxopen.domain.model.VmStatus
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

    override suspend fun getContainerStatus(
        serverId: Long, node: String, vmid: Int,
    ): ApiResult<ContainerStatus> = call(serverId) { api ->
        val status = api.getContainerStatus(node, vmid)
        val ifaces = api.getContainerInterfaces(node, vmid)
        status.toContainerStatus(node, ifaces)
    }

    override suspend fun listSnapshots(
        serverId: Long, node: String, vmid: Int, type: GuestType,
    ): ApiResult<List<Snapshot>> = call(serverId) { api ->
        api.listSnapshots(node, type.apiPath, vmid)
            .filter { it.name != "current" }
            .map { it.toSnapshot() }
    }

    override suspend fun createSnapshot(
        serverId: Long, node: String, vmid: Int, type: GuestType, snapname: String, description: String?,
    ): ApiResult<String> = call(serverId) { api ->
        api.createSnapshot(node, type.apiPath, vmid, snapname, description)
    }

    override suspend fun rollbackSnapshot(
        serverId: Long, node: String, vmid: Int, type: GuestType, snapname: String,
    ): ApiResult<String> = call(serverId) { api ->
        api.rollbackSnapshot(node, type.apiPath, vmid, snapname)
    }

    override suspend fun deleteSnapshot(
        serverId: Long, node: String, vmid: Int, type: GuestType, snapname: String,
    ): ApiResult<Unit> = call(serverId) { api ->
        api.deleteSnapshot(node, type.apiPath, vmid, snapname)
    }

    override suspend fun createBackup(
        serverId: Long, node: String, vmid: Int, storage: String?, mode: String, compress: String?,
        protected: Boolean, notesTemplate: String?,
    ): ApiResult<String> = call(serverId) { api ->
        api.createBackup(node, vmid, storage, mode, compress, protected, notesTemplate)
    }

    override suspend fun listBackupStorages(serverId: Long, node: String): ApiResult<List<String>> =
        call(serverId) { api ->
            api.listBackupStorages(node).map { it.storage }
        }

    override suspend fun listBackups(
        serverId: Long, node: String, storage: String, vmid: Int,
    ): ApiResult<List<Backup>> = call(serverId) { api ->
        api.listBackups(node, storage, vmid).map { it.toBackup(storage) }
    }

    override suspend fun restoreBackup(
        serverId: Long, node: String, vmid: Int, archive: String, storage: String?,
    ): ApiResult<String> = call(serverId) { api ->
        api.restoreBackup(node, vmid, archive, storage)
    }

    override suspend fun getVmStatus(
        serverId: Long, node: String, vmid: Int,
    ): ApiResult<VmStatus> = call(serverId) { api ->
        val status = api.getVmStatus(node, vmid)
        val ifaces = try { api.getVmAgentInterfaces(node, vmid) } catch (_: Exception) { emptyList() }
        status.toVmStatus(node, ifaces)
    }

    override suspend fun getVmConfig(
        serverId: Long, node: String, vmid: Int,
    ): ApiResult<VmConfig> = call(serverId) { api ->
        api.getVmConfig(node, vmid).toVmConfig()
    }

    override suspend fun setVmConfig(
        serverId: Long, node: String, vmid: Int, params: Map<String, String>,
    ): ApiResult<Unit> = call(serverId) { api ->
        api.setVmConfig(node, vmid, params)
    }

    override suspend fun getGuestConfig(
        serverId: Long, node: String, vmid: Int, type: GuestType,
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
