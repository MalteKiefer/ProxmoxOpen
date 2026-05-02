package de.kiefer_networks.proxmoxopen.data.api.repository

import de.kiefer_networks.proxmoxopen.data.db.dao.ServerDao
import de.kiefer_networks.proxmoxopen.data.db.entity.ServerEntity
import de.kiefer_networks.proxmoxopen.data.secrets.SecretStore
import de.kiefer_networks.proxmoxopen.domain.model.Realm
import de.kiefer_networks.proxmoxopen.domain.model.Server
import de.kiefer_networks.proxmoxopen.domain.repository.ServerRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class ServerRepositoryImpl @Inject constructor(
    private val dao: ServerDao,
    private val secrets: SecretStore,
) : ServerRepository {

    override fun observeAll(): Flow<List<Server>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getById(id: Long): Server? = dao.getById(id)?.toDomain()

    override suspend fun add(server: Server, tokenSecret: String?): Long {
        val id = dao.insert(server.toEntity())
        tokenSecret?.let { secrets.put(tokenKey(id), it) }
        // Defensive: passwords are no longer persisted (F-006). Clear any stale entry
        // that might be present from a previous install or earlier app version.
        secrets.remove(passwordKey(id))
        return id
    }

    override suspend fun update(server: Server) {
        dao.update(server.toEntity())
    }

    override suspend fun delete(server: Server) {
        secrets.remove(tokenKey(server.id))
        // Cleanup: remove any historical password blob from older app versions.
        secrets.remove(passwordKey(server.id))
        dao.delete(server.toEntity())
    }

    override suspend fun touchLastConnected(id: Long) {
        dao.touchLastConnected(id, System.currentTimeMillis())
    }

    override suspend fun getTokenSecret(serverId: Long): String? = secrets.get(tokenKey(serverId))

    companion object {
        private fun tokenKey(id: Long) = "server_${id}_token"

        // Retained for cleanup of legacy stored passwords on add()/delete(). Do not
        // reintroduce password persistence — see F-006.
        private fun passwordKey(id: Long) = "server_${id}_password"
    }
}

private fun ServerEntity.toDomain(): Server = Server(
    id = id,
    name = name,
    host = host,
    port = port,
    realm = Realm.fromApiKey(realm) ?: Realm.PAM,
    username = username,
    tokenId = tokenId,
    fingerprintSha256 = fingerprintSha256,
    createdAt = createdAt,
    lastConnectedAt = lastConnectedAt,
)

private fun Server.toEntity(): ServerEntity = ServerEntity(
    id = id,
    name = name,
    host = host,
    port = port,
    realm = realm.apiKey,
    username = username,
    tokenId = tokenId,
    fingerprintSha256 = fingerprintSha256,
    createdAt = createdAt,
    lastConnectedAt = lastConnectedAt,
)
