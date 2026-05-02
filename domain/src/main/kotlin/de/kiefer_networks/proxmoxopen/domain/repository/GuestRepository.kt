package de.kiefer_networks.proxmoxopen.domain.repository

import de.kiefer_networks.proxmoxopen.domain.model.Backup
import de.kiefer_networks.proxmoxopen.domain.model.ContainerStatus
import de.kiefer_networks.proxmoxopen.domain.model.Guest
import de.kiefer_networks.proxmoxopen.domain.model.VmConfig
import de.kiefer_networks.proxmoxopen.domain.model.InterfaceIp
import de.kiefer_networks.proxmoxopen.domain.model.VmStatus
import de.kiefer_networks.proxmoxopen.domain.model.GuestConfig
import de.kiefer_networks.proxmoxopen.domain.model.GuestType
import de.kiefer_networks.proxmoxopen.domain.model.Snapshot
import de.kiefer_networks.proxmoxopen.domain.model.RrdPoint
import de.kiefer_networks.proxmoxopen.domain.model.RrdTimeframe
import de.kiefer_networks.proxmoxopen.domain.result.ApiResult

interface GuestRepository {
    suspend fun listGuests(serverId: Long): ApiResult<List<Guest>>
    suspend fun getGuest(
        serverId: Long,
        node: String,
        vmid: Int,
        type: GuestType,
    ): ApiResult<Guest>
    suspend fun getContainerStatus(
        serverId: Long,
        node: String,
        vmid: Int,
    ): ApiResult<ContainerStatus>

    suspend fun listSnapshots(
        serverId: Long,
        node: String,
        vmid: Int,
        type: GuestType,
    ): ApiResult<List<Snapshot>>

    suspend fun createSnapshot(
        serverId: Long,
        node: String,
        vmid: Int,
        type: GuestType,
        snapname: String,
        description: String?,
    ): ApiResult<String>

    suspend fun rollbackSnapshot(
        serverId: Long,
        node: String,
        vmid: Int,
        type: GuestType,
        snapname: String,
    ): ApiResult<String>

    suspend fun deleteSnapshot(
        serverId: Long,
        node: String,
        vmid: Int,
        type: GuestType,
        snapname: String,
    ): ApiResult<Unit>

    suspend fun createBackup(
        serverId: Long,
        node: String,
        vmid: Int,
        storage: String?,
        mode: String,
        compress: String?,
        protected: Boolean = false,
        notesTemplate: String? = null,
    ): ApiResult<String>

    suspend fun listBackupStorages(serverId: Long, node: String): ApiResult<List<String>>

    suspend fun listBackups(
        serverId: Long, node: String, storage: String, vmid: Int,
    ): ApiResult<List<Backup>>

    suspend fun restoreBackup(
        serverId: Long, node: String, vmid: Int, archive: String, storage: String?,
    ): ApiResult<String>

    // --- QEMU VM specific ---
    suspend fun getVmStatus(serverId: Long, node: String, vmid: Int): ApiResult<VmStatus>
    suspend fun getVmAgentIps(serverId: Long, node: String, vmid: Int): ApiResult<List<InterfaceIp>>
    suspend fun getVmConfig(serverId: Long, node: String, vmid: Int): ApiResult<VmConfig>
    suspend fun setVmConfig(serverId: Long, node: String, vmid: Int, params: Map<String, String>): ApiResult<Unit>

    // --- LXC config ---
    suspend fun getGuestConfig(
        serverId: Long,
        node: String,
        vmid: Int,
        type: GuestType,
    ): ApiResult<GuestConfig>

    suspend fun setGuestConfig(
        serverId: Long,
        node: String,
        vmid: Int,
        type: GuestType,
        params: Map<String, String>,
    ): ApiResult<Unit>

    suspend fun deleteGuest(
        serverId: Long,
        node: String,
        vmid: Int,
        type: GuestType,
        purge: Boolean,
        destroyUnreferencedDisks: Boolean,
    ): ApiResult<String>

    suspend fun getGuestRrd(
        serverId: Long,
        node: String,
        vmid: Int,
        type: GuestType,
        timeframe: RrdTimeframe,
    ): ApiResult<List<RrdPoint>>

    suspend fun migrateGuest(
        serverId: Long,
        node: String,
        vmid: Int,
        type: GuestType,
        target: String,
        online: Boolean,
    ): ApiResult<String>

    // --- Clone ---
    suspend fun cloneGuest(
        serverId: Long,
        node: String,
        vmid: Int,
        type: GuestType,
        newid: Int,
        name: String? = null,
        full: Boolean = true,
        target: String? = null,
        storage: String? = null,
    ): ApiResult<String>

    suspend fun listNodes(serverId: Long): ApiResult<List<String>>

    suspend fun listStorages(serverId: Long, node: String): ApiResult<List<String>>
}
