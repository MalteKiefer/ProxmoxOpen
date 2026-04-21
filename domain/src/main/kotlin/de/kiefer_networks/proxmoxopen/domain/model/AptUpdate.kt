package de.kiefer_networks.proxmoxopen.domain.model

/** A single pending APT package update as reported by `/nodes/{n}/apt/update`. */
data class AptUpdate(
    val packageName: String,
    val currentVersion: String,
    val candidateVersion: String,
    val origin: String? = null,
    val priority: String? = null,
    val title: String? = null,
    val description: String? = null,
    val section: String? = null,
)
