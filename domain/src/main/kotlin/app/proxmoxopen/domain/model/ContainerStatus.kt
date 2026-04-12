package app.proxmoxopen.domain.model

/**
 * Extended status information for a running LXC container. Includes
 * everything from `/status/current` plus IP addresses if available.
 */
data class ContainerStatus(
    val vmid: Int,
    val name: String,
    val status: GuestStatus,
    val uptime: Long,
    val haState: String?,
    val node: String,
    val type: GuestType,
    val unprivileged: Boolean,
    val ostype: String?,
    val pid: Int?,
    // Resource usage
    val cpuUsage: Double,
    val cpuCount: Int,
    val memUsed: Long,
    val memTotal: Long,
    val swapUsed: Long,
    val swapTotal: Long,
    val diskUsed: Long,
    val diskTotal: Long,
    val netIn: Long,
    val netOut: Long,
    val diskRead: Long,
    val diskWrite: Long,
    // IPs (from /interfaces endpoint or config)
    val ipAddresses: List<InterfaceIp>,
)

data class InterfaceIp(
    val name: String,       // eth0, lo, etc.
    val hwaddr: String?,
    val inet: String?,      // IPv4 address
    val inet6: String?,     // IPv6 address
)
