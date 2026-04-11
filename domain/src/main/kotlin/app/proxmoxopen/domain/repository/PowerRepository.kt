package app.proxmoxopen.domain.repository

import app.proxmoxopen.domain.model.GuestType
import app.proxmoxopen.domain.model.PowerAction
import app.proxmoxopen.domain.result.ApiResult

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
