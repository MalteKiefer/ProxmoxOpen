package de.kiefer_networks.proxmoxopen.data.api.repository

import de.kiefer_networks.proxmoxopen.data.api.ProxmoxApiClient
import de.kiefer_networks.proxmoxopen.data.api.session.ProxmoxSessionManager
import de.kiefer_networks.proxmoxopen.domain.model.BackupJob
import de.kiefer_networks.proxmoxopen.domain.model.Credentials
import de.kiefer_networks.proxmoxopen.domain.model.Realm
import de.kiefer_networks.proxmoxopen.domain.repository.BackupJobRepository
import de.kiefer_networks.proxmoxopen.domain.repository.ServerRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupJobRepositoryImpl @Inject constructor(
    private val sessions: ProxmoxSessionManager,
    private val servers: ServerRepository,
) : BackupJobRepository {

    override suspend fun listJobs(serverId: Long): List<BackupJob> =
        withApi(serverId) { api ->
            api.listBackupJobs().map { dto ->
                BackupJob(
                    id = dto.id,
                    enabled = dto.enabled != 0,
                    schedule = dto.schedule,
                    storage = dto.storage,
                    vmid = dto.vmid,
                    mode = dto.mode,
                    compress = dto.compress,
                    mailto = dto.mailto,
                    mailnotification = dto.mailnotification,
                    node = dto.node,
                    pool = dto.pool,
                    allGuests = dto.all == 1,
                    notes = dto.notes,
                    nextRun = dto.nextRun,
                )
            }
        }

    override suspend fun runJob(serverId: Long, id: String) {
        withApi(serverId) { api -> api.runBackupJob(id) }
    }

    override suspend fun createJob(serverId: Long, params: Map<String, String>) {
        withApi(serverId) { api -> api.createBackupJob(params) }
    }

    override suspend fun updateJob(serverId: Long, id: String, params: Map<String, String>) {
        withApi(serverId) { api -> api.updateBackupJob(id, params) }
    }

    override suspend fun deleteJob(serverId: Long, id: String) {
        withApi(serverId) { api -> api.deleteBackupJob(id) }
    }

    override suspend fun listBackupStorages(serverId: Long, node: String): List<String> =
        withApi(serverId) { api ->
            api.listBackupStorages(node).map { it.storage }
        }

    private suspend fun <T> withApi(
        serverId: Long,
        block: suspend (ProxmoxApiClient) -> T,
    ): T {
        val server = servers.getById(serverId)
            ?: error("server $serverId missing")
        val credentials = tokenCredentialsIfNeeded(serverId, server.realm, server.username, server.tokenId)
        return block(sessions.apiClient(server, credentials))
    }

    private suspend fun tokenCredentialsIfNeeded(
        serverId: Long,
        realm: Realm,
        username: String?,
        tokenId: String?,
    ): Credentials? {
        if (realm != Realm.PVE_TOKEN) return null
        val secret = servers.getTokenSecret(serverId) ?: return null
        return Credentials.ApiToken(
            username = username ?: "root",
            realm = realm,
            tokenId = tokenId ?: "",
            tokenSecret = secret,
        )
    }
}
