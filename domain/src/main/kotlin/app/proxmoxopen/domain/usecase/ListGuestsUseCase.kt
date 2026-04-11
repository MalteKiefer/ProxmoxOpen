package app.proxmoxopen.domain.usecase

import app.proxmoxopen.domain.model.Guest
import app.proxmoxopen.domain.repository.GuestRepository
import app.proxmoxopen.domain.result.ApiResult
import javax.inject.Inject

class ListGuestsUseCase @Inject constructor(
    private val guests: GuestRepository,
) {
    suspend operator fun invoke(serverId: Long): ApiResult<List<Guest>> =
        guests.listGuests(serverId)
}
