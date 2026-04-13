package de.kiefer_networks.proxmoxopen.domain.model

data class BackupJob(
    val id: String,
    val enabled: Boolean,
    val schedule: String?,
    val storage: String?,
    val vmid: String?,
    val mode: String?,
    val compress: String?,
    val mailto: String?,
    val mailnotification: String?,
    val node: String?,
    val pool: String?,
    val allGuests: Boolean,
    val notes: String?,
    val nextRun: Long?,
)
