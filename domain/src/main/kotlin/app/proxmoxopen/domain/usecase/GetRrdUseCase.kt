package app.proxmoxopen.domain.usecase

import app.proxmoxopen.domain.model.GuestType
import app.proxmoxopen.domain.model.RrdPoint
import app.proxmoxopen.domain.model.RrdTimeframe
import app.proxmoxopen.domain.repository.ClusterRepository
import app.proxmoxopen.domain.repository.GuestRepository
import app.proxmoxopen.domain.result.ApiResult
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
