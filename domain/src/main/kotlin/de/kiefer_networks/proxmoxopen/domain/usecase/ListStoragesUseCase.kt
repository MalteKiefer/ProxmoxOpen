package de.kiefer_networks.proxmoxopen.domain.usecase

import de.kiefer_networks.proxmoxopen.domain.model.StorageInfo
import de.kiefer_networks.proxmoxopen.domain.repository.StorageRepository
import de.kiefer_networks.proxmoxopen.domain.result.ApiResult
import javax.inject.Inject

class ListStoragesUseCase @Inject constructor(
    private val storage: StorageRepository,
) {
    suspend operator fun invoke(serverId: Long, node: String): ApiResult<List<StorageInfo>> =
        storage.listStorages(serverId, node)
}
