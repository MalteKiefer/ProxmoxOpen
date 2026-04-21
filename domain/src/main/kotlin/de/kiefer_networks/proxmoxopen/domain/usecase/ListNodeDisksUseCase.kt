package de.kiefer_networks.proxmoxopen.domain.usecase

import de.kiefer_networks.proxmoxopen.domain.model.DiskInfo
import de.kiefer_networks.proxmoxopen.domain.model.SmartReport
import de.kiefer_networks.proxmoxopen.domain.repository.NodeDiskRepository
import de.kiefer_networks.proxmoxopen.domain.result.ApiResult
import javax.inject.Inject

class ListNodeDisksUseCase @Inject constructor(
    private val repo: NodeDiskRepository,
) {
    suspend operator fun invoke(serverId: Long, node: String): ApiResult<List<DiskInfo>> =
        repo.listDisks(serverId, node)
}

class GetDiskSmartUseCase @Inject constructor(
    private val repo: NodeDiskRepository,
) {
    suspend operator fun invoke(
        serverId: Long,
        node: String,
        devpath: String,
    ): ApiResult<SmartReport> = repo.getSmart(serverId, node, devpath)
}
