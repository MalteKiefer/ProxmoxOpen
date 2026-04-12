package de.kiefer_networks.proxmoxopen.data.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class SnapshotDto(
    val name: String,
    val description: String? = null,
    val snaptime: Long? = null,
    val parent: String? = null,
    val vmstate: Int? = null,
)
