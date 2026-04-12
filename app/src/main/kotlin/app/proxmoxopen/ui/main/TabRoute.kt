package app.proxmoxopen.ui.main

import kotlinx.serialization.Serializable

sealed interface TabRoute {
    @Serializable data object Servers : TabRoute
    @Serializable data object Activity : TabRoute
    @Serializable data object Settings : TabRoute
    @Serializable data class Dashboard(val serverId: Long) : TabRoute
    @Serializable data class NodeDetail(val serverId: Long, val node: String) : TabRoute
    @Serializable data class GuestDetail(
        val serverId: Long,
        val node: String,
        val vmid: Int,
        val type: String,
    ) : TabRoute
}
