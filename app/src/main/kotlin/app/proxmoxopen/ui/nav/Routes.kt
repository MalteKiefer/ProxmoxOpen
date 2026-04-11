package app.proxmoxopen.ui.nav

import kotlinx.serialization.Serializable

sealed interface Route {
    @Serializable data object ServerList : Route
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
    @Serializable data object Settings : Route
    @Serializable data class TaskLog(val serverId: Long) : Route
}
