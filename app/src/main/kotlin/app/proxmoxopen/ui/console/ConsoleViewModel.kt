package app.proxmoxopen.ui.console

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import app.proxmoxopen.data.api.session.ProxmoxSessionManager
import app.proxmoxopen.domain.repository.ServerRepository
import app.proxmoxopen.ui.nav.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ConsoleUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val consoleUrl: String? = null,
    val authTicket: String? = null,
    val serverHost: String? = null,
    val serverPort: Int? = null,
    val fingerprint: String? = null,
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
    val type = route.type  // "qemu", "lxc", or "node"

    private val _state = MutableStateFlow(ConsoleUiState())
    val state = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val server = serverRepository.getById(serverId)
                    ?: run { _state.update { it.copy(isLoading = false, error = "Server not found") }; return@launch }

                val session = sessionManager.getSession(serverId)
                    ?: run { _state.update { it.copy(isLoading = false, error = "Not authenticated") }; return@launch }

                // Create the proxy ticket based on console type
                val credentials = if (server.realm == app.proxmoxopen.domain.model.Realm.PVE_TOKEN) {
                    val secret = serverRepository.getTokenSecret(serverId)
                    if (secret != null) app.proxmoxopen.domain.model.Credentials.ApiToken(
                        username = server.username ?: "root",
                        realm = server.realm,
                        tokenId = server.tokenId ?: "",
                        tokenSecret = secret,
                    ) else null
                } else null

                val apiClient = sessionManager.apiClient(server, credentials)

                // Request the appropriate proxy
                when (type) {
                    "node" -> apiClient.createNodeTermProxy(node)
                    "lxc" -> apiClient.createLxcTermProxy(node, vmid)
                    "qemu" -> apiClient.createVncProxy(node, "qemu", vmid)
                    else -> apiClient.createVncProxy(node, type, vmid)
                }

                // Build the console URL
                val consoleParam = when (type) {
                    "node" -> "shell"
                    "lxc" -> "lxc"
                    "qemu" -> "kvm"
                    else -> "kvm"
                }
                val urlBuilder = StringBuilder("https://${server.host}:${server.port}/")
                urlBuilder.append("?console=$consoleParam")
                urlBuilder.append("&novnc=1")
                urlBuilder.append("&node=$node")
                urlBuilder.append("&resize=off")
                if (type != "node") {
                    urlBuilder.append("&vmid=$vmid")
                    urlBuilder.append("&vmname=")
                }

                _state.update {
                    it.copy(
                        isLoading = false,
                        consoleUrl = urlBuilder.toString(),
                        authTicket = session.ticket,
                        serverHost = server.host,
                        serverPort = server.port,
                        fingerprint = server.fingerprintSha256,
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message ?: "Unknown error") }
            }
        }
    }
}
