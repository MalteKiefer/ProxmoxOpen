package de.kiefer_networks.proxmoxopen.domain.repository

import de.kiefer_networks.proxmoxopen.domain.model.NodeNetworkIface
import de.kiefer_networks.proxmoxopen.domain.result.ApiResult

/**
 * Read-only access to a node's host network interfaces.
 *
 * There are intentionally no mutation methods here. Editing bridges/bonds from
 * a phone is too risky — users must use the Proxmox web UI for changes.
 */
interface NodeNetworkRepository {
    suspend fun listInterfaces(serverId: Long, node: String): ApiResult<List<NodeNetworkIface>>
}
