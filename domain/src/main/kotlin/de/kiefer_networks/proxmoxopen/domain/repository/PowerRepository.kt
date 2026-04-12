package de.kiefer_networks.proxmoxopen.domain.repository

import de.kiefer_networks.proxmoxopen.domain.model.GuestType
import de.kiefer_networks.proxmoxopen.domain.model.PowerAction
import de.kiefer_networks.proxmoxopen.domain.result.ApiResult

interface PowerRepository {
    /** Returns the UPID of the triggered task. */
    suspend fun execute(
        serverId: Long,
        node: String,
        vmid: Int,
        type: GuestType,
        action: PowerAction,
    ): ApiResult<String>
}
