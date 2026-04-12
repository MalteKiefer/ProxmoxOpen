package app.proxmoxopen.data.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class BackupVolumeDto(
    val volid: String,
    val vmid: Int? = null,
    val ctime: Long? = null,
    val size: Long? = null,
    val format: String? = null,
    val content: String? = null,
    val notes: String? = null,
    val protected: Int? = null,
    val subtype: String? = null,
)
