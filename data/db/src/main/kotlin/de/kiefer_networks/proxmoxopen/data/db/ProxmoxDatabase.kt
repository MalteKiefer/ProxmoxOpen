package de.kiefer_networks.proxmoxopen.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import de.kiefer_networks.proxmoxopen.data.db.dao.ClusterCacheDao
import de.kiefer_networks.proxmoxopen.data.db.dao.ServerDao
import de.kiefer_networks.proxmoxopen.data.db.entity.CachedClusterResourceEntity
import de.kiefer_networks.proxmoxopen.data.db.entity.ServerEntity

@Database(
    entities = [ServerEntity::class, CachedClusterResourceEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class ProxmoxDatabase : RoomDatabase() {
    abstract fun serverDao(): ServerDao
    abstract fun clusterCacheDao(): ClusterCacheDao

    companion object {
        const val DATABASE_NAME = "proxmoxopen.db"
    }
}
