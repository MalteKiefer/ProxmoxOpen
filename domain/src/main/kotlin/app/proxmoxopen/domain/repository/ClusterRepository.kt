package app.proxmoxopen.domain.repository

import app.proxmoxopen.domain.model.Cluster
import app.proxmoxopen.domain.model.Node
import app.proxmoxopen.domain.model.RrdPoint
import app.proxmoxopen.domain.model.RrdTimeframe
import app.proxmoxopen.domain.result.ApiResult

interface ClusterRepository {
    suspend fun getCluster(serverId: Long): ApiResult<Cluster>
    suspend fun getNode(serverId: Long, node: String): ApiResult<Node>
    suspend fun getNodeRrd(
        serverId: Long,
        node: String,
        timeframe: RrdTimeframe,
    ): ApiResult<List<RrdPoint>>
}
