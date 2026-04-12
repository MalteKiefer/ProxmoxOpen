package de.kiefer_networks.proxmoxopen.domain.model

/**
 * Extended status for a QEMU VM. Similar to [ContainerStatus] but with
 * VM-specific fields (no swap, has qmpstatus, agent info).
 */
data class VmStatus(
    val vmid: Int,
    val name: String,
    val status: GuestStatus,
    val qmpStatus: String?,
    val uptime: Long,
    val haState: String?,
    val node: String,
    val pid: Int?,
    val agentEnabled: Boolean,
    // Resources
    val cpuUsage: Double,
    val cpuCount: Int,
    val memUsed: Long,
    val memTotal: Long,
    val diskUsed: Long,
    val diskTotal: Long,
    val netIn: Long,
    val netOut: Long,
    val diskRead: Long,
    val diskWrite: Long,
    // IPs from guest agent
    val ipAddresses: List<InterfaceIp>,
    // VM-specific
    val runningMachine: String?,
    val runningQemu: String?,
)
