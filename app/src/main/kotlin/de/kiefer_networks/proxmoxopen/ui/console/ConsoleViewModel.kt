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
    val noVncUrl: String? = null,
    val title: String = "Console",
)

@HiltViewModel
class ConsoleViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val serverRepository: ServerRepository,
    private val sessionManager: ProxmoxSessionManager,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<Route.Console>()
    private val node = route.node
    private val vmid = route.vmid
    private val type = route.type

    private val _state = MutableStateFlow(ConsoleUiState())
    val state = _state.asStateFlow()

    private var proxy: LocalWebSocketProxy? = null

    init { load() }

    fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val server = serverRepository.getById(route.serverId)
                        ?: run { _state.update { it.copy(isLoading = false, error = "Server not found") }; return@withContext }
                    val session = sessionManager.getSession(route.serverId)
                        ?: run { _state.update { it.copy(isLoading = false, error = "Not authenticated") }; return@withContext }

                    val credentials: Credentials? = if (server.realm == Realm.PVE_TOKEN) {
                        val secret = serverRepository.getTokenSecret(route.serverId)
                        if (secret != null) Credentials.ApiToken(server.username ?: "root", server.realm, server.tokenId ?: "", secret) else null
                    } else null

                    val apiClient = sessionManager.apiClient(server, credentials)
                    val proxyTicket = when (type) {
                        "node" -> apiClient.createNodeTermProxy(node)
                        "lxc" -> apiClient.createLxcTermProxy(node, vmid)
                        "qemu" -> apiClient.createVncProxy(node, "qemu", vmid)
                        else -> apiClient.createLxcTermProxy(node, vmid)
                    }

                    val title = when (type) {
                        "node" -> "Shell: $node"; "lxc" -> "CT $vmid"; "qemu" -> "VM $vmid"; else -> "Console"
                    }

                    // Build upstream WebSocket URL
                    val wsPath = when (type) {
                        "node" -> "/api2/json/nodes/$node/vncwebsocket"
                        "lxc" -> "/api2/json/nodes/$node/lxc/$vmid/vncwebsocket"
                        "qemu" -> "/api2/json/nodes/$node/qemu/$vmid/vncwebsocket"
                        else -> "/api2/json/nodes/$node/lxc/$vmid/vncwebsocket"
                    }
                    val upstreamUrl = "wss://${server.host}:${server.port}$wsPath?port=${proxyTicket.port}&vncticket=${java.net.URLEncoder.encode(proxyTicket.ticket, "UTF-8")}"

                    // Start local WebSocket proxy
                    proxy?.stop()
                    val localProxy = LocalWebSocketProxy(
                        targetUrl = upstreamUrl,
                        cookie = session.ticket,
                        fingerprint = server.fingerprintSha256,
                    )
                    val localPort = localProxy.start()
                    proxy = localProxy

                    // Build noVNC URL pointing to local proxy (no SSL!)
                    val noVncUrl = "file:///android_asset/novnc/console.html" +
                        "?host=127.0.0.1" +
                        "&port=$localPort" +
                        "&path=" +
                        "&encrypt=0" +
                        "&password=${java.net.URLEncoder.encode(proxyTicket.ticket, "UTF-8")}"

                    _state.update { it.copy(isLoading = false, noVncUrl = noVncUrl, title = title) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message ?: "Error") }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        proxy?.stop()
    }
}
