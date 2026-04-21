package de.kiefer_networks.proxmoxopen.ui.nodedisks

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import de.kiefer_networks.proxmoxopen.domain.model.SmartReport
import de.kiefer_networks.proxmoxopen.domain.result.ApiError
import de.kiefer_networks.proxmoxopen.domain.result.ApiResult
import de.kiefer_networks.proxmoxopen.domain.usecase.GetDiskSmartUseCase
import de.kiefer_networks.proxmoxopen.ui.nav.Route
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DiskSmartUiState(
    val report: SmartReport? = null,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: ApiError? = null,
)

@HiltViewModel
class DiskSmartViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getSmart: GetDiskSmartUseCase,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<Route.DiskSmart>()
    val serverId: Long = route.serverId
    val node: String = route.node
    val disk: String = route.disk

    private val _state = MutableStateFlow(DiskSmartUiState())
    val state = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        _state.update { it.copy(isRefreshing = true, error = null) }
        viewModelScope.launch {
            val result = getSmart(serverId, node, disk)
            _state.update {
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    report = (result as? ApiResult.Success)?.value ?: it.report,
                    error = (result as? ApiResult.Failure)?.error,
                )
            }
        }
    }
}
