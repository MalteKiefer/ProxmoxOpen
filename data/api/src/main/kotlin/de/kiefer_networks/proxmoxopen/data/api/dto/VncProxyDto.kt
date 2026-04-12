package de.kiefer_networks.proxmoxopen.data.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class VncProxyDto(
    val port: Int,
    val ticket: String,
    val upid: String? = null,
    val cert: String? = null,
)
