package app.proxmoxopen.domain.usecase

import app.proxmoxopen.domain.model.Cluster
import app.proxmoxopen.domain.repository.ClusterRepository
import app.proxmoxopen.domain.result.ApiResult

class GetClusterUseCase(
    private val cluster: ClusterRepository,
) {
    suspend operator fun invoke(serverId: Long): ApiResult<Cluster> = cluster.getCluster(serverId)
}
