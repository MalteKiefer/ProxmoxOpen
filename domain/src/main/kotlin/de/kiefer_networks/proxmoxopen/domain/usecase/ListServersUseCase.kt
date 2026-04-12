package de.kiefer_networks.proxmoxopen.domain.usecase

import de.kiefer_networks.proxmoxopen.domain.model.Server
import de.kiefer_networks.proxmoxopen.domain.repository.ServerRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ListServersUseCase @Inject constructor(
    private val servers: ServerRepository,
) {
    operator fun invoke(): Flow<List<Server>> = servers.observeAll()
}
