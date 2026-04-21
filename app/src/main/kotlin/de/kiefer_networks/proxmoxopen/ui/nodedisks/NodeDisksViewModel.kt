package de.kiefer_networks.proxmoxopen.ui.nodedisks

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import de.kiefer_networks.proxmoxopen.domain.model.DiskInfo
import de.kiefer_networks.proxmoxopen.domain.result.ApiError
import de.kiefer_networks.proxmoxopen.domain.result.ApiResult
import de.kiefer_networks.proxmoxopen.domain.usecase.ListNodeDisksUseCase
import de.kiefer_networks.proxmoxopen.ui.nav.Route
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NodeDisksUiState(
    val disks: List<DiskInfo> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: ApiError? = null,
)

@HiltViewModel
class NodeDisksViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val listDisks: ListNodeDisksUseCase,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<Route.NodeDisks>()
    val serverId: Long = route.serverId
    val node: String = route.node

    private val _state = MutableStateFlow(NodeDisksUiState())
    val state = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        _state.update {
            it.copy(
                isLoading = it.disks.isEmpty(),
                isRefreshing = true,
                error = null,
            )
        }
        viewModelScope.launch {
            val result = listDisks(serverId, node)
            _state.update {
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    disks = (result as? ApiResult.Success)?.value ?: it.disks,
                    error = (result as? ApiResult.Failure)?.error,
                )
            }
        }
    }
}
