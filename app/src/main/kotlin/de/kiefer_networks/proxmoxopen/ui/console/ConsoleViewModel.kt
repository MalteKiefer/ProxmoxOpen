package de.kiefer_networks.proxmoxopen.ui.console

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.termux.terminal.TerminalEmulator
import com.termux.terminal.TerminalOutput
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient
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
import okio.ByteString
import java.security.SecureRandom
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

data class ConsoleUiState(
    val isConnecting: Boolean = true,
    val isConnected: Boolean = false,
    val error: String? = null,
    val title: String = "Console",
    val sessionReady: Boolean = false,
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
    private val mainHandler = Handler(Looper.getMainLooper())

    // Termux terminal session — we use it as an emulator only (no local process)
    var terminalSession: TerminalSession? = null
        private set

    private val sessionClient = object : TerminalSessionClient {
        override fun onTextChanged(changedSession: TerminalSession) {}
        override fun onTitleChanged(changedSession: TerminalSession) {}
        override fun onSessionFinished(finishedSession: TerminalSession) {}
        override fun onCopyTextToClipboard(session: TerminalSession, text: String?) {}
        override fun onPasteTextFromClipboard(session: TerminalSession?) {}
        override fun onBell(session: TerminalSession) {}
        override fun onColorsChanged(session: TerminalSession) {}
        override fun onTerminalCursorStateChange(state: Boolean) {}
        override fun setTerminalShellPid(session: TerminalSession, pid: Int) {}
        override fun getTerminalCursorStyle(): Int = TerminalEmulator.TERMINAL_CURSOR_STYLE_UNDERLINE
        override fun logError(tag: String?, message: String?) {}
        override fun logWarn(tag: String?, message: String?) {}
        override fun logInfo(tag: String?, message: String?) {}
        override fun logDebug(tag: String?, message: String?) {}
        override fun logVerbose(tag: String?, message: String?) {}
        override fun logStackTraceWithMessage(tag: String?, message: String?, e: Exception?) {}
        override fun logStackTrace(tag: String?, e: Exception?) {}
    }

    init { connect() }

    fun connect() {
        _state.update { it.copy(isConnecting = true, error = null) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val server = serverRepository.getById(serverId)
                    ?: run { _state.update { it.copy(isConnecting = false, error = "Server not found") }; return@launch }

                val session = sessionManager.getSession(serverId)
                    ?: run { _state.update { it.copy(isConnecting = false, error = "Not authenticated") }; return@launch }

                val credentials = if (server.realm == Realm.PVE_TOKEN) {
                    val secret = serverRepository.getTokenSecret(serverId)
                    if (secret != null) de.kiefer_networks.proxmoxopen.domain.model.Credentials.ApiToken(
                        username = server.username ?: "root", realm = server.realm,
                        tokenId = server.tokenId ?: "", tokenSecret = secret,
                    ) else null
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

                // Create TerminalSession with dummy shell — we won't start a local process
                val ts = TerminalSession("/bin/false", "/", arrayOfNulls(0), arrayOfNulls(0), null, sessionClient)
                ts.updateTerminalSessionClient(sessionClient)
                terminalSession = ts

                _state.update { it.copy(title = title, sessionReady = true) }

                // WebSocket
                val wsPath = when (type) {
                    "node" -> "/api2/json/nodes/$node/vncwebsocket"
                    "lxc" -> "/api2/json/nodes/$node/lxc/$vmid/vncwebsocket"
                    "qemu" -> "/api2/json/nodes/$node/qemu/$vmid/vncwebsocket"
                    else -> "/api2/json/nodes/$node/lxc/$vmid/vncwebsocket"
                }
                val wsUrl = "wss://${server.host}:${server.port}$wsPath?port=${proxy.port}&vncticket=${java.net.URLEncoder.encode(proxy.ticket, "UTF-8")}"

                val trustManager = de.kiefer_networks.proxmoxopen.data.api.tls.TofuTrustManager(server.fingerprintSha256)
                val sslContext = SSLContext.getInstance("TLS").apply {
                    init(null, arrayOf<X509TrustManager>(trustManager), SecureRandom())
                }
                val okClient = OkHttpClient.Builder()
                    .sslSocketFactory(sslContext.socketFactory, trustManager)
                    .hostnameVerifier { _, _ -> true }
                    .pingInterval(10, TimeUnit.SECONDS)
                    .readTimeout(0, TimeUnit.MILLISECONDS)
                    .build()

                val request = Request.Builder()
                    .url(wsUrl)
                    .addHeader("Cookie", "PVEAuthCookie=${session.ticket}")
                    .build()

                webSocket = okClient.newWebSocket(request, object : WebSocketListener() {
                    override fun onOpen(ws: WebSocket, response: Response) {
                        _state.update { it.copy(isConnecting = false, isConnected = true) }
                    }

                    override fun onMessage(ws: WebSocket, text: String) {
                        // Feed terminal data to Termux emulator
                        val bytes = text.toByteArray(Charsets.UTF_8)
                        mainHandler.post {
                            terminalSession?.emulator?.append(bytes, bytes.size)
                        }
                    }

                    override fun onMessage(ws: WebSocket, bytes: ByteString) {
                        val data = bytes.toByteArray()
                        mainHandler.post {
                            terminalSession?.emulator?.append(data, data.size)
                        }
                    }

                    override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                        _state.update { it.copy(isConnecting = false, isConnected = false, error = t.message ?: "Connection failed") }
                    }

                    override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                        _state.update { it.copy(isConnected = false) }
                    }
                })
            } catch (e: Exception) {
                _state.update { it.copy(isConnecting = false, error = e.message ?: "Error") }
            }
        }
    }

    fun sendInput(data: ByteArray) {
        webSocket?.send(okio.ByteString.of(*data))
    }

    fun sendInput(text: String) {
        webSocket?.send(text)
    }

    fun updateSize(rows: Int, cols: Int) {
        terminalSession?.updateSize(cols, rows, 0, 0)
        // Tell Proxmox about the new size
        webSocket?.send("\u001b[8;${rows};${cols}t")
    }

    override fun onCleared() {
        super.onCleared()
        webSocket?.close(1000, "closed")
    }
}
