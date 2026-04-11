package app.proxmoxopen.data.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class RrdPointDto(
    val time: Long,
    val cpu: Double? = null,
    val mem: Double? = null,
    val maxmem: Double? = null,
    val netin: Double? = null,
    val netout: Double? = null,
    val diskread: Double? = null,
    val diskwrite: Double? = null,
)
