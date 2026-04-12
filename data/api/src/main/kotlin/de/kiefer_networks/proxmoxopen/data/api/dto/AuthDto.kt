package de.kiefer_networks.proxmoxopen.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TicketDto(
    val ticket: String,
    @SerialName("CSRFPreventionToken") val csrfToken: String,
    val username: String? = null,
    @SerialName("NeedTFA") val needTfa: Int? = null,
)
