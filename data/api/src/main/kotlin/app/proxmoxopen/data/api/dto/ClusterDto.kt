package app.proxmoxopen.data.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class ClusterStatusDto(
    val type: String,                 // "cluster" or "node"
    val name: String? = null,
    val nodes: Int? = null,
    val quorate: Int? = null,
    val version: Long? = null,
    val online: Int? = null,
    val id: String? = null,
    val ip: String? = null,
    val level: String? = null,
    val local: Int? = null,
    val nodeid: Int? = null,
)
