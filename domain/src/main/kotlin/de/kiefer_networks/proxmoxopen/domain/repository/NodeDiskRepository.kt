package de.kiefer_networks.proxmoxopen.domain.repository

import de.kiefer_networks.proxmoxopen.domain.model.DiskInfo
import de.kiefer_networks.proxmoxopen.domain.model.SmartReport
import de.kiefer_networks.proxmoxopen.domain.result.ApiResult

/** Read-only access to node disk inventory and SMART reports. */
interface NodeDiskRepository {
    suspend fun listDisks(serverId: Long, node: String): ApiResult<List<DiskInfo>>
    suspend fun getSmart(serverId: Long, node: String, devpath: String): ApiResult<SmartReport>
}
