package de.kiefer_networks.proxmoxopen.ui.configbackup

import kotlinx.serialization.Serializable

/**
 * Top-level payload that gets JSON-serialized and then AES-GCM wrapped.
 *
 * Secrets (passwords, API token secrets) are deliberately NOT included — they stay in
 * the Keystore-backed secret store. Users will have to re-enter their credentials after
 * importing on a new device.
 */
@Serializable
data class ExportedConfig(
    val version: Int = 1,
    val exportedAt: Long,
    val servers: List<ExportedServer>,
    val preferences: ExportedPreferences,
)

@Serializable
data class ExportedServer(
    val id: Long,
    val name: String,
    val host: String,
    val port: Int,
    val realm: String,
    val username: String? = null,
    val tokenId: String? = null,
    val fingerprintSha256: String,
    val createdAt: Long,
    val lastConnectedAt: Long? = null,
)

@Serializable
data class ExportedPreferences(
    val themeMode: String? = null,
    val useDynamicColor: Boolean? = null,
    val amoledBlack: Boolean? = null,
    val language: String? = null,
    val appLockEnabled: Boolean? = null,
    val blockScreenshots: Boolean? = null,
    val refreshInterval: String? = null,
    val terminalFontSize: String? = null,
    val terminalTheme: String? = null,
)
