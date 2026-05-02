package de.kiefer_networks.proxmoxopen.domain.repository

import de.kiefer_networks.proxmoxopen.domain.model.Server
import kotlinx.coroutines.flow.Flow

interface ServerRepository {
    fun observeAll(): Flow<List<Server>>
    suspend fun getById(id: Long): Server?
    suspend fun add(server: Server, tokenSecret: String?): Long
    suspend fun update(server: Server)
    suspend fun delete(server: Server)
    suspend fun touchLastConnected(id: Long)

    /** Returns the stored token secret for a PVE_TOKEN server, or null if absent. */
    suspend fun getTokenSecret(serverId: Long): String?
}
