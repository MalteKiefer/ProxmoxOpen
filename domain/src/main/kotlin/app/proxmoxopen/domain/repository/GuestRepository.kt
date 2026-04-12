package app.proxmoxopen.domain.repository

import app.proxmoxopen.domain.model.Guest
import app.proxmoxopen.domain.model.GuestConfig
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
    suspend fun getGuestConfig(
        serverId: Long,
        node: String,
        vmid: Int,
        type: GuestType,
    ): ApiResult<GuestConfig>

    suspend fun setGuestConfig(
        serverId: Long,
        node: String,
        vmid: Int,
        type: GuestType,
        params: Map<String, String>,
    ): ApiResult<Unit>

    suspend fun getGuestRrd(
        serverId: Long,
        node: String,
        vmid: Int,
        type: GuestType,
        timeframe: RrdTimeframe,
    ): ApiResult<List<RrdPoint>>
}
