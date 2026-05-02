package de.kiefer_networks.proxmoxopen.ui.console

import android.app.Application
import timber.log.Timber
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import de.kiefer_networks.proxmoxopen.auth.AuthGate
import de.kiefer_networks.proxmoxopen.data.secrets.SecretStoreLockedException
import de.kiefer_networks.proxmoxopen.domain.repository.ConsoleRepository
import de.kiefer_networks.proxmoxopen.preferences.UserPreferencesRepository
import de.kiefer_networks.proxmoxopen.ui.nav.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import de.kiefer_networks.proxmoxopen.core.common.DispatcherProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

private suspend fun <T> AuthGate.withSecretAuth(
    title: String,
    subtitle: String? = null,
    block: suspend () -> T,
): T {
    return try {
        block()
    } catch (_: SecretStoreLockedException) {
        if (ensureFreshAuth(title, subtitle)) {
            block()
        } else {
            throw SecretStoreLockedException()
        }
    }
}

data class ConsoleUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val webViewUrl: String? = null,
    val bootstrap: ConsoleBootstrap? = null,
    val title: String = "Console",
)

/**
 * Bootstrap data injected into the WebView via JS after page-load.
 * Keeps secrets (password / ticket) out of the WebView URL itself, which is
 * otherwise visible to history APIs, embeds and crash logs.
 */
@Serializable
data class ConsoleBootstrap(
    val host: String,
    val port: Int,
    val sessionSecret: String,
    val password: String? = null,
    val user: String? = null,
    val ticket: String? = null,
    val fontSize: Int? = null,
    val theme: String? = null,
    val isTerminal: Boolean,
)

@HiltViewModel
class ConsoleViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val application: Application,
    private val consoleRepository: ConsoleRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val dispatchers: DispatcherProvider,
    private val authGate: AuthGate,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<Route.Console>()
    private val node = route.node
    private val vmid = route.vmid
    private val type = route.type

    private val _state = MutableStateFlow(ConsoleUiState())
    val state = _state.asStateFlow()
    @Volatile private var proxy: LocalWebSocketProxy? = null
    private var loadJob: Job? = null

    init { load() }

    fun load() {
        loadJob?.cancel()
        _state.update { it.copy(isLoading = true, error = null) }
        loadJob = viewModelScope.launch {
            try {
                withContext(dispatchers.io) {
                    val info = try {
                        authGate.withSecretAuth(
                            title = "ProxMoxOpen",
                            subtitle = "Authenticate to open console",
                        ) {
                            consoleRepository.createConsoleProxy(
                                serverId = route.serverId,
                                node = node,
                                vmid = vmid,
                                type = type,
                            )
                        }
                    } catch (_: SecretStoreLockedException) {
                        throw IllegalStateException("Authentication required to open console")
                    }

                    val title = when (type) {
                        "node" -> "Console: $node"; "lxc" -> "CT $vmid"; "qemu" -> "VM $vmid"; else -> "Console"
                    }

                    val assetDir = if (type == "qemu") "novnc" else "xterm"
                    val indexFile = if (type == "qemu") "console.html" else "terminal.html"
                    val isTerminal = type != "qemu"

                    proxy?.stop()
                    val localProxy = LocalWebSocketProxy(
                        targetUrl = info.upstreamUrl,
                        cookie = info.cookie,
                        fingerprint = info.fingerprint,
                        context = application,
                        assetDir = assetDir,
                        indexFile = indexFile,
                        isTerminal = isTerminal,
                    )
                    val localPort = localProxy.start()
                    proxy = localProxy
                    val sessionSecret = localProxy.sessionSecret

                    val prefs = preferencesRepository.preferences.first()

                    // F-014: keep the VNC/xterm ticket OUT of the WebView URL.
                    // The URL only carries the session secret (path prefix) so the proxy
                    // can authorize the request. The actual auth ticket / password is
                    // injected via window.__pxo after page load.
                    val url = "http://127.0.0.1:$localPort/$sessionSecret/$indexFile"

                    // Spec requires URLEncoder-encoded user; we still encode it here so the
                    // bootstrap object matches the original on-the-wire payload that xterm
                    // received via URLSearchParams.
                    val pveUserEncoded = java.net.URLEncoder.encode(info.username, "UTF-8")

                    val bootstrap = if (isTerminal) {
                        ConsoleBootstrap(
                            host = "127.0.0.1",
                            port = localPort,
                            sessionSecret = sessionSecret,
                            user = pveUserEncoded,
                            ticket = info.vncTicket,
                            fontSize = prefs.terminalFontSize.px,
                            theme = prefs.terminalTheme.name,
                            isTerminal = true,
                        )
                    } else {
                        ConsoleBootstrap(
                            host = "127.0.0.1",
                            port = localPort,
                            sessionSecret = sessionSecret,
                            password = info.vncTicket,
                            isTerminal = false,
                        )
                    }

                    _state.update {
                        it.copy(
                            isLoading = false,
                            webViewUrl = url,
                            bootstrap = bootstrap,
                            title = title,
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "load failed")
                _state.update { it.copy(isLoading = false, error = e.message ?: "Error") }
            }
        }
    }

    /**
     * Fully terminate the proxy (called from the screen on Lifecycle.ON_STOP so a
     * backgrounded console does not keep the loopback listener / upstream socket open).
     */
    fun stopProxy() {
        proxy?.stop()
    }

    override fun onCleared() {
        super.onCleared()
        proxy?.stop()
    }
}
