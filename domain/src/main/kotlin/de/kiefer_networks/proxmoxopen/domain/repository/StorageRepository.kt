package de.kiefer_networks.proxmoxopen.domain.repository

import de.kiefer_networks.proxmoxopen.domain.model.StorageInfo
import de.kiefer_networks.proxmoxopen.domain.result.ApiResult

interface StorageRepository {
    suspend fun listStorages(serverId: Long, node: String): ApiResult<List<StorageInfo>>
}
