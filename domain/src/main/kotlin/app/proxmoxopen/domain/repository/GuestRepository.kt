package app.proxmoxopen.domain.repository

import app.proxmoxopen.domain.model.Guest
import app.proxmoxopen.domain.model.GuestType
import app.proxmoxopen.domain.model.RrdPoint
import app.proxmoxopen.domain.model.RrdTimeframe
import app.proxmoxopen.domain.result.ApiResult

interface GuestRepository {
    suspend fun listGuests(serverId: Long): ApiResult<List<Guest>>
    suspend fun getGuest(
        serverId: Long,
        node: String,
        vmid: Int,
        type: GuestType,
    ): ApiResult<Guest>
    suspend fun getGuestRrd(
        serverId: Long,
        node: String,
        vmid: Int,
        type: GuestType,
        timeframe: RrdTimeframe,
    ): ApiResult<List<RrdPoint>>
}
