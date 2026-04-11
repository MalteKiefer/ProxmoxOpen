package app.proxmoxopen.data.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class TaskDto(
    val upid: String,
    val node: String,
    val type: String,
    val user: String,
    val id: String? = null,
    val status: String? = null,
    val starttime: Long = 0,
    val endtime: Long? = null,
    val exitstatus: String? = null,
)

@Serializable
data class TaskStatusDto(
    val upid: String,
    val node: String,
    val type: String,
    val user: String,
    val status: String,             // "running" or "stopped"
    val exitstatus: String? = null,
    val starttime: Long = 0,
)
