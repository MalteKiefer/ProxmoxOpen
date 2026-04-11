package app.proxmoxopen.ui.addserver

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.proxmoxopen.domain.model.Realm
import app.proxmoxopen.domain.repository.ServerProbe
import app.proxmoxopen.domain.result.ApiError
import app.proxmoxopen.domain.result.ApiResult
import app.proxmoxopen.domain.usecase.AddServerUseCase
import app.proxmoxopen.domain.usecase.ProbeServerUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
            val id = addServer(
                name = s.name.ifBlank { s.host },
                host = s.host,
                port = port,
                realm = s.realm,
                username = s.username.ifBlank { null },
                tokenId = s.tokenId.ifBlank { null },
                fingerprintSha256 = probe.sha256Fingerprint,
                tokenSecret = s.tokenSecret.ifBlank { null },
                password = s.password.ifBlank { null },
            )
            _state.update { it.copy(savedServerId = id, probe = null) }
        }
    }

    fun dismissProbe() {
        _state.update { it.copy(probe = null) }
    }
}
