package de.kiefer_networks.proxmoxopen.ui.main

import kotlinx.serialization.Serializable

sealed interface TabRoute {
    @Serializable data object Servers : TabRoute
    @Serializable data object Activity : TabRoute
    @Serializable data object Settings : TabRoute
}
