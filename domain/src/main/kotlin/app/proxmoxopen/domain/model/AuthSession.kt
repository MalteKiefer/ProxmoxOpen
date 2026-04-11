package app.proxmoxopen.domain.model

/** In-memory only — never persisted. */
data class AuthSession(
    val ticket: String,
    val csrfToken: String,
    val expiresAt: Long,
)
