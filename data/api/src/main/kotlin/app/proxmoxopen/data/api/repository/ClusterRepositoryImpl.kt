package app.proxmoxopen.data.api.repository

import app.proxmoxopen.data.api.mapper.buildCluster
import app.proxmoxopen.data.api.mapper.toDomain
import app.proxmoxopen.data.api.session.ProxmoxSessionManager
import app.proxmoxopen.domain.model.Cluster
import app.proxmoxopen.domain.model.Node
import app.proxmoxopen.domain.model.RrdPoint
import app.proxmoxopen.domain.model.RrdTimeframe
import app.proxmoxopen.domain.repository.ClusterRepository
import app.proxmoxopen.domain.repository.ServerRepository
import app.proxmoxopen.domain.result.ApiError
import app.proxmoxopen.domain.result.ApiResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClusterRepositoryImpl @Inject constructor(
    private val sessions: ProxmoxSessionManager,
    private val servers: ServerRepository,
) : ClusterRepository {

    override suspend fun getCluster(serverId: Long): ApiResult<Cluster> = execute(serverId) { api ->
        val status = api.getClusterStatus()
        val nodes = api.getNodes()
        buildCluster(status, nodes)
    }

    override suspend fun getNode(serverId: Long, node: String): ApiResult<Node> = execute(serverId) { api ->
        api.getNodeStatus(node).toDomain(node)
    }

    override suspend fun nodeAction(serverId: Long, node: String, command: String): ApiResult<Unit> =
        execute(serverId) { api -> api.nodeAction(node, command) }

    override suspend fun getNodeRrd(
        serverId: Long,
        node: String,
        timeframe: RrdTimeframe,
    ): ApiResult<List<RrdPoint>> = execute(serverId) { api ->
        api.getNodeRrd(node, timeframe.apiKey).map { it.toDomain() }
    }

    private suspend fun <T> execute(
        serverId: Long,
        block: suspend (app.proxmoxopen.data.api.ProxmoxApiClient) -> T,
    ): ApiResult<T> = runCatchingApi {
        val server = servers.getById(serverId)
            ?: return@runCatchingApi throw IllegalStateException("server $serverId missing")
        val credentials = when (server.realm) {
            app.proxmoxopen.domain.model.Realm.PVE_TOKEN -> {
                val secret = servers.getTokenSecret(serverId)
                    ?: return ApiResult.Failure(ApiError.Auth("token missing"))
                app.proxmoxopen.domain.model.Credentials.ApiToken(
                    username = server.username ?: "root",
                    realm = server.realm,
                    tokenId = server.tokenId ?: "",
                    tokenSecret = secret,
                )
            }
            else -> null
        }
        block(sessions.apiClient(server, credentials))
    }
}

internal inline fun <T> runCatchingApi(block: () -> T): ApiResult<T> = try {
    ApiResult.Success(block())
} catch (e: app.proxmoxopen.data.api.ProxmoxHttpException) {
    when (e.code) {
        401, 403 -> ApiResult.Failure(ApiError.Auth(e.message ?: "unauthorized"))
        else -> ApiResult.Failure(ApiError.Http(e.code, e.message ?: "http error"))
    }
} catch (e: io.ktor.utils.io.errors.IOException) {
    ApiResult.Failure(ApiError.Network(e.message ?: "network error"))
} catch (e: app.proxmoxopen.data.api.tls.FingerprintMismatchException) {
    ApiResult.Failure(ApiError.FingerprintMismatch(e.expected, e.actual))
} catch (e: kotlinx.serialization.SerializationException) {
    ApiResult.Failure(ApiError.Parse(e.message ?: "parse error"))
} catch (e: IllegalStateException) {
    ApiResult.Failure(ApiError.Unknown(e.message ?: "state error"))
}
