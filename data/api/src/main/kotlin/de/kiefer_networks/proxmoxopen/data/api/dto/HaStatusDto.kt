package de.kiefer_networks.proxmoxopen.data.api.dto

import kotlinx.serialization.Serializable

/**
 * Entry from `/cluster/ha/status/current`. The endpoint mixes several logical
 * entry types, distinguished by [type]:
 *  - `quorum`  — one line per cluster reporting quorate state.
 *  - `master`  — name of the current HA master.
 *  - `node`    — an HA-enabled node and its current status.
 *  - `service` — an HA-managed service (VM/CT) with its running state.
 */
@Serializable
data class HaStatusDto(
    val id: String? = null,
    val type: String? = null,
    val state: String? = null,
    val quorate: Int? = null,
    val master: String? = null,
    val node: String? = null,
    val status: String? = null,
    val timestamp: Long? = null,
    val service: String? = null,
    val sid: String? = null,
    val group: String? = null,
    val comment: String? = null,
)
