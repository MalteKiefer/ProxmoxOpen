package app.proxmoxopen.ui.guestconfig

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import app.proxmoxopen.domain.model.GuestConfig
import app.proxmoxopen.domain.model.GuestType
import app.proxmoxopen.domain.model.NetworkInterface
import app.proxmoxopen.domain.repository.GuestRepository
import app.proxmoxopen.domain.result.ApiError
import app.proxmoxopen.domain.result.ApiResult
import app.proxmoxopen.ui.nav.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GuestConfigUiState(
    val config: GuestConfig? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: ApiError? = null,
    val saved: Boolean = false,
    // Editable fields
    val name: String = "",
    val hostname: String = "",
    val onboot: Boolean = false,
    val nameserver: String = "",
    val searchdomain: String = "",
    val nets: List<NetworkInterface> = emptyList(),
)

@HiltViewModel
class GuestConfigViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val guestRepository: GuestRepository,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<Route.GuestConfig>()
    val serverId = route.serverId
    val node = route.node
    val vmid = route.vmid
    val type = GuestType.fromApiPath(route.type) ?: GuestType.QEMU

    private val _state = MutableStateFlow(GuestConfigUiState())
    val state = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = guestRepository.getGuestConfig(serverId, node, vmid, type)) {
                is ApiResult.Success -> {
                    val c = result.value
                    _state.update {
                        it.copy(
                            isLoading = false,
                            config = c,
                            name = c.name,
                            hostname = c.hostname ?: "",
                            onboot = c.onboot,
                            nameserver = c.nameserver ?: "",
                            searchdomain = c.searchdomain ?: "",
                            nets = c.networkInterfaces,
                        )
                    }
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(isLoading = false, error = result.error)
                }
            }
        }
    }

    fun onName(v: String) = _state.update { it.copy(name = v) }
    fun onHostname(v: String) = _state.update { it.copy(hostname = v) }
    fun onOnboot(v: Boolean) = _state.update { it.copy(onboot = v) }
    fun onNameserver(v: String) = _state.update { it.copy(nameserver = v) }
    fun onSearchdomain(v: String) = _state.update { it.copy(searchdomain = v) }

    fun onNetIp(index: Int, ip: String) {
        _state.update {
            val updated = it.nets.toMutableList()
            if (index in updated.indices) updated[index] = updated[index].copy(ip = ip)
            it.copy(nets = updated)
        }
    }

    fun onNetGw(index: Int, gw: String) {
        _state.update {
            val updated = it.nets.toMutableList()
            if (index in updated.indices) updated[index] = updated[index].copy(gw = gw)
            it.copy(nets = updated)
        }
    }

    fun save() {
        val s = _state.value
        _state.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            val params = mutableMapOf<String, String>()
            val orig = s.config
            if (orig != null) {
                if (s.name != orig.name) {
                    if (type == GuestType.LXC) params["hostname"] = s.name
                    else params["name"] = s.name
                }
                if (s.hostname.isNotBlank() && s.hostname != orig.hostname) params["hostname"] = s.hostname
                if (s.onboot != orig.onboot) params["onboot"] = if (s.onboot) "1" else "0"
                if (s.nameserver != (orig.nameserver ?: "")) params["nameserver"] = s.nameserver
                if (s.searchdomain != (orig.searchdomain ?: "")) params["searchdomain"] = s.searchdomain
                s.nets.forEachIndexed { i, net ->
                    val origNet = orig.networkInterfaces.getOrNull(i)
                    if (origNet == null || net.ip != origNet.ip || net.gw != origNet.gw) {
                        params[net.id] = net.toProxmoxString()
                    }
                }
            }
            if (params.isEmpty()) {
                _state.update { it.copy(isSaving = false, saved = true) }
                return@launch
            }
            when (val result = guestRepository.setGuestConfig(serverId, node, vmid, type, params)) {
                is ApiResult.Success -> _state.update { it.copy(isSaving = false, saved = true) }
                is ApiResult.Failure -> _state.update { it.copy(isSaving = false, error = result.error) }
            }
        }
    }
}
