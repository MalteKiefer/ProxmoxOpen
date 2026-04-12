package app.proxmoxopen.data.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class TaskLogLineDto(
    val n: Int,
    val t: String,
)
