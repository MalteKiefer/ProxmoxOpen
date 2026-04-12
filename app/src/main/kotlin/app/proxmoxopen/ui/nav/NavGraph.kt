package app.proxmoxopen.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.proxmoxopen.ui.addserver.AddServerScreen
import app.proxmoxopen.ui.login.LoginScreen
import app.proxmoxopen.ui.main.MainScreen

@Composable
fun NavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Route.Main) {
        composable<Route.Main> { backStackEntry ->
            // After login completes it writes the serverId into this entry's savedStateHandle.
            val openDashboard = backStackEntry
                .savedStateHandle
                .get<Long>("openDashboard")

            MainScreen(
                onAddServer = { navController.navigate(Route.AddServer) },
                onLogin = { serverId -> navController.navigate(Route.Login(serverId)) },
                openDashboardServerId = openDashboard,
                onDashboardOpened = {
                    backStackEntry.savedStateHandle.remove<Long>("openDashboard")
                },
            )
        }
        composable<Route.AddServer> {
            AddServerScreen(
                onBack = { navController.popBackStack() },
                onSaved = { serverId ->
                    navController.popBackStack()
                    navController.navigate(Route.Login(serverId))
                },
            )
        }
        composable<Route.Login> {
            LoginScreen(
                onBack = { navController.popBackStack() },
                onSignedIn = { serverId ->
                    // Pop Login and tell MainScreen to open the dashboard
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("openDashboard", serverId)
                    navController.popBackStack()
                },
            )
        }
    }
}
