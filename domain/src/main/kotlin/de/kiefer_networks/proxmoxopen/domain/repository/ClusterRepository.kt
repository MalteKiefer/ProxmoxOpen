package de.kiefer_networks.proxmoxopen.domain.repository

import de.kiefer_networks.proxmoxopen.domain.model.Cluster
import de.kiefer_networks.proxmoxopen.domain.model.Node
import de.kiefer_networks.proxmoxopen.domain.model.RrdPoint
import de.kiefer_networks.proxmoxopen.domain.model.RrdTimeframe
import de.kiefer_networks.proxmoxopen.domain.result.ApiResult

interface ClusterRepository {
    suspend fun getCluster(serverId: Long): ApiResult<Cluster>
    suspend fun getNode(serverId: Long, node: String): ApiResult<Node>
    suspend fun nodeAction(serverId: Long, node: String, command: String): ApiResult<Unit>

    suspend fun getNodeRrd(
        serverId: Long,
        node: String,
        timeframe: RrdTimeframe,
    ): ApiResult<List<RrdPoint>>
}
