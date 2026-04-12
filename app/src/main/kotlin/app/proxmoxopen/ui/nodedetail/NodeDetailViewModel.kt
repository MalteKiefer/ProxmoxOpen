package app.proxmoxopen.ui.nodedetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import app.proxmoxopen.domain.model.Node
import app.proxmoxopen.domain.model.RrdPoint
import app.proxmoxopen.domain.model.RrdTimeframe
import app.proxmoxopen.domain.repository.ClusterRepository
import app.proxmoxopen.domain.result.ApiError
import app.proxmoxopen.domain.result.ApiResult
import app.proxmoxopen.domain.usecase.GetNodeRrdUseCase
import app.proxmoxopen.preferences.RefreshInterval
import app.proxmoxopen.preferences.UserPreferencesRepository
import app.proxmoxopen.ui.nav.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NodeDetailUiState(
    val node: Node? = null,
    val rrd: List<RrdPoint> = emptyList(),
    val timeframe: RrdTimeframe = RrdTimeframe.HOUR,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: ApiError? = null,
)

@HiltViewModel
class NodeDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val cluster: ClusterRepository,
    private val getRrd: GetNodeRrdUseCase,
    private val prefsRepo: UserPreferencesRepository,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<Route.NodeDetail>()
    val serverId: Long = route.serverId
    val nodeName: String = route.node

    private val _state = MutableStateFlow(NodeDetailUiState())
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

    fun setTimeframe(tf: RrdTimeframe) {
        _state.update { it.copy(timeframe = tf) }
        viewModelScope.launch {
            val rrdResult = getRrd(serverId, nodeName, tf)
            _state.update { it.copy(rrd = (rrdResult as? ApiResult.Success)?.value ?: it.rrd) }
        }
    }

    fun refresh(silent: Boolean = false) {
        _state.update {
            if (silent) it.copy(isRefreshing = true, error = null)
            else it.copy(isLoading = it.node == null, isRefreshing = true, error = null)
        }
        viewModelScope.launch {
            val nodeResult = cluster.getNode(serverId, nodeName)
            val rrdResult = getRrd(serverId, nodeName, _state.value.timeframe)
            _state.update {
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    node = (nodeResult as? ApiResult.Success)?.value ?: it.node,
                    rrd = (rrdResult as? ApiResult.Success)?.value ?: it.rrd,
                    error = (nodeResult as? ApiResult.Failure)?.error,
                )
            }
        }
    }

    fun onTabChanged() { refresh(silent = true) }

    fun nodeAction(command: String) {
        viewModelScope.launch {
            cluster.nodeAction(serverId, nodeName, command)
        }
    }
}
