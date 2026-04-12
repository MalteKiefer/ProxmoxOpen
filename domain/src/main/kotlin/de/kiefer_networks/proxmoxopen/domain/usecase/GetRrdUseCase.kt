package de.kiefer_networks.proxmoxopen.domain.usecase

import de.kiefer_networks.proxmoxopen.domain.model.GuestType
import de.kiefer_networks.proxmoxopen.domain.model.RrdPoint
import de.kiefer_networks.proxmoxopen.domain.model.RrdTimeframe
import de.kiefer_networks.proxmoxopen.domain.repository.ClusterRepository
import de.kiefer_networks.proxmoxopen.domain.repository.GuestRepository
import de.kiefer_networks.proxmoxopen.domain.result.ApiResult
import javax.inject.Inject

class GetNodeRrdUseCase @Inject constructor(
    private val cluster: ClusterRepository,
) {
    suspend operator fun invoke(
        serverId: Long,
        node: String,
        timeframe: RrdTimeframe,
    ): ApiResult<List<RrdPoint>> = cluster.getNodeRrd(serverId, node, timeframe)
}

class GetGuestRrdUseCase @Inject constructor(
    private val guests: GuestRepository,
) {
    suspend operator fun invoke(
        serverId: Long,
        node: String,
        vmid: Int,
        type: GuestType,
        timeframe: RrdTimeframe,
    ): ApiResult<List<RrdPoint>> = guests.getGuestRrd(serverId, node, vmid, type, timeframe)
}
