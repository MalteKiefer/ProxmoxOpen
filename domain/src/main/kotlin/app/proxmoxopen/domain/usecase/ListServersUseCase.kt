package app.proxmoxopen.domain.usecase

import app.proxmoxopen.domain.model.Server
import app.proxmoxopen.domain.repository.ServerRepository
import kotlinx.coroutines.flow.Flow

class ListServersUseCase(
    private val servers: ServerRepository,
) {
    operator fun invoke(): Flow<List<Server>> = servers.observeAll()
}
