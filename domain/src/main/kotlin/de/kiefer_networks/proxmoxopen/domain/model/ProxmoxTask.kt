package de.kiefer_networks.proxmoxopen.domain.model

enum class TaskState { RUNNING, OK, FAILED, UNKNOWN }

data class ProxmoxTask(
    val upid: String,
    val node: String,
    val type: String,
    val id: String?,
    val user: String,
    val state: TaskState,
    val startTime: Long,
    val endTime: Long?,
    val exitStatus: String?,
)
