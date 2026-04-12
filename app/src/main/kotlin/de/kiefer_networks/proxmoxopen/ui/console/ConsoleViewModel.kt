package de.kiefer_networks.proxmoxopen.ui.console

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import de.kiefer_networks.proxmoxopen.data.api.session.ProxmoxSessionManager
import de.kiefer_networks.proxmoxopen.domain.model.Credentials
import de.kiefer_networks.proxmoxopen.domain.model.Realm
import de.kiefer_networks.proxmoxopen.domain.repository.ServerRepository
import de.kiefer_networks.proxmoxopen.ui.nav.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ConsoleUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val consoleUrl: String? = null,
    val serverHost: String? = null,
    val serverPort: Int = 8006,
    val fingerprint: String? = null,
    val title: String = "Console",
)

@HiltViewModel
class ConsoleViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val serverRepository: ServerRepository,
    private val sessionManager: ProxmoxSessionManager,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<Route.Console>()
    val serverId = route.serverId
    val node = route.node
    val vmid = route.vmid
    val type = route.type

    private val _state = MutableStateFlow(ConsoleUiState())
    val state = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val server = serverRepository.getById(serverId)
                        ?: run { _state.update { it.copy(isLoading = false, error = "Server not found") }; return@withContext }
                    val session = sessionManager.getSession(serverId)
                        ?: run { _state.update { it.copy(isLoading = false, error = "Not authenticated") }; return@withContext }

                    val credentials: Credentials? = if (server.realm == Realm.PVE_TOKEN) {
                        val secret = serverRepository.getTokenSecret(serverId)
                        if (secret != null) Credentials.ApiToken(server.username ?: "root", server.realm, server.tokenId ?: "", secret) else null
                    } else null

                    val apiClient = sessionManager.apiClient(server, credentials)
                    val proxy = when (type) {
                        "node" -> apiClient.createNodeTermProxy(node)
                        "lxc" -> apiClient.createLxcTermProxy(node, vmid)
                        "qemu" -> apiClient.createVncProxy(node, "qemu", vmid)
                        else -> apiClient.createLxcTermProxy(node, vmid)
                    }

                    val title = when (type) {
                        "node" -> "Shell: $node"
                        "lxc" -> "CT $vmid"
                        "qemu" -> "VM $vmid"
                        else -> "Console"
                    }

                    // Build URL for local noVNC HTML with parameters
                    val url = "file:///android_asset/novnc/console.html" +
                        "?host=${server.host}" +
                        "&port=${server.port}" +
                        "&node=$node" +
                        "&vmid=$vmid" +
                        "&type=$type" +
                        "&wsport=${proxy.port}" +
                        "&vncticket=${java.net.URLEncoder.encode(proxy.ticket, "UTF-8")}" +
                        "&token=${java.net.URLEncoder.encode(session.ticket, "UTF-8")}"

                    _state.update {
                        it.copy(
                            isLoading = false,
                            consoleUrl = url,
                            serverHost = server.host,
                            serverPort = server.port,
                            fingerprint = server.fingerprintSha256,
                            title = title,
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message ?: "Error") }
            }
        }
    }
}
