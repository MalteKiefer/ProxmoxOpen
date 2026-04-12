package app.proxmoxopen.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import app.proxmoxopen.ui.addserver.AddServerScreen
import app.proxmoxopen.ui.dashboard.DashboardScreen
import app.proxmoxopen.ui.guestconfig.GuestConfigScreen
import app.proxmoxopen.ui.guestdetail.GuestDetailScreen
import app.proxmoxopen.ui.guestdetail.VmDetailScreen
import app.proxmoxopen.ui.login.LoginScreen
import app.proxmoxopen.ui.main.MainScreen
import app.proxmoxopen.ui.nodedetail.NodeDetailScreen
import app.proxmoxopen.ui.serverlist.EditServerScreen
import app.proxmoxopen.ui.settings.SettingsScreen
import app.proxmoxopen.ui.activity.ActivityScreen
// Console removed — will be reimplemented with native xterm.js WebSocket
import app.proxmoxopen.ui.taskdetail.TaskDetailScreen

@Composable
fun NavGraph() {
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = Route.Main) {
        composable<Route.Main> {
            MainScreen(
                onAddServer = { nav.navigate(Route.AddServer) },
                onOpenServer = { server -> nav.navigate(Route.Login(server.id)) },
                onEditServer = { id -> nav.navigate(Route.EditServer(id)) },
            )
        }
        composable<Route.AddServer> {
            AddServerScreen(
                onBack = { nav.popBackStack() },
                onSaved = { serverId ->
                    nav.popBackStack()
                    nav.navigate(Route.Login(serverId))
                },
            )
        }
        composable<Route.EditServer> {
            EditServerScreen(
                onBack = { nav.popBackStack() },
                onSaved = { nav.popBackStack() },
            )
        }
        composable<Route.Login> {
            LoginScreen(
                onBack = { nav.popBackStack() },
                onSignedIn = { serverId ->
                    nav.popBackStack()
                    nav.navigate(Route.Dashboard(serverId))
                },
            )
        }
        composable<Route.Dashboard> { entry ->
            val route = entry.toRoute<Route.Dashboard>()
            DashboardScreen(
                onBack = { nav.popBackStack() },
                onOpenNode = { nodeName ->
                    nav.navigate(Route.NodeDetail(route.serverId, nodeName))
                },
                onOpenGuest = { guest ->
                    nav.navigate(
                        Route.GuestDetail(
                            serverId = route.serverId,
                            node = guest.node,
                            vmid = guest.vmid,
                            type = guest.type.apiPath,
                        ),
                    )
                },
                onSettings = { nav.navigate(Route.Settings) },
                onActivity = { nav.navigate(Route.Activity) },
            )
        }
        composable<Route.NodeDetail> { entry ->
            val route = entry.toRoute<Route.NodeDetail>()
            NodeDetailScreen(
                onBack = { nav.popBackStack() },
                onSettings = { nav.navigate(Route.Settings) },
                onActivity = { nav.navigate(Route.Activity) },
                onConsole = { /* coming soon */ },
            )
        }
        composable<Route.GuestDetail> { entry ->
            val route = entry.toRoute<Route.GuestDetail>()
            val onOpenTask = { node: String, upid: String ->
                nav.navigate(Route.TaskDetail(route.serverId, node, upid))
            }
            if (route.type == "qemu") {
                VmDetailScreen(
                    onBack = { nav.popBackStack() },
                    onSettings = { nav.navigate(Route.Settings) },
                    onConsole = { /* coming soon */ },
                    onOpenTask = onOpenTask,
                )
            } else {
                GuestDetailScreen(
                    onBack = { nav.popBackStack() },
                    onSettings = { nav.navigate(Route.Settings) },
                    onActivity = { nav.navigate(Route.Activity) },
                    onEditConfig = {
                        nav.navigate(
                            Route.GuestConfig(route.serverId, route.node, route.vmid, route.type),
                        )
                    },
                    onConsole = { /* coming soon */ },
                    onOpenTask = onOpenTask,
                )
            }
        }
        composable<Route.GuestConfig> {
            GuestConfigScreen(onBack = { nav.popBackStack() })
        }
        // Console: coming soon — will use native xterm.js WebSocket
        composable<Route.TaskDetail> {
            TaskDetailScreen(onBack = { nav.popBackStack() })
        }
        composable<Route.Settings> {
            SettingsScreen(
                onBack = { nav.popBackStack() },
                showBackButton = true,
            )
        }
        composable<Route.Activity> {
            ActivityScreen(
                onBack = { nav.popBackStack() },
                showBackButton = true,
            )
        }
    }
}
