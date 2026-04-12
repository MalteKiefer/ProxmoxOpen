package app.proxmoxopen.domain.repository

import app.proxmoxopen.domain.model.StorageInfo
import app.proxmoxopen.domain.result.ApiResult

interface StorageRepository {
    suspend fun listStorages(serverId: Long, node: String): ApiResult<List<StorageInfo>>
}
