package de.kiefer_networks.proxmoxopen.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import de.kiefer_networks.proxmoxopen.data.db.dao.ServerDao
import de.kiefer_networks.proxmoxopen.data.db.entity.ServerEntity

@Database(entities = [ServerEntity::class], version = 1, exportSchema = true)
abstract class ProxmoxDatabase : RoomDatabase() {
    abstract fun serverDao(): ServerDao

    companion object {
        const val DATABASE_NAME = "proxmoxopen.db"
    }
}
