package app.proxmoxopen.domain.model

data class StorageInfo(
    val name: String,
    val type: String,
    val content: String,
    val enabled: Boolean,
    val active: Boolean,
    val total: Long,
    val used: Long,
    val available: Long,
)
