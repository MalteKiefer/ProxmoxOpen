package de.kiefer_networks.proxmoxopen.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import de.kiefer_networks.proxmoxopen.R
import de.kiefer_networks.proxmoxopen.domain.model.Server
import de.kiefer_networks.proxmoxopen.ui.activity.ActivityScreen
import de.kiefer_networks.proxmoxopen.ui.serverlist.ServerListScreen
import de.kiefer_networks.proxmoxopen.ui.settings.SettingsScreen
import kotlin.reflect.KClass

private data class BottomTab(
    val routeObject: TabRoute,
    val routeClass: KClass<out TabRoute>,
    val icon: ImageVector,
    val labelRes: Int,
)

private val TABS = listOf(
    BottomTab(TabRoute.Servers, TabRoute.Servers::class, Icons.Outlined.Dns, R.string.tab_servers),
    BottomTab(TabRoute.Activity, TabRoute.Activity::class, Icons.Outlined.History, R.string.tab_activity),
    BottomTab(TabRoute.Settings, TabRoute.Settings::class, Icons.Outlined.Settings, R.string.tab_settings),
)

@Composable
fun MainScreen(
    onAddServer: () -> Unit,
    onOpenServer: (Server) -> Unit,
    onEditServer: (Long) -> Unit,
) {
    val tabNav = rememberNavController()
    val backStackEntry by tabNav.currentBackStackEntryAsState()

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                TABS.forEach { tab ->
                    val selected = backStackEntry?.destination?.hierarchy?.any {
                        it.hasRoute(tab.routeClass)
                    } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            tabNav.navigate(tab.routeObject) {
                                popUpTo(tabNav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(stringResource(tab.labelRes)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        ),
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = tabNav,
            startDestination = TabRoute.Servers,
            modifier = Modifier.padding(padding),
        ) {
            composable<TabRoute.Servers> {
                ServerListScreen(
                    onAddServer = onAddServer,
                    onOpenServer = onOpenServer,
                    onEditServer = onEditServer,
                )
            }
            composable<TabRoute.Activity> { ActivityScreen() }
            composable<TabRoute.Settings> { SettingsScreen() }
        }
    }
}
