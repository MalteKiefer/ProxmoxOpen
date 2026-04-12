package app.proxmoxopen.domain.repository

import app.proxmoxopen.domain.model.ProxmoxTask
import app.proxmoxopen.domain.model.TaskLogLine
import app.proxmoxopen.domain.result.ApiResult
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    suspend fun listTasks(serverId: Long, node: String, limit: Int = 50): ApiResult<List<ProxmoxTask>>
    suspend fun listTasksForVmid(serverId: Long, node: String, vmid: Int, limit: Int = 50): ApiResult<List<ProxmoxTask>>
    suspend fun getTask(serverId: Long, node: String, upid: String): ApiResult<ProxmoxTask>

    suspend fun getTaskLog(
        serverId: Long,
        node: String,
        upid: String,
        start: Int = 0,
        limit: Int = 500,
    ): ApiResult<List<TaskLogLine>>

    /** Polls `/status` until the task is no longer running; emits each state. */
    fun streamTask(serverId: Long, node: String, upid: String): Flow<ApiResult<ProxmoxTask>>
}
