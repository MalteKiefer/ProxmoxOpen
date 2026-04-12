package de.kiefer_networks.proxmoxopen.domain.model

enum class NodeStatus { ONLINE, OFFLINE, UNKNOWN }

data class Node(
    val name: String,
    val status: NodeStatus,
    val cpuUsage: Double,
    val cpuCount: Int,
    val cpuModel: String?,
    val memUsed: Long,
    val memTotal: Long,
    val diskUsed: Long,
    val diskTotal: Long,
    val swapUsed: Long,
    val swapTotal: Long,
    val uptimeSeconds: Long,
    val loadAverage: List<Double>,
    val ioDelay: Double?,
    val ksmShared: Long?,
    val kernelVersion: String?,
    val pveVersion: String?,
)

data class Cluster(
    val name: String,
    val quorate: Boolean,
    val version: Long,
    val nodes: List<Node>,
)
