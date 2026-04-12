package app.proxmoxopen.ui.nav

import kotlinx.serialization.Serializable

/** Top-level routes (outer NavHost). Tab-level routes live in [app.proxmoxopen.ui.main.TabRoute]. */
sealed interface Route {
    @Serializable data object Main : Route
    @Serializable data object AddServer : Route
    @Serializable data class Login(val serverId: Long) : Route
    @Serializable data class Dashboard(val serverId: Long) : Route
    @Serializable data class NodeDetail(val serverId: Long, val node: String) : Route
    @Serializable data class GuestDetail(
        val serverId: Long,
        val node: String,
        val vmid: Int,
        val type: String,
    ) : Route
}
