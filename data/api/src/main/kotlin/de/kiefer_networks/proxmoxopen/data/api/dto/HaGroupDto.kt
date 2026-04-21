package de.kiefer_networks.proxmoxopen.data.api.dto

import kotlinx.serialization.Serializable

/** Entry from `/cluster/ha/groups` describing an HA failover group. */
@Serializable
data class HaGroupDto(
    val group: String,
    val nodes: String? = null,
    val comment: String? = null,
    val restricted: Int? = null,
    val nofailback: Int? = null,
    val type: String? = null,
    val digest: String? = null,
)
