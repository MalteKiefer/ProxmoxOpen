package app.proxmoxopen.ui.nav

import kotlinx.serialization.Serializable

sealed interface Route {
    @Serializable data object Main : Route
    @Serializable data object AddServer : Route
    @Serializable data class EditServer(val serverId: Long) : Route
    @Serializable data class Login(val serverId: Long) : Route
    @Serializable data class Dashboard(val serverId: Long) : Route
    @Serializable data class NodeDetail(val serverId: Long, val node: String) : Route
    @Serializable data class GuestDetail(
        val serverId: Long, val node: String, val vmid: Int, val type: String,
    ) : Route
    @Serializable data class TaskDetail(
        val serverId: Long, val node: String, val upid: String,
    ) : Route
    @Serializable data object Settings : Route
    @Serializable data object Activity : Route
    @Serializable data class GuestConfig(
        val serverId: Long, val node: String, val vmid: Int, val type: String,
    ) : Route
    @Serializable data class Console(
        val serverId: Long, val node: String, val vmid: Int = 0, val type: String,
    ) : Route
}
