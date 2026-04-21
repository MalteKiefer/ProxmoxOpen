package de.kiefer_networks.proxmoxopen.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AptUpdateDto(
    @SerialName("Package") val packageName: String? = null,
    @SerialName("Version") val version: String? = null,
    @SerialName("OldVersion") val oldVersion: String? = null,
    @SerialName("Origin") val origin: String? = null,
    @SerialName("Priority") val priority: String? = null,
    @SerialName("Title") val title: String? = null,
    @SerialName("Description") val description: String? = null,
    @SerialName("Section") val section: String? = null,
)
