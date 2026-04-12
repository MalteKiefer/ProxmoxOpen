package app.proxmoxopen.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dashboard
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.navigation.toRoute
import app.proxmoxopen.R
import app.proxmoxopen.ui.activity.ActivityScreen
import app.proxmoxopen.ui.dashboard.DashboardScreen
import app.proxmoxopen.ui.guestdetail.GuestDetailScreen
import app.proxmoxopen.ui.nodedetail.NodeDetailScreen
import app.proxmoxopen.ui.serverlist.ServerListScreen
import app.proxmoxopen.ui.settings.SettingsScreen
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
    onLogin: (Long) -> Unit,
    openDashboardServerId: Long? = null,
    onDashboardOpened: () -> Unit = {},
) {
    val tabNav = rememberNavController()
    val backStackEntry by tabNav.currentBackStackEntryAsState()

    // When login completes, navigate into Dashboard tab.
    LaunchedEffect(openDashboardServerId) {
        if (openDashboardServerId != null) {
            tabNav.navigate(TabRoute.Dashboard(openDashboardServerId)) {
                launchSingleTop = true
            }
            onDashboardOpened()
        }
    }

    val inDashboard = backStackEntry?.destination?.let { dest ->
        dest.hasRoute(TabRoute.Dashboard::class) ||
            dest.hasRoute(TabRoute.NodeDetail::class) ||
            dest.hasRoute(TabRoute.GuestDetail::class)
    } ?: false

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                TABS.forEach { tab ->
                    val isServersTab = tab.routeClass == TabRoute.Servers::class
                    val selected = if (isServersTab && inDashboard) {
                        true
                    } else {
                        backStackEntry?.destination?.hierarchy?.any {
                            it.hasRoute(tab.routeClass)
                        } == true
                    }
                    val icon = if (isServersTab && inDashboard) Icons.Outlined.Dashboard else tab.icon
                    val label = if (isServersTab && inDashboard) {
                        stringResource(R.string.dashboard_title)
                    } else {
                        stringResource(tab.labelRes)
                    }

                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            tabNav.navigate(tab.routeObject) {
                                popUpTo(tabNav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(icon, contentDescription = null) },
                        label = { Text(label) },
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
                    onOpenServer = { server -> onLogin(server.id) },
                )
            }
            composable<TabRoute.Activity> { ActivityScreen() }
            composable<TabRoute.Settings> { SettingsScreen() }
            composable<TabRoute.Dashboard> { entry ->
                val route = entry.toRoute<TabRoute.Dashboard>()
                DashboardScreen(
                    onBack = { tabNav.popBackStack() },
                    onOpenNode = { nodeName ->
                        tabNav.navigate(TabRoute.NodeDetail(route.serverId, nodeName))
                    },
                    onOpenGuest = { guest ->
                        tabNav.navigate(
                            TabRoute.GuestDetail(
                                serverId = route.serverId,
                                node = guest.node,
                                vmid = guest.vmid,
                                type = guest.type.apiPath,
                            ),
                        )
                    },
                )
            }
            composable<TabRoute.NodeDetail> {
                NodeDetailScreen(onBack = { tabNav.popBackStack() })
            }
            composable<TabRoute.GuestDetail> {
                GuestDetailScreen(onBack = { tabNav.popBackStack() })
            }
        }
    }
}
