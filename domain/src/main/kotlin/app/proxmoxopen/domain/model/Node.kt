package app.proxmoxopen.domain.model

enum class NodeStatus { ONLINE, OFFLINE, UNKNOWN }

data class Node(
    val name: String,
    val status: NodeStatus,
    val cpuUsage: Double,
    val cpuCount: Int,
    val memUsed: Long,
    val memTotal: Long,
    val diskUsed: Long,
    val diskTotal: Long,
    val uptimeSeconds: Long,
    val loadAverage: List<Double>,
)

data class Cluster(
    val name: String,
    val quorate: Boolean,
    val version: Long,
    val nodes: List<Node>,
)
