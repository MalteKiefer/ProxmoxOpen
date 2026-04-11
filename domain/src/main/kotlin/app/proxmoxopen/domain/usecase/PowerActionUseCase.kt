package app.proxmoxopen.domain.usecase

import app.proxmoxopen.domain.model.GuestType
import app.proxmoxopen.domain.model.PowerAction
import app.proxmoxopen.domain.repository.PowerRepository
import app.proxmoxopen.domain.result.ApiResult
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
