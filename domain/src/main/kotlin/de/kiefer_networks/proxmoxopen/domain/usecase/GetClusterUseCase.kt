package de.kiefer_networks.proxmoxopen.domain.usecase

import de.kiefer_networks.proxmoxopen.domain.model.Cluster
import de.kiefer_networks.proxmoxopen.domain.repository.ClusterRepository
import de.kiefer_networks.proxmoxopen.domain.result.ApiResult
import javax.inject.Inject

class GetClusterUseCase @Inject constructor(
    private val cluster: ClusterRepository,
) {
    suspend operator fun invoke(serverId: Long): ApiResult<Cluster> = cluster.getCluster(serverId)
}
