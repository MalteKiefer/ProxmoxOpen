package app.proxmoxopen.domain.usecase

import app.proxmoxopen.domain.model.StorageInfo
import app.proxmoxopen.domain.repository.StorageRepository
import app.proxmoxopen.domain.result.ApiResult
import javax.inject.Inject

class ListStoragesUseCase @Inject constructor(
    private val storage: StorageRepository,
) {
    suspend operator fun invoke(serverId: Long, node: String): ApiResult<List<StorageInfo>> =
        storage.listStorages(serverId, node)
}
