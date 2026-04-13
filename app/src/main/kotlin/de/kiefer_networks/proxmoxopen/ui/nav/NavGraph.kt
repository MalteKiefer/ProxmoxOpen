package de.kiefer_networks.proxmoxopen.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import de.kiefer_networks.proxmoxopen.ui.addserver.AddServerScreen
import de.kiefer_networks.proxmoxopen.ui.dashboard.DashboardScreen
import de.kiefer_networks.proxmoxopen.ui.guestconfig.GuestConfigScreen
import de.kiefer_networks.proxmoxopen.ui.guestdetail.GuestDetailScreen
import de.kiefer_networks.proxmoxopen.ui.guestdetail.VmDetailScreen
import de.kiefer_networks.proxmoxopen.ui.login.LoginScreen
import de.kiefer_networks.proxmoxopen.ui.main.MainScreen
import de.kiefer_networks.proxmoxopen.ui.nodedetail.NodeDetailScreen
import de.kiefer_networks.proxmoxopen.ui.serverlist.EditServerScreen
import de.kiefer_networks.proxmoxopen.ui.settings.SettingsScreen
import de.kiefer_networks.proxmoxopen.ui.about.AboutScreen
import de.kiefer_networks.proxmoxopen.ui.activity.ActivityScreen
import de.kiefer_networks.proxmoxopen.ui.console.ConsoleScreen
import de.kiefer_networks.proxmoxopen.ui.clone.CloneScreen
import de.kiefer_networks.proxmoxopen.ui.migrate.MigrateScreen
import de.kiefer_networks.proxmoxopen.ui.storage.StorageScreen
import de.kiefer_networks.proxmoxopen.ui.taskdetail.TaskDetailScreen

@Composable
fun NavGraph() {
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = Route.Main) {
        composable<Route.Main> {
            MainScreen(
                onAddServer = { nav.navigate(Route.AddServer) },
                onOpenServer = { server -> nav.navigate(Route.Login(server.id)) },
                onEditServer = { id -> nav.navigate(Route.EditServer(id)) },
                onAbout = { nav.navigate(Route.About) },
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
                onStorage = { nodeName ->
                    nav.navigate(Route.StorageOverview(route.serverId, nodeName))
                },
            )
        }
        composable<Route.NodeDetail> { entry ->
            val route = entry.toRoute<Route.NodeDetail>()
            NodeDetailScreen(
                onBack = { nav.popBackStack() },
                onSettings = { nav.navigate(Route.Settings) },
                onActivity = { nav.navigate(Route.Activity) },
                onConsole = { nav.navigate(Route.Console(route.serverId, route.node, type = "node")) },
                onStorage = {
                    nav.navigate(Route.StorageOverview(route.serverId, route.node))
                },
                onOpenTask = { node, upid -> nav.navigate(Route.TaskDetail(route.serverId, node, upid)) },
            )
        }
        composable<Route.GuestDetail> { entry ->
            val route = entry.toRoute<Route.GuestDetail>()
            val onOpenTask = { node: String, upid: String ->
                nav.navigate(Route.TaskDetail(route.serverId, node, upid))
            }
            val onMigrate = {
                nav.navigate(
                    Route.MigrateGuest(route.serverId, route.node, route.vmid, route.type),
                )
            }
            val onClone = {
                nav.navigate(
                    Route.CloneGuest(route.serverId, route.node, route.vmid, route.type),
                )
            }
            if (route.type == "qemu") {
                VmDetailScreen(
                    onBack = { nav.popBackStack() },
                    onConsole = { nav.navigate(Route.Console(route.serverId, route.node, route.vmid, route.type)) },
                    onOpenTask = onOpenTask,
                    onMigrate = onMigrate,
                    onClone = onClone,
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
                    onConsole = { nav.navigate(Route.Console(route.serverId, route.node, route.vmid, route.type)) },
                    onOpenTask = onOpenTask,
                    onMigrate = onMigrate,
                    onClone = onClone,
                )
            }
        }
        composable<Route.CloneGuest> {
            CloneScreen(onBack = { nav.popBackStack() })
        }
        composable<Route.GuestConfig> {
            GuestConfigScreen(onBack = { nav.popBackStack() })
        }
        composable<Route.Console> { ConsoleScreen(onBack = { nav.popBackStack() }) }
        composable<Route.TaskDetail> {
            TaskDetailScreen(onBack = { nav.popBackStack() })
        }
        composable<Route.MigrateGuest> {
            MigrateScreen(onBack = { nav.popBackStack() })
        }
        composable<Route.Settings> {
            SettingsScreen(
                onBack = { nav.popBackStack() },
                onAbout = { nav.navigate(Route.About) },
                showBackButton = true,
            )
        }
        composable<Route.About> {
            AboutScreen(onBack = { nav.popBackStack() })
        }
        composable<Route.Activity> {
            ActivityScreen(
                onBack = { nav.popBackStack() },
                showBackButton = true,
            )
        }
        composable<Route.StorageOverview> {
            StorageScreen(onBack = { nav.popBackStack() })
        }
    }
}
