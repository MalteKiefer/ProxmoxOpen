package app.proxmoxopen.data.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class StorageInfoDto(
    val storage: String,
    val type: String? = null,
    val content: String? = null,
    val enabled: Int? = null,
    val active: Int? = null,
    val total: Long? = null,
    val used: Long? = null,
    val avail: Long? = null,
)
