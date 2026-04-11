package app.proxmoxopen.domain.usecase

import app.proxmoxopen.domain.model.ProxmoxTask
import app.proxmoxopen.domain.repository.TaskRepository
import app.proxmoxopen.domain.result.ApiResult
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
