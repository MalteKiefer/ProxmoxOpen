package de.kiefer_networks.proxmoxopen.data.api.repository

import de.kiefer_networks.proxmoxopen.data.db.dao.ClusterCacheDao
import de.kiefer_networks.proxmoxopen.data.db.entity.CachedClusterResourceEntity
import de.kiefer_networks.proxmoxopen.domain.repository.CachedClusterSnapshot
import de.kiefer_networks.proxmoxopen.domain.repository.ClusterCacheRepository
import de.kiefer_networks.proxmoxopen.domain.repository.ClusterResourceSnapshot
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClusterCacheRepositoryImpl @Inject constructor(
    private val dao: ClusterCacheDao,
) : ClusterCacheRepository {

    override suspend fun snapshot(serverId: Long): CachedClusterSnapshot? {
        val rows = dao.getForServer(serverId)
        if (rows.isEmpty()) return null
        // Use the oldest capturedAt so the banner is honest even if multiple writes
        // interleaved (should not happen, but defensive).
        val oldest = rows.minOf { it.capturedAt }
        return CachedClusterSnapshot(
            resources = rows.map { it.toDomain() },
            capturedAt = oldest,
        )
    }

    override suspend fun save(serverId: Long, list: List<ClusterResourceSnapshot>) {
        val now = System.currentTimeMillis()
        // Clear stale rows first so resources that have disappeared upstream do not
        // linger in the cache.
        dao.clearForServer(serverId)
        if (list.isEmpty()) return
        dao.upsertAll(list.map { it.toEntity(serverId, now) })
    }

    override suspend fun oldestCapture(serverId: Long): Long? = dao.oldestCaptureTs(serverId)
}

private fun CachedClusterResourceEntity.toDomain(): ClusterResourceSnapshot = ClusterResourceSnapshot(
    type = type,
    node = node,
    vmid = vmid.takeIf { it != 0 },
    name = name,
    status = status,
    cpu = cpu,
    mem = mem,
    maxmem = maxmem,
    diskUsed = diskUsed,
    maxdisk = maxdisk,
    tags = tags,
)

private fun ClusterResourceSnapshot.toEntity(serverId: Long, capturedAt: Long): CachedClusterResourceEntity =
    CachedClusterResourceEntity(
        serverId = serverId,
        type = type,
        node = node,
        // Room disallows NULL in composite primary keys — fold missing vmid into 0.
        vmid = vmid ?: 0,
        name = name,
        status = status,
        cpu = cpu,
        mem = mem,
        maxmem = maxmem,
        diskUsed = diskUsed,
        maxdisk = maxdisk,
        tags = tags,
        capturedAt = capturedAt,
    )
