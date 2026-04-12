package de.kiefer_networks.proxmoxopen.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import de.kiefer_networks.proxmoxopen.data.db.entity.ServerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ServerDao {
    @Query("SELECT * FROM servers ORDER BY name")
    fun observeAll(): Flow<List<ServerEntity>>

    @Query("SELECT * FROM servers WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ServerEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(server: ServerEntity): Long

    @Update
    suspend fun update(server: ServerEntity)

    @Delete
    suspend fun delete(server: ServerEntity)

    @Query("UPDATE servers SET lastConnectedAt = :timestamp WHERE id = :id")
    suspend fun touchLastConnected(id: Long, timestamp: Long)
}
