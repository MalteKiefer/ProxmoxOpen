package app.proxmoxopen.ui.nav

import kotlinx.serialization.Serializable

/** Top-level routes (outer NavHost). Detail routes live in [app.proxmoxopen.ui.main.TabRoute]. */
sealed interface Route {
    @Serializable data object Main : Route
    @Serializable data object AddServer : Route
    @Serializable data class Login(val serverId: Long) : Route
}
