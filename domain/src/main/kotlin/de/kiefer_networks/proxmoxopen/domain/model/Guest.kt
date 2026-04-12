package de.kiefer_networks.proxmoxopen.domain.model

enum class GuestType(val apiPath: String) {
    QEMU("qemu"),
    LXC("lxc"),
    ;

    companion object {
        fun fromApiPath(path: String): GuestType? = entries.firstOrNull { it.apiPath == path }
    }
}

enum class GuestStatus {
    RUNNING,
    STOPPED,
    PAUSED,
    SUSPENDED,
    UNKNOWN,
    ;

    companion object {
        fun fromProxmox(value: String?): GuestStatus = when (value?.lowercase()) {
            "running" -> RUNNING
            "stopped" -> STOPPED
            "paused" -> PAUSED
            "suspended" -> SUSPENDED
            else -> UNKNOWN
        }
    }
}

data class Guest(
    val vmid: Int,
    val name: String,
    val node: String,
    val type: GuestType,
    val status: GuestStatus,
    val cpuUsage: Double,
    val cpuCount: Int,
    val memUsed: Long,
    val memTotal: Long,
    val diskUsed: Long,
    val diskTotal: Long,
    val uptimeSeconds: Long,
    val tags: List<String>,
)
