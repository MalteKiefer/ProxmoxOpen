package app.proxmoxopen.ui.nodedetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import app.proxmoxopen.domain.model.Node
import app.proxmoxopen.domain.model.RrdPoint
import app.proxmoxopen.domain.model.RrdTimeframe
import app.proxmoxopen.domain.result.ApiError
import app.proxmoxopen.domain.result.ApiResult
import app.proxmoxopen.domain.usecase.GetNodeRrdUseCase
import app.proxmoxopen.domain.repository.ClusterRepository
import app.proxmoxopen.ui.nav.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NodeDetailUiState(
    val node: Node? = null,
    val rrd: List<RrdPoint> = emptyList(),
    val timeframe: RrdTimeframe = RrdTimeframe.HOUR,
    val isLoading: Boolean = true,
    val error: ApiError? = null,
)

@HiltViewModel
class NodeDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val cluster: ClusterRepository,
    private val getRrd: GetNodeRrdUseCase,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<Route.NodeDetail>()
    val serverId: Long = route.serverId
    val nodeName: String = route.node

    private val _state = MutableStateFlow(NodeDetailUiState())
    val state = _state.asStateFlow()

    init { refresh() }

    fun setTimeframe(tf: RrdTimeframe) {
        _state.update { it.copy(timeframe = tf) }
        refresh()
    }

    fun refresh() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val nodeResult = cluster.getNode(serverId, nodeName)
            val rrdResult = getRrd(serverId, nodeName, _state.value.timeframe)
            _state.update {
                it.copy(
                    isLoading = false,
                    node = (nodeResult as? ApiResult.Success)?.value,
                    rrd = (rrdResult as? ApiResult.Success)?.value ?: emptyList(),
                    error = (nodeResult as? ApiResult.Failure)?.error
                        ?: (rrdResult as? ApiResult.Failure)?.error,
                )
            }
        }
    }
}
