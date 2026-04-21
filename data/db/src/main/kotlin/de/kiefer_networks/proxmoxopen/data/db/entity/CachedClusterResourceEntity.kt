package de.kiefer_networks.proxmoxopen.data.db.entity

import androidx.room.Entity

/**
 * Last-known snapshot of a single cluster resource row (VM / LXC) for a given server.
 *
 * Stored so the dashboard can fall back to the most recent data when the device is
 * offline or the live API call fails. Populated on every successful
 * `listClusterResources()` and read as a whole set per `serverId`.
 *
 * [capturedAt] is an epoch-millisecond timestamp for the whole snapshot batch.
 *
 * Note on [vmid]: Room does not permit NULL values in composite primary keys, so the
 * storage column is NOT NULL. Incoming `null` vmids from the Proxmox API (rare for
 * VM/LXC rows) are mapped to `0` at the repository boundary and represented as `null`
 * in the domain model via [vmid] == 0.
 */
@Entity(
    tableName = "cached_cluster_resource",
    primaryKeys = ["serverId", "type", "vmid", "node"],
)
data class CachedClusterResourceEntity(
    val serverId: Long,
    val type: String,
    val node: String,
    val vmid: Int,
    val name: String?,
    val status: String?,
    val cpu: Double?,
    val mem: Long?,
    val maxmem: Long?,
    val diskUsed: Long?,
    val maxdisk: Long?,
    val tags: String?,
    val capturedAt: Long,
)
