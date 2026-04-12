package de.kiefer_networks.proxmoxopen.domain.model

data class Backup(
    val volid: String,
    val vmid: Int,
    val createdAt: Long,
    val size: Long,
    val format: String?,
    val notes: String?,
    val protected: Boolean,
    val storage: String,
)
