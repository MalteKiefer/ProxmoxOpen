package app.proxmoxopen.domain.usecase

import app.proxmoxopen.domain.model.Server
import app.proxmoxopen.domain.repository.ServerRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ListServersUseCase @Inject constructor(
    private val servers: ServerRepository,
) {
    operator fun invoke(): Flow<List<Server>> = servers.observeAll()
}
