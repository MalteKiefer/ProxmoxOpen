package de.kiefer_networks.proxmoxopen.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "servers")
data class ServerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val host: String,
    val port: Int,
    val realm: String,
    val username: String?,
    val tokenId: String?,
    val fingerprintSha256: String,
    val createdAt: Long,
    val lastConnectedAt: Long?,
)
