package de.kiefer_networks.proxmoxopen.core.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    error = LightError,
    onError = LightOnError,
)

private val DarkColors = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = ProxmoxSurfaceHigh,
    onSurfaceVariant = ProxmoxOnSurfaceVariant,
    surfaceContainer = ProxmoxSurface,
    surfaceContainerLow = ProxmoxNearBlack,
    surfaceContainerLowest = ProxmoxBlack,
    surfaceContainerHigh = ProxmoxSurfaceHigh,
    surfaceContainerHighest = ProxmoxSurfaceHigher,
    outline = ProxmoxOutline,
    error = DarkError,
    onError = DarkOnError,
)

/**
 * Material 3 theme for ProxMoxOpen, locked to the Proxmox brand palette
 * (orange on near-black) by default. Pass `useDarkTheme = false` to opt
 * into the light scheme; dynamic color is supported but not used by
 * default because the brand palette is what we want to ship.
 */
@Composable
fun ProxMoxOpenTheme(
    useDarkTheme: Boolean? = null,
    dynamicColor: Boolean = false,
    amoledBlack: Boolean = false,
    content: @Composable () -> Unit,
) {
    val resolvedDark = useDarkTheme ?: isSystemInDarkTheme()
    val ctx = LocalContext.current
    val baseScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (resolvedDark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        resolvedDark -> DarkColors
        else -> LightColors
    }
    val scheme = if (resolvedDark && amoledBlack) {
        baseScheme.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceContainerLowest = Color.Black,
            surfaceContainerLow = Color.Black,
            surfaceContainer = Color(0xFF0A0A0A),
        )
    } else {
        baseScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = scheme.background.toArgb()
            window.navigationBarColor = scheme.background.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !resolvedDark
            controller.isAppearanceLightNavigationBars = !resolvedDark
        }
    }

    MaterialTheme(colorScheme = scheme, typography = AppTypography, content = content)
}
