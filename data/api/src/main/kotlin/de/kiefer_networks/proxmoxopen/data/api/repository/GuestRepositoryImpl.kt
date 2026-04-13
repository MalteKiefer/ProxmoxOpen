package de.kiefer_networks.proxmoxopen.data.api.repository

import de.kiefer_networks.proxmoxopen.data.api.ProxmoxApiClient
import de.kiefer_networks.proxmoxopen.data.api.mapper.toBackup
import de.kiefer_networks.proxmoxopen.data.api.mapper.toContainerStatus
import de.kiefer_networks.proxmoxopen.data.api.mapper.toVmStatus
import de.kiefer_networks.proxmoxopen.data.api.mapper.toVmConfig
import de.kiefer_networks.proxmoxopen.data.api.mapper.toGuestConfig
import de.kiefer_networks.proxmoxopen.data.api.mapper.toDomain
import de.kiefer_networks.proxmoxopen.data.api.mapper.toSnapshot
import de.kiefer_networks.proxmoxopen.data.api.mapper.toGuestOrNull
import de.kiefer_networks.proxmoxopen.data.api.session.ProxmoxSessionManager
import de.kiefer_networks.proxmoxopen.domain.model.Credentials
import de.kiefer_networks.proxmoxopen.domain.model.Backup
import de.kiefer_networks.proxmoxopen.domain.model.ContainerStatus
import de.kiefer_networks.proxmoxopen.domain.model.Guest
import de.kiefer_networks.proxmoxopen.domain.model.GuestConfig
import de.kiefer_networks.proxmoxopen.domain.model.GuestType
import de.kiefer_networks.proxmoxopen.domain.model.Snapshot
import de.kiefer_networks.proxmoxopen.domain.model.VmConfig
import de.kiefer_networks.proxmoxopen.domain.model.VmStatus
import de.kiefer_networks.proxmoxopen.domain.model.Realm
import de.kiefer_networks.proxmoxopen.domain.model.RrdPoint
import de.kiefer_networks.proxmoxopen.domain.model.RrdTimeframe
import de.kiefer_networks.proxmoxopen.domain.repository.GuestRepository
import de.kiefer_networks.proxmoxopen.domain.repository.ServerRepository
import de.kiefer_networks.proxmoxopen.domain.result.ApiError
import de.kiefer_networks.proxmoxopen.domain.result.ApiResult
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
        // Status only — agent IPs loaded separately to avoid blocking
        api.getVmStatus(node, vmid).toVmStatus(node, emptyList())
    }

    override suspend fun getVmAgentIps(
        serverId: Long, node: String, vmid: Int,
    ): ApiResult<List<de.kiefer_networks.proxmoxopen.domain.model.InterfaceIp>> = call(serverId) { api ->
        api.getVmAgentInterfaces(node, vmid).flatMap { iface ->
            val inet4 = iface.ip_addresses?.firstOrNull { it.ip_address_type == "ipv4" }
            val inet6 = iface.ip_addresses?.firstOrNull { it.ip_address_type == "ipv6" }
            if (inet4 != null || inet6 != null) {
                listOf(de.kiefer_networks.proxmoxopen.domain.model.InterfaceIp(
                    name = iface.name ?: "?",
                    hwaddr = iface.hardware_address,
                    inet = inet4?.let { "${it.ip_address}/${it.prefix}" },
                    inet6 = inet6?.let { "${it.ip_address}/${it.prefix}" },
                ))
            } else emptyList()
        }
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

    override suspend fun deleteGuest(
        serverId: Long,
        node: String,
        vmid: Int,
        type: GuestType,
        purge: Boolean,
        destroyUnreferencedDisks: Boolean,
    ): ApiResult<String> = call(serverId) { api ->
        api.deleteGuest(node, type.apiPath, vmid, purge, destroyUnreferencedDisks)
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

    override suspend fun migrateGuest(
        serverId: Long,
        node: String,
        vmid: Int,
        type: de.kiefer_networks.proxmoxopen.domain.model.GuestType,
        target: String,
        online: Boolean,
    ): ApiResult<String> = call(serverId) { api ->
        api.migrateGuest(node, type.apiPath, vmid, target, online)
    }

    override suspend fun cloneGuest(
        serverId: Long,
        node: String,
        vmid: Int,
        type: GuestType,
        newid: Int,
        name: String?,
        full: Boolean,
        target: String?,
        storage: String?,
    ): ApiResult<String> = call(serverId) { api ->
        api.cloneGuest(node, type.apiPath, vmid, newid, name, full, target, storage)
    }

    override suspend fun listNodes(serverId: Long): ApiResult<List<String>> =
        call(serverId) { api ->
            api.getNodes().map { it.node }
        }

    override suspend fun listStorages(serverId: Long, node: String): ApiResult<List<String>> =
        call(serverId) { api ->
            api.listStorages(node).map { it.storage }
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

