package de.kiefer_networks.proxmoxopen.ui.console

import android.app.Application
import timber.log.Timber
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
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

data class ConsoleUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val webViewUrl: String? = null,
    val title: String = "Console",
)

@HiltViewModel
class ConsoleViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val application: Application,
    private val consoleRepository: ConsoleRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val dispatchers: DispatcherProvider,
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
                    val info = consoleRepository.createConsoleProxy(
                        serverId = route.serverId,
                        node = node,
                        vmid = vmid,
                        type = type,
                    )

                    val title = when (type) {
                        "node" -> "Console: $node"; "lxc" -> "CT $vmid"; "qemu" -> "VM $vmid"; else -> "Console"
                    }

                    val assetDir = if (type == "qemu") "novnc" else "xterm"
                    val indexFile = if (type == "qemu") "console.html" else "terminal.html"

                    proxy?.stop()
                    val localProxy = LocalWebSocketProxy(
                        targetUrl = info.upstreamUrl,
                        cookie = info.cookie,
                        fingerprint = info.fingerprint,
                        context = application,
                        assetDir = assetDir,
                        indexFile = indexFile,
                        isTerminal = type != "qemu",
                    )
                    val localPort = localProxy.start()
                    proxy = localProxy

                    val pveUser = java.net.URLEncoder.encode(info.username, "UTF-8")
                    val vncTicketEncoded = java.net.URLEncoder.encode(info.vncTicket, "UTF-8")
                    val prefs = preferencesRepository.preferences.first()
                    val url = if (type == "qemu") {
                        "http://127.0.0.1:$localPort/$indexFile" +
                            "?host=127.0.0.1&port=$localPort" +
                            "&password=$vncTicketEncoded"
                    } else {
                        "http://127.0.0.1:$localPort/$indexFile" +
                            "?host=127.0.0.1&port=$localPort" +
                            "&user=$pveUser&ticket=$vncTicketEncoded" +
                            "&fontSize=${prefs.terminalFontSize.px}" +
                            "&theme=${prefs.terminalTheme.name}"
                    }

                    _state.update { it.copy(isLoading = false, webViewUrl = url, title = title) }
                }
            } catch (e: Exception) {
                Timber.e(e, "load failed")
                _state.update { it.copy(isLoading = false, error = e.message ?: "Error") }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        proxy?.stop()
    }
}
