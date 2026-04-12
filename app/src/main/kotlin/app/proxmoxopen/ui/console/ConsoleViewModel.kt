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
    val type = route.type

    private val _state = MutableStateFlow(ConsoleUiState())
    val state = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val server = serverRepository.getById(serverId)
                    ?: run { _state.update { it.copy(isLoading = false, error = "Server not found") }; return@launch }

                val session = sessionManager.getSession(serverId)
                    ?: run { _state.update { it.copy(isLoading = false, error = "Not authenticated") }; return@launch }

                val apiClient = sessionManager.apiClient(server, null)
                val vncProxy = apiClient.createVncProxy(node, type, vmid)

                val consoleType = when (type) {
                    "qemu" -> "kvm"
                    "lxc" -> "lxc"
                    else -> "kvm"
                }

                val url = "https://${server.host}:${server.port}/" +
                    "?console=$consoleType" +
                    "&novnc=1" +
                    "&vmid=$vmid" +
                    "&vmname=" +
                    "&node=$node" +
                    "&resize=off"

                _state.update {
                    it.copy(
                        isLoading = false,
                        consoleUrl = url,
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
