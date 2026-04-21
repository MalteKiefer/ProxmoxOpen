package de.kiefer_networks.proxmoxopen.domain.repository

import de.kiefer_networks.proxmoxopen.domain.model.AptUpdate
import de.kiefer_networks.proxmoxopen.domain.result.ApiResult

interface AptRepository {
    /** Returns the list of pending package updates on [node]. */
    suspend fun listUpdates(serverId: Long, node: String): ApiResult<List<AptUpdate>>

    /** Refreshes the APT cache. Returns the UPID of the triggered task. */
    suspend fun refresh(serverId: Long, node: String): ApiResult<String>

    /** Starts a dist-upgrade on [node]. Returns the UPID of the triggered task. */
    suspend fun upgradeAll(serverId: Long, node: String): ApiResult<String>
}
