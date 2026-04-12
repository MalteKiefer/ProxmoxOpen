package app.proxmoxopen.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import app.proxmoxopen.R
import app.proxmoxopen.domain.model.Server
import app.proxmoxopen.ui.activity.ActivityScreen
import app.proxmoxopen.ui.serverlist.ServerListScreen
import app.proxmoxopen.ui.settings.SettingsScreen
import kotlin.reflect.KClass

private data class BottomTab(
    val routeObject: TabRoute,
    val routeClass: KClass<out TabRoute>,
    val icon: ImageVector,
    val labelRes: Int,
)

@Composable
fun MainScreen(
    onAddServer: () -> Unit,
    onOpenServer: (Server) -> Unit,
) {
    val tabNav = rememberNavController()
    val backStackEntry by tabNav.currentBackStackEntryAsState()

    val tabs = listOf(
        BottomTab(
            routeObject = TabRoute.Servers,
            routeClass = TabRoute.Servers::class,
            icon = Icons.Outlined.Dns,
            labelRes = R.string.tab_servers,
        ),
        BottomTab(
            routeObject = TabRoute.Activity,
            routeClass = TabRoute.Activity::class,
            icon = Icons.Outlined.History,
            labelRes = R.string.tab_activity,
        ),
        BottomTab(
            routeObject = TabRoute.Settings,
            routeClass = TabRoute.Settings::class,
            icon = Icons.Outlined.Settings,
            labelRes = R.string.tab_settings,
        ),
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
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
                )
            }
            composable<TabRoute.Activity> { ActivityScreen() }
            composable<TabRoute.Settings> { SettingsScreen() }
        }
    }
}
