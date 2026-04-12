package de.kiefer_networks.proxmoxopen.domain.usecase

import de.kiefer_networks.proxmoxopen.domain.model.GuestType
import de.kiefer_networks.proxmoxopen.domain.model.PowerAction
import de.kiefer_networks.proxmoxopen.domain.repository.PowerRepository
import de.kiefer_networks.proxmoxopen.domain.result.ApiResult
import javax.inject.Inject

class PowerActionUseCase @Inject constructor(
    private val power: PowerRepository,
) {
    suspend operator fun invoke(
        serverId: Long,
        node: String,
        vmid: Int,
        type: GuestType,
        action: PowerAction,
    ): ApiResult<String> = power.execute(serverId, node, vmid, type, action)
}
