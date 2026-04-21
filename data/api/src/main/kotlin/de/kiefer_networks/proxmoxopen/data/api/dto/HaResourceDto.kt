package de.kiefer_networks.proxmoxopen.data.api.dto

import kotlinx.serialization.Serializable

/** Entry from `/cluster/ha/resources` describing an HA-managed VM/LXC. */
@Serializable
data class HaResourceDto(
    val sid: String,
    val type: String? = null,
    val state: String? = null,
    val group: String? = null,
    val comment: String? = null,
    val max_relocate: Int? = null,
    val max_restart: Int? = null,
    val digest: String? = null,
)
