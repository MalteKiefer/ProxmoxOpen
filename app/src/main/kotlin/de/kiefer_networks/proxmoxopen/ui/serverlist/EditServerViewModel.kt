package de.kiefer_networks.proxmoxopen.ui.serverlist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import de.kiefer_networks.proxmoxopen.domain.model.Realm
import de.kiefer_networks.proxmoxopen.domain.model.Server
import de.kiefer_networks.proxmoxopen.domain.repository.ServerRepository
import de.kiefer_networks.proxmoxopen.ui.nav.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditServerUiState(
    val name: String = "",
    val host: String = "",
    val port: String = "8006",
    val realm: Realm = Realm.PAM,
    val username: String = "",
    val tokenId: String = "",
    val saved: Boolean = false,
)

@HiltViewModel
class EditServerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val serverRepository: ServerRepository,
) : ViewModel() {

    private val serverId: Long = savedStateHandle.toRoute<Route.EditServer>().serverId

    private val _state = MutableStateFlow(EditServerUiState())
    val state = _state.asStateFlow()

    private var original: Server? = null

    init {
        viewModelScope.launch {
            val server = serverRepository.getById(serverId) ?: return@launch
            original = server
            _state.update {
                it.copy(
                    name = server.name,
                    host = server.host,
                    port = server.port.toString(),
                    realm = server.realm,
                    username = server.username.orEmpty(),
                    tokenId = server.tokenId.orEmpty(),
                )
            }
        }
    }

    fun onName(v: String) = _state.update { it.copy(name = v) }
    fun onHost(v: String) = _state.update { it.copy(host = v) }
    fun onPort(v: String) = _state.update { it.copy(port = v.filter { c -> c.isDigit() }) }
    fun onUsername(v: String) = _state.update { it.copy(username = v) }
    fun onTokenId(v: String) = _state.update { it.copy(tokenId = v) }

    fun save() {
        val s = _state.value
        val orig = original ?: return
        val port = s.port.toIntOrNull() ?: return
        viewModelScope.launch {
            val updated = orig.copy(
                name = s.name.ifBlank { orig.name },
                host = s.host.ifBlank { orig.host },
                port = port,
                username = s.username.ifBlank { null },
                tokenId = s.tokenId.ifBlank { null },
            )
            serverRepository.update(updated)
            _state.update { it.copy(saved = true) }
        }
    }
}
