package de.kiefer_networks.proxmoxopen.domain.repository

import de.kiefer_networks.proxmoxopen.domain.model.SearchResult
import de.kiefer_networks.proxmoxopen.domain.result.ApiResult

interface SearchRepository {
    /**
     * Runs a global search across all cluster resources (guests, nodes, storages) on
     * [serverId]. Tokens in [query] are whitespace-separated and AND-combined. Each
     * token matches against vmid (exact/prefix), name (contains, case-insensitive),
     * node (contains), tags (contains), or type (exact: qemu/lxc/storage/node).
     * An empty query yields an empty result list.
     */
    suspend fun search(serverId: Long, query: String): ApiResult<List<SearchResult>>
}
