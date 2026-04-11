package app.proxmoxopen.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Route.ServerList) {
        composable<Route.ServerList> { Placeholder("Server list") }
        composable<Route.AddServer> { Placeholder("Add server") }
        composable<Route.Login> { entry ->
            val args = entry.toRoute<Route.Login>()
            Placeholder("Login: server ${args.serverId}")
        }
        composable<Route.Dashboard> { entry ->
            val args = entry.toRoute<Route.Dashboard>()
            Placeholder("Dashboard: server ${args.serverId}")
        }
        composable<Route.NodeDetail> { entry ->
            val args = entry.toRoute<Route.NodeDetail>()
            Placeholder("Node ${args.node} on server ${args.serverId}")
        }
        composable<Route.GuestDetail> { entry ->
            val args = entry.toRoute<Route.GuestDetail>()
            Placeholder("Guest ${args.type}/${args.vmid} on ${args.node}")
        }
        composable<Route.Settings> { Placeholder("Settings") }
        composable<Route.TaskLog> { entry ->
            val args = entry.toRoute<Route.TaskLog>()
            Placeholder("Tasks: server ${args.serverId}")
        }
    }
}

@Composable
private fun Placeholder(label: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(label)
    }
}
