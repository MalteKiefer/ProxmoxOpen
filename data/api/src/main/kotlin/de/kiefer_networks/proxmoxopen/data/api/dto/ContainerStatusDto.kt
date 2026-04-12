package de.kiefer_networks.proxmoxopen.data.api.dto

import kotlinx.serialization.Serializable

/** Extended LXC status from `/status/current`. */
@Serializable
data class ContainerCurrentStatusDto(
    val vmid: Int? = null,
    val name: String? = null,
    val status: String? = null,
    val uptime: Long? = null,
    val ha: HaStatusDto? = null,
    val pid: Int? = null,
    val cpu: Double? = null,
    val cpus: Int? = null,
    val mem: Long? = null,
    val maxmem: Long? = null,
    val swap: Long? = null,
    val maxswap: Long? = null,
    val disk: Long? = null,
    val maxdisk: Long? = null,
    val netin: Long? = null,
    val netout: Long? = null,
    val diskread: Long? = null,
    val diskwrite: Long? = null,
    val type: String? = null,
    val tags: String? = null,
)

@Serializable
data class HaStatusDto(
    val managed: Int? = null,
    val state: String? = null,
)

/** Entry from `/lxc/{vmid}/interfaces` — one per network interface. */
@Serializable
data class InterfaceDto(
    val name: String? = null,
    val hwaddr: String? = null,
    val inet: String? = null,
    val inet6: String? = null,
)
