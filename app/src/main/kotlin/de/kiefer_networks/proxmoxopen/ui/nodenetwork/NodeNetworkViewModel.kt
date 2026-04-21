package de.kiefer_networks.proxmoxopen.ui.nodenetwork

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import de.kiefer_networks.proxmoxopen.domain.model.NodeNetworkIface
import de.kiefer_networks.proxmoxopen.domain.repository.NodeNetworkRepository
import de.kiefer_networks.proxmoxopen.domain.result.ApiError
import de.kiefer_networks.proxmoxopen.domain.result.ApiResult
import de.kiefer_networks.proxmoxopen.preferences.RefreshInterval
import de.kiefer_networks.proxmoxopen.preferences.UserPreferencesRepository
import de.kiefer_networks.proxmoxopen.ui.nav.Route
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NodeNetworkUiState(
    val interfaces: List<NodeNetworkIface> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: ApiError? = null,
)

@HiltViewModel
class NodeNetworkViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: NodeNetworkRepository,
    private val prefsRepo: UserPreferencesRepository,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<Route.NodeNetwork>()
    val serverId: Long = route.serverId
    val node: String = route.node

    private val _state = MutableStateFlow(NodeNetworkUiState())
    val state = _state.asStateFlow()

    private var autoRefreshJob: Job? = null

    init {
        refresh()
        startAutoRefresh()
    }

    private fun startAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = viewModelScope.launch {
            prefsRepo.preferences.collectLatest { prefs ->
                if (prefs.refreshInterval == RefreshInterval.OFF) return@collectLatest
                while (true) {
                    delay(prefs.refreshInterval.seconds * 1000L)
                    refresh(silent = true)
                }
            }
        }
    }

    fun refresh(silent: Boolean = false) {
        _state.update {
            if (silent) it.copy(isRefreshing = true, error = null)
            else it.copy(isLoading = it.interfaces.isEmpty(), isRefreshing = true, error = null)
        }
        viewModelScope.launch {
            val result = repo.listInterfaces(serverId, node)
            _state.update {
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    interfaces = (result as? ApiResult.Success)?.value ?: it.interfaces,
                    error = (result as? ApiResult.Failure)?.error,
                )
            }
        }
    }
}
