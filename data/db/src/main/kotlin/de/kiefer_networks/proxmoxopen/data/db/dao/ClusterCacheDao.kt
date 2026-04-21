package de.kiefer_networks.proxmoxopen.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import de.kiefer_networks.proxmoxopen.data.db.entity.CachedClusterResourceEntity

/**
 * DAO for the offline cluster-resource cache.
 *
 * The cache is keyed by `(serverId, type, vmid, node)` so that upsert replaces existing
 * rows instead of duplicating them, which keeps the table size bounded to the live
 * inventory of a given server.
 */
@Dao
interface ClusterCacheDao {

    @Upsert
    suspend fun upsertAll(list: List<CachedClusterResourceEntity>)

    @Query("SELECT * FROM cached_cluster_resource WHERE serverId = :serverId")
    suspend fun getForServer(serverId: Long): List<CachedClusterResourceEntity>

    @Query("SELECT MIN(capturedAt) FROM cached_cluster_resource WHERE serverId = :serverId")
    suspend fun oldestCaptureTs(serverId: Long): Long?

    @Query("DELETE FROM cached_cluster_resource WHERE serverId = :serverId")
    suspend fun clearForServer(serverId: Long)
}
