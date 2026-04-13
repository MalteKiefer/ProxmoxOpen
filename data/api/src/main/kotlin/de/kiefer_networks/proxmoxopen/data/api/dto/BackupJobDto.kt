package de.kiefer_networks.proxmoxopen.data.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class BackupJobDto(
    val id: String,
    val enabled: Int? = null,
    val schedule: String? = null,
    val storage: String? = null,
    val vmid: String? = null,  // comma-separated VMIDs or "all"
    val mode: String? = null,  // snapshot, suspend, stop
    val compress: String? = null,
    val mailnotification: String? = null,
    val mailto: String? = null,
    val node: String? = null,
    val pool: String? = null,
    val all: Int? = null,
    val notes: String? = null,
    @kotlinx.serialization.SerialName("next-run")
    val nextRun: Long? = null,
    val type: String? = null,
)
