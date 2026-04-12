package de.kiefer_networks.proxmoxopen.domain.usecase

import de.kiefer_networks.proxmoxopen.domain.model.ProxmoxTask
import de.kiefer_networks.proxmoxopen.domain.repository.TaskRepository
import de.kiefer_networks.proxmoxopen.domain.result.ApiResult
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class StreamTaskUseCase @Inject constructor(
    private val tasks: TaskRepository,
) {
    operator fun invoke(
        serverId: Long,
        node: String,
        upid: String,
    ): Flow<ApiResult<ProxmoxTask>> = tasks.streamTask(serverId, node, upid)
}
