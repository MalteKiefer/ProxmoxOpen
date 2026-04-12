package de.kiefer_networks.proxmoxopen.ui.console

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import de.kiefer_networks.proxmoxopen.data.api.session.ProxmoxSessionManager
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
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.security.SecureRandom
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

data class ConsoleUiState(
    val isConnecting: Boolean = true,
    val isConnected: Boolean = false,
    val error: String? = null,
    val terminalOutput: String = "",
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

    private var webSocket: WebSocket? = null
    private val outputBuffer = StringBuilder()

    init { connect() }

    fun connect() {
        _state.update { it.copy(isConnecting = true, error = null) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val server = serverRepository.getById(serverId)
                    ?: run { _state.update { it.copy(isConnecting = false, error = "Server not found") }; return@launch }

                val session = sessionManager.getSession(serverId)
                    ?: run { _state.update { it.copy(isConnecting = false, error = "Not authenticated") }; return@launch }

                // Get credentials for API token servers
                val credentials = if (server.realm == Realm.PVE_TOKEN) {
                    val secret = serverRepository.getTokenSecret(serverId)
                    if (secret != null) de.kiefer_networks.proxmoxopen.domain.model.Credentials.ApiToken(
                        username = server.username ?: "root",
                        realm = server.realm,
                        tokenId = server.tokenId ?: "",
                        tokenSecret = secret,
                    ) else null
                } else null

                val apiClient = sessionManager.apiClient(server, credentials)

                // Create termproxy / vncproxy
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
                _state.update { it.copy(title = title) }

                // Build WebSocket URL
                val wsPath = when (type) {
                    "node" -> "/api2/json/nodes/$node/vncwebsocket"
                    "lxc" -> "/api2/json/nodes/$node/lxc/$vmid/vncwebsocket"
                    "qemu" -> "/api2/json/nodes/$node/qemu/$vmid/vncwebsocket"
                    else -> "/api2/json/nodes/$node/lxc/$vmid/vncwebsocket"
                }
                val wsUrl = "wss://${server.host}:${server.port}$wsPath?port=${proxy.port}&vncticket=${java.net.URLEncoder.encode(proxy.ticket, "UTF-8")}"

                // Build OkHttp client with TOFU TLS
                val trustManager = de.kiefer_networks.proxmoxopen.data.api.tls.TofuTrustManager(server.fingerprintSha256)
                val sslContext = SSLContext.getInstance("TLS").apply {
                    init(null, arrayOf<X509TrustManager>(trustManager), SecureRandom())
                }
                val okClient = OkHttpClient.Builder()
                    .sslSocketFactory(sslContext.socketFactory, trustManager)
                    .hostnameVerifier { _, _ -> true }
                    .pingInterval(30, TimeUnit.SECONDS)
                    .readTimeout(0, TimeUnit.MILLISECONDS)
                    .build()

                val request = Request.Builder()
                    .url(wsUrl)
                    .addHeader("Cookie", "PVEAuthCookie=${session.ticket}")
                    .build()

                webSocket = okClient.newWebSocket(request, object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        _state.update { it.copy(isConnecting = false, isConnected = true) }
                    }

                    override fun onMessage(webSocket: WebSocket, text: String) {
                        outputBuffer.append(text)
                        _state.update { it.copy(terminalOutput = outputBuffer.toString()) }
                    }

                    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                        _state.update { it.copy(isConnecting = false, isConnected = false, error = t.message ?: "Connection failed") }
                    }

                    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                        _state.update { it.copy(isConnected = false, error = "Connection closed: $reason") }
                    }
                })
            } catch (e: Exception) {
                _state.update { it.copy(isConnecting = false, error = e.message ?: "Unknown error") }
            }
        }
    }

    fun sendInput(text: String) {
        webSocket?.send(text)
    }

    override fun onCleared() {
        super.onCleared()
        webSocket?.close(1000, "Screen closed")
    }
}
