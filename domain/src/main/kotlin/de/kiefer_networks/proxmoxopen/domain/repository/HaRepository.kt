package de.kiefer_networks.proxmoxopen.domain.repository

import de.kiefer_networks.proxmoxopen.domain.model.HaClusterStatus
import de.kiefer_networks.proxmoxopen.domain.model.HaGroup
import de.kiefer_networks.proxmoxopen.domain.model.HaResource
import de.kiefer_networks.proxmoxopen.domain.result.ApiResult

/** Read-only access to Proxmox HA manager endpoints. */
interface HaRepository {
    suspend fun getStatus(serverId: Long): ApiResult<HaClusterStatus>
    suspend fun getResources(serverId: Long): ApiResult<List<HaResource>>
    suspend fun getGroups(serverId: Long): ApiResult<List<HaGroup>>
}
