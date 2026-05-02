package de.kiefer_networks.proxmoxopen.ui.addserver

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.kiefer_networks.proxmoxopen.auth.AuthGate
import de.kiefer_networks.proxmoxopen.data.secrets.SecretStoreLockedException
import de.kiefer_networks.proxmoxopen.domain.model.Realm
import de.kiefer_networks.proxmoxopen.domain.repository.ServerProbe
import de.kiefer_networks.proxmoxopen.domain.result.ApiError
import de.kiefer_networks.proxmoxopen.domain.result.ApiResult
import de.kiefer_networks.proxmoxopen.domain.usecase.AddServerUseCase
import de.kiefer_networks.proxmoxopen.domain.usecase.ProbeServerUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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

data class AddServerUiState(
    val name: String = "",
    val host: String = "",
    val port: String = "8006",
    val realm: Realm = Realm.PVE_TOKEN,
    val username: String = "",
    val tokenId: String = "",
    val tokenSecret: String = "",
    val password: String = "",
    val isConnecting: Boolean = false,
    val probe: ServerProbe? = null,
    val error: ApiError? = null,
    val savedServerId: Long? = null,
)

@HiltViewModel
class AddServerViewModel @Inject constructor(
    private val probeServer: ProbeServerUseCase,
    private val addServer: AddServerUseCase,
    private val authGate: AuthGate,
) : ViewModel() {

    private val _state = MutableStateFlow(AddServerUiState())
    val state = _state.asStateFlow()

    fun onName(v: String) = _state.update { it.copy(name = v, error = null) }
    fun onHost(v: String) = _state.update { it.copy(host = v, error = null) }
    fun onPort(v: String) = _state.update { it.copy(port = v.filter { c -> c.isDigit() }, error = null) }
    fun onRealm(v: Realm) = _state.update { it.copy(realm = v, error = null) }
    fun onUsername(v: String) = _state.update { it.copy(username = v, error = null) }
    fun onTokenId(v: String) = _state.update { it.copy(tokenId = v, error = null) }
    fun onTokenSecret(v: String) = _state.update { it.copy(tokenSecret = v, error = null) }
    fun onPassword(v: String) = _state.update { it.copy(password = v, error = null) }

    fun connect() {
        val s = _state.value
        val port = s.port.toIntOrNull() ?: return
        _state.update { it.copy(isConnecting = true, error = null) }
        viewModelScope.launch {
            when (val result = probeServer(s.host, port)) {
                is ApiResult.Success -> _state.update {
                    it.copy(isConnecting = false, probe = result.value)
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(isConnecting = false, error = result.error)
                }
            }
        }
    }

    fun confirmTrust() {
        val s = _state.value
        val probe = s.probe ?: return
        val port = s.port.toIntOrNull() ?: return
        viewModelScope.launch {
            val id = try {
                authGate.withSecretAuth(
                    title = "ProxMoxOpen",
                    subtitle = "Authenticate to save server credentials",
                ) {
                    addServer(
                        name = s.name.ifBlank { s.host },
                        host = s.host,
                        port = port,
                        realm = s.realm,
                        username = s.username.ifBlank { null },
                        tokenId = s.tokenId.ifBlank { null },
                        fingerprintSha256 = probe.sha256Fingerprint,
                        tokenSecret = s.tokenSecret.ifBlank { null },
                    )
                }
            } catch (_: SecretStoreLockedException) {
                _state.update {
                    it.copy(error = ApiError.Auth("Authentication required to save server"))
                }
                return@launch
            }
            // Clear plaintext password from UI state immediately after save (F-006).
            _state.update { it.copy(savedServerId = id, probe = null, password = "") }
        }
    }

    fun dismissProbe() {
        _state.update { it.copy(probe = null) }
    }
}
