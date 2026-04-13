package de.kiefer_networks.proxmoxopen.core.ui.theme

import androidx.compose.ui.graphics.Color

// Proxmox brand: orange on near-black surfaces.
val ProxmoxOrange = Color(0xFFE57000)
val ProxmoxOrangeLight = Color(0xFFFFB66B)
val ProxmoxOrangeDeep = Color(0xFFB85700)
val ProxmoxBlack = Color(0xFF0B0B0C)
val ProxmoxNearBlack = Color(0xFF131316)
val ProxmoxSurface = Color(0xFF1A1A1E)
val ProxmoxSurfaceHigh = Color(0xFF24242A)
val ProxmoxSurfaceHigher = Color(0xFF2E2E36)
val ProxmoxOnSurface = Color(0xFFEFEFF1)
val ProxmoxOnSurfaceVariant = Color(0xFFB6B6BC)
val ProxmoxOutline = Color(0xFF3A3A42)

// Light scheme is still available for users who explicitly choose it.
val LightPrimary = ProxmoxOrangeDeep
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFFFDDC4)
val LightOnPrimaryContainer = Color(0xFF2A1100)
val LightSecondary = Color(0xFF735847)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightBackground = Color(0xFFFFFBF8)
val LightOnBackground = Color(0xFF1F1B17)
val LightSurface = Color(0xFFFFF8F4)
val LightOnSurface = Color(0xFF1F1B17)
val LightError = Color(0xFFBA1A1A)
val LightOnError = Color(0xFFFFFFFF)

val DarkPrimary = ProxmoxOrange
val DarkOnPrimary = Color(0xFF000000)
val DarkPrimaryContainer = ProxmoxOrangeDeep
val DarkOnPrimaryContainer = Color(0xFFFFE5D1)
val DarkSecondary = ProxmoxOrangeLight
val DarkOnSecondary = Color(0xFF1A0E00)
val DarkBackground = ProxmoxBlack
val DarkOnBackground = ProxmoxOnSurface
val DarkSurface = ProxmoxNearBlack
val DarkOnSurface = ProxmoxOnSurface
val DarkError = Color(0xFFFFB4AB)
val DarkOnError = Color(0xFF690005)

// Status colors shared across themes.
val StatusRunning = Color(0xFF6BCB77)
val StatusStopped = Color(0xFF8C8C95)
val StatusPaused = ProxmoxOrange
val StatusError = Color(0xFFFF6B6B)

// Resource gauge colors.
val ResourceCpu = Color(0xFF6BCB77)    // Same as StatusRunning - green
val ResourceRam = Color(0xFF5B9BD5)    // Blue
val ResourceDisk = Color(0xFFE8A838)   // Orange (standardize to one shade)
