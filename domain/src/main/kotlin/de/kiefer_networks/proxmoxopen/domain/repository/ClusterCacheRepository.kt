package de.kiefer_networks.proxmoxopen.domain.repository

/**
 * Persisted snapshot of a single cluster-resource row (VM / LXC).
 *
 * Kept intentionally small and decoupled from the rich [de.kiefer_networks.proxmoxopen.domain.model.Guest]
 * model — the cache only needs to feed the dashboard fallback path. Consumers are
 * expected to map this into their own domain types.
 */
data class ClusterResourceSnapshot(
    val type: String,
    val node: String,
    val vmid: Int?,
    val name: String?,
    val status: String?,
    val cpu: Double?,
    val mem: Long?,
    val maxmem: Long?,
    val diskUsed: Long?,
    val maxdisk: Long?,
    val tags: String?,
)

/**
 * A whole snapshot set for one server, with the capture time of the oldest row.
 *
 * [capturedAt] is an epoch-millisecond timestamp and is `null` when the cache is empty.
 */
data class CachedClusterSnapshot(
    val resources: List<ClusterResourceSnapshot>,
    val capturedAt: Long,
)

/**
 * Read/write access to the offline cluster-resource cache.
 *
 * Implementations must be safe to call from coroutines and should persist a fresh batch
 * with a single [capturedAt] timestamp per save so the UI can render an honest
 * "cached HH:mm" label.
 */
interface ClusterCacheRepository {
    suspend fun snapshot(serverId: Long): CachedClusterSnapshot?
    suspend fun save(serverId: Long, list: List<ClusterResourceSnapshot>)
    suspend fun oldestCapture(serverId: Long): Long?
}
