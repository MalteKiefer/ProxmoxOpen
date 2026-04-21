package de.kiefer_networks.proxmoxopen.data.api.dto

import kotlinx.serialization.Serializable

/** Entry from `/cluster/resources`. */
@Serializable
data class ClusterResourceDto(
    val id: String,
    val type: String,
    val node: String? = null,
    val vmid: Int? = null,
    val name: String? = null,
    val status: String? = null,
    val cpu: Double? = null,
    val maxcpu: Int? = null,
    val mem: Long? = null,
    val maxmem: Long? = null,
    val disk: Long? = null,
    val maxdisk: Long? = null,
    val uptime: Long? = null,
    val tags: String? = null,
    val template: Int? = null,
    val storage: String? = null,
    val content: String? = null,
)

/** Per-guest `/status/current`. */
@Serializable
data class GuestStatusDto(
    val vmid: Int? = null,
    val name: String? = null,
    val status: String? = null,
    val cpu: Double? = null,
    val cpus: Int? = null,
    val mem: Long? = null,
    val maxmem: Long? = null,
    val disk: Long? = null,
    val maxdisk: Long? = null,
    val uptime: Long? = null,
    val tags: String? = null,
    val qmpstatus: String? = null,
)
