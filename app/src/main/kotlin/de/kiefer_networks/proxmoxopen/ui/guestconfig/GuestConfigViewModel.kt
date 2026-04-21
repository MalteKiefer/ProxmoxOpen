package de.kiefer_networks.proxmoxopen.ui.guestconfig

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import de.kiefer_networks.proxmoxopen.domain.model.GuestConfig
import de.kiefer_networks.proxmoxopen.domain.model.GuestType
import de.kiefer_networks.proxmoxopen.domain.model.NetworkInterface
import de.kiefer_networks.proxmoxopen.domain.repository.GuestRepository
import de.kiefer_networks.proxmoxopen.domain.result.ApiError
import de.kiefer_networks.proxmoxopen.domain.result.ApiResult
import de.kiefer_networks.proxmoxopen.domain.util.formatTags
import de.kiefer_networks.proxmoxopen.domain.util.parseTags
import de.kiefer_networks.proxmoxopen.ui.nav.Route
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
    // General
    val hostname: String = "",
    val onboot: Boolean = false,
    val protection: Boolean = false,
    val startup: String = "",
    val description: String = "",
    // Resources
    val cores: String = "",
    val cpulimit: String = "",
    val memory: String = "",
    val swap: String = "",
    // DNS
    val nameserver: String = "",
    val searchdomain: String = "",
    // Network
    val nets: List<NetworkInterface> = emptyList(),
    // Tags (comma/semicolon separated raw input)
    val tags: String = "",
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
    val type = GuestType.fromApiPath(route.type) ?: GuestType.LXC

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
                            hostname = c.hostname ?: c.name,
                            onboot = c.onboot,
                            protection = c.protection,
                            startup = c.startup ?: "",
                            description = c.description ?: "",
                            cores = c.cores?.toString() ?: "",
                            cpulimit = c.cpulimit?.toString() ?: "",
                            memory = c.memory?.toString() ?: "",
                            swap = c.swap?.toString() ?: "",
                            nameserver = c.nameserver ?: "",
                            searchdomain = c.searchdomain ?: "",
                            nets = c.networkInterfaces,
                            tags = parseTags(c.tags).joinToString(", "),
                        )
                    }
                }
                is ApiResult.Failure -> _state.update { it.copy(isLoading = false, error = result.error) }
            }
        }
    }

    // General
    fun onHostname(v: String) = _state.update { it.copy(hostname = v) }
    fun onOnboot(v: Boolean) = _state.update { it.copy(onboot = v) }
    fun onProtection(v: Boolean) = _state.update { it.copy(protection = v) }
    fun onStartup(v: String) = _state.update { it.copy(startup = v) }
    fun onDescription(v: String) = _state.update { it.copy(description = v) }

    // Resources
    fun onCores(v: String) = _state.update { it.copy(cores = v.filter { c -> c.isDigit() }) }
    fun onCpulimit(v: String) = _state.update { it.copy(cpulimit = v) }
    fun onMemory(v: String) = _state.update { it.copy(memory = v.filter { c -> c.isDigit() }) }
    fun onSwap(v: String) = _state.update { it.copy(swap = v.filter { c -> c.isDigit() }) }

    // DNS
    fun onNameserver(v: String) = _state.update { it.copy(nameserver = v) }
    fun onSearchdomain(v: String) = _state.update { it.copy(searchdomain = v) }

    // Tags
    fun onTags(v: String) = _state.update { it.copy(tags = v) }

    // Network
    fun onNetField(index: Int, field: NetField, value: String) {
        _state.update { s ->
            val updated = s.nets.toMutableList()
            if (index !in updated.indices) return@update s
            updated[index] = when (field) {
                NetField.IP -> updated[index].copy(ip = value)
                NetField.GW -> updated[index].copy(gw = value)
                NetField.IP6 -> updated[index].copy(ip6 = value)
                NetField.GW6 -> updated[index].copy(gw6 = value)
                NetField.BRIDGE -> updated[index].copy(bridge = value)
                NetField.MTU -> updated[index].copy(mtu = value.toIntOrNull())
                NetField.RATE -> updated[index].copy(rate = value.toDoubleOrNull())
                NetField.TAG -> updated[index].copy(tag = value.toIntOrNull())
            }
            s.copy(nets = updated)
        }
    }

    fun addNetInterface() {
        _state.update { s ->
            val nextId = "net${s.nets.size}"
            val newNet = NetworkInterface(
                id = nextId, rawValue = "", name = "eth${s.nets.size}",
                bridge = "vmbr0", hwaddr = null, ip = "dhcp", gw = null,
                ip6 = null, gw6 = null, firewall = true, mtu = null,
                rate = null, tag = null, type = "veth", linkDown = false,
            )
            s.copy(nets = s.nets + newNet)
        }
    }

    fun deleteNetInterface(index: Int) {
        _state.update { s ->
            if (s.nets.size <= 1 || index !in s.nets.indices) return@update s
            val updated = s.nets.toMutableList().apply { removeAt(index) }
            // Re-number net IDs
            val renumbered = updated.mapIndexed { i, net -> net.copy(id = "net$i") }
            s.copy(nets = renumbered)
        }
    }

    fun onNetFirewall(index: Int, enabled: Boolean) {
        _state.update { s ->
            val updated = s.nets.toMutableList()
            if (index in updated.indices) updated[index] = updated[index].copy(firewall = enabled)
            s.copy(nets = updated)
        }
    }

    fun save() {
        val s = _state.value
        val orig = s.config ?: return
        _state.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            val params = buildDiff(orig, s)
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

    private fun buildDiff(orig: GuestConfig, s: GuestConfigUiState): Map<String, String> {
        val params = mutableMapOf<String, String>()
        if (s.hostname != (orig.hostname ?: orig.name)) params["hostname"] = s.hostname
        if (s.onboot != orig.onboot) params["onboot"] = if (s.onboot) "1" else "0"
        if (s.protection != orig.protection) params["protection"] = if (s.protection) "1" else "0"
        if (s.startup != (orig.startup ?: "")) params["startup"] = s.startup
        if (s.description != (orig.description ?: "")) params["description"] = s.description
        s.cores.toIntOrNull()?.let { if (it != orig.cores) params["cores"] = it.toString() }
        s.cpulimit.toDoubleOrNull()?.let { if (it != orig.cpulimit) params["cpulimit"] = it.toString() }
        s.memory.toIntOrNull()?.let { if (it != orig.memory) params["memory"] = it.toString() }
        s.swap.toIntOrNull()?.let { if (it != orig.swap) params["swap"] = it.toString() }
        if (s.nameserver != (orig.nameserver ?: "")) params["nameserver"] = s.nameserver
        if (s.searchdomain != (orig.searchdomain ?: "")) params["searchdomain"] = s.searchdomain
        val newTags = formatTags(parseTags(s.tags))
        val origTags = formatTags(parseTags(orig.tags))
        if (newTags != origTags) params["tags"] = newTags ?: ""
        s.nets.forEachIndexed { i, net ->
            val origNet = orig.networkInterfaces.getOrNull(i)
            if (origNet == null || net != origNet) {
                params[net.id] = net.toProxmoxString()
            }
        }
        return params
    }
}

enum class NetField { IP, GW, IP6, GW6, BRIDGE, MTU, RATE, TAG }
