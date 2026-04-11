package app.proxmoxopen.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import app.proxmoxopen.ui.addserver.AddServerScreen
import app.proxmoxopen.ui.dashboard.DashboardScreen
import app.proxmoxopen.ui.guestdetail.GuestDetailScreen
import app.proxmoxopen.ui.nodedetail.NodeDetailScreen
import app.proxmoxopen.ui.serverlist.ServerListScreen
import app.proxmoxopen.ui.settings.SettingsScreen
import app.proxmoxopen.ui.tasklog.TaskLogScreen

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Route.ServerList) {
        composable<Route.ServerList> {
            ServerListScreen(
                onAddServer = { navController.navigate(Route.AddServer) },
                onOpenServer = { server -> navController.navigate(Route.Dashboard(server.id)) },
            )
        }
        composable<Route.AddServer> {
            AddServerScreen(
                onBack = { navController.popBackStack() },
                onSaved = { serverId ->
                    navController.popBackStack()
                    navController.navigate(Route.Dashboard(serverId))
                },
            )
        }
        composable<Route.Dashboard> { backStackEntry ->
            val route = backStackEntry.toRoute<Route.Dashboard>()
            DashboardScreen(
                onBack = { navController.popBackStack() },
                onOpenNode = { nodeName ->
                    navController.navigate(Route.NodeDetail(route.serverId, nodeName))
                },
                onOpenGuest = { guest ->
                    navController.navigate(
                        Route.GuestDetail(
                            serverId = route.serverId,
                            node = guest.node,
                            vmid = guest.vmid,
                            type = guest.type.apiPath,
                        ),
                    )
                },
            )
        }
        composable<Route.NodeDetail> {
            NodeDetailScreen(onBack = { navController.popBackStack() })
        }
        composable<Route.GuestDetail> {
            GuestDetailScreen(onBack = { navController.popBackStack() })
        }
        composable<Route.Settings> {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable<Route.TaskLog> {
            TaskLogScreen(onBack = { navController.popBackStack() })
        }
        composable<Route.Login> { Unit }
    }
}
