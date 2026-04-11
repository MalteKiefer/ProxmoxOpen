package app.proxmoxopen.data.api.repository

import app.proxmoxopen.data.api.mapper.toDomain
import app.proxmoxopen.data.api.session.ProxmoxSessionManager
import app.proxmoxopen.domain.model.Credentials
import app.proxmoxopen.domain.model.ProxmoxTask
import app.proxmoxopen.domain.model.Realm
import app.proxmoxopen.domain.model.TaskState
import app.proxmoxopen.domain.repository.ServerRepository
import app.proxmoxopen.domain.repository.TaskRepository
import app.proxmoxopen.domain.result.ApiResult
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val sessions: ProxmoxSessionManager,
    private val servers: ServerRepository,
) : TaskRepository {

    override suspend fun listTasks(
        serverId: Long,
        node: String,
        limit: Int,
    ): ApiResult<List<ProxmoxTask>> = runCatchingApi {
        val api = apiClientFor(serverId)
        api.listTasks(node, limit).map { it.toDomain() }
    }

    override suspend fun getTask(
        serverId: Long,
        node: String,
        upid: String,
    ): ApiResult<ProxmoxTask> = runCatchingApi {
        apiClientFor(serverId).getTaskStatus(node, upid).toDomain()
    }

    override fun streamTask(
        serverId: Long,
        node: String,
        upid: String,
    ): Flow<ApiResult<ProxmoxTask>> = flow {
        while (true) {
            val result = getTask(serverId, node, upid)
            emit(result)
            if (result !is ApiResult.Success || result.value.state != TaskState.RUNNING) break
            delay(POLL_INTERVAL_MILLIS)
        }
    }

    private suspend fun apiClientFor(serverId: Long): app.proxmoxopen.data.api.ProxmoxApiClient {
        val server = servers.getById(serverId) ?: error("server $serverId missing")
        val credentials: Credentials? = if (server.realm == Realm.PVE_TOKEN) {
            val secret = servers.getTokenSecret(serverId) ?: error("token secret missing")
            Credentials.ApiToken(
                username = server.username ?: "root",
                realm = server.realm,
                tokenId = server.tokenId ?: "",
                tokenSecret = secret,
            )
        } else {
            null
        }
        return sessions.apiClient(server, credentials)
    }

    companion object {
        private const val POLL_INTERVAL_MILLIS = 2_000L
    }
}
