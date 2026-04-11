package app.proxmoxopen.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import app.proxmoxopen.data.db.dao.ServerDao
import app.proxmoxopen.data.db.entity.ServerEntity

@Database(entities = [ServerEntity::class], version = 1, exportSchema = true)
abstract class ProxmoxDatabase : RoomDatabase() {
    abstract fun serverDao(): ServerDao

    companion object {
        const val DATABASE_NAME = "proxmoxopen.db"
    }
}
