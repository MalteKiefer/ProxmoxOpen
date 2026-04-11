package app.proxmoxopen.domain.repository

import app.proxmoxopen.domain.model.ProxmoxTask
import app.proxmoxopen.domain.result.ApiResult
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    suspend fun listTasks(serverId: Long, node: String, limit: Int = 50): ApiResult<List<ProxmoxTask>>
    suspend fun getTask(serverId: Long, node: String, upid: String): ApiResult<ProxmoxTask>

    /** Polls `/status` until the task is no longer running; emits each state. */
    fun streamTask(serverId: Long, node: String, upid: String): Flow<ApiResult<ProxmoxTask>>
}
