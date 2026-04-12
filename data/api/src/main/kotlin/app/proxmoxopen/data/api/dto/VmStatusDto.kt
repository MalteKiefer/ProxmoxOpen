package app.proxmoxopen.data.api.dto

import kotlinx.serialization.Serializable

/** Extended QEMU VM status from `/qemu/{vmid}/status/current`. */
@Serializable
data class VmCurrentStatusDto(
    val vmid: Int? = null,
    val name: String? = null,
    val status: String? = null,
    val qmpstatus: String? = null,
    val uptime: Long? = null,
    val ha: HaStatusDto? = null,
    val pid: Int? = null,
    val cpu: Double? = null,
    val cpus: Int? = null,
    val mem: Long? = null,
    val maxmem: Long? = null,
    val disk: Long? = null,
    val maxdisk: Long? = null,
    val netin: Long? = null,
    val netout: Long? = null,
    val diskread: Long? = null,
    val diskwrite: Long? = null,
    val tags: String? = null,
    val agent: Int? = null,
    val lock: String? = null,
    @Suppress("PropertyName") val running_machine: String? = null,
    @Suppress("PropertyName") val running_qemu: String? = null,
)

/** QEMU guest agent network interface response. */
@Serializable
data class AgentInterfaceDto(
    val name: String? = null,
    @Suppress("PropertyName") val hardware_address: String? = null,
    val ip_addresses: List<AgentIpDto>? = null,
)

@Serializable
data class AgentIpDto(
    val ip_address: String? = null,
    val ip_address_type: String? = null,  // "ipv4" or "ipv6"
    val prefix: Int? = null,
)

/** Wrapper for agent network response which nests under "result". */
@Serializable
data class AgentNetworkResult(
    val result: List<AgentInterfaceDto>? = null,
)
