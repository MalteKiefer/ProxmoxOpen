package de.kiefer_networks.proxmoxopen.domain.usecase

import de.kiefer_networks.proxmoxopen.domain.model.Guest
import de.kiefer_networks.proxmoxopen.domain.repository.GuestRepository
import de.kiefer_networks.proxmoxopen.domain.result.ApiResult
import javax.inject.Inject

class ListGuestsUseCase @Inject constructor(
    private val guests: GuestRepository,
) {
    suspend operator fun invoke(serverId: Long): ApiResult<List<Guest>> =
        guests.listGuests(serverId)
}
