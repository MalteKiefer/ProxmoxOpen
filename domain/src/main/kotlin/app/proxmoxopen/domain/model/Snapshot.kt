package app.proxmoxopen.domain.model

data class Snapshot(
    val name: String,
    val description: String?,
    val snaptime: Long?,
    val parent: String?,
    val vmstate: Boolean,
)
