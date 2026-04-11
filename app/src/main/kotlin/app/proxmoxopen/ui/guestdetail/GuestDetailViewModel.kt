package app.proxmoxopen.ui.guestdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import app.proxmoxopen.domain.model.Guest
import app.proxmoxopen.domain.model.GuestType
import app.proxmoxopen.domain.model.PowerAction
import app.proxmoxopen.domain.model.RrdPoint
import app.proxmoxopen.domain.model.RrdTimeframe
import app.proxmoxopen.domain.repository.GuestRepository
import app.proxmoxopen.domain.result.ApiError
import app.proxmoxopen.domain.result.ApiResult
import app.proxmoxopen.domain.usecase.GetGuestRrdUseCase
import app.proxmoxopen.domain.usecase.PowerActionUseCase
import app.proxmoxopen.ui.nav.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GuestDetailUiState(
    val guest: Guest? = null,
    val rrd: List<RrdPoint> = emptyList(),
    val timeframe: RrdTimeframe = RrdTimeframe.HOUR,
    val isLoading: Boolean = true,
    val error: ApiError? = null,
    val actionRunning: PowerAction? = null,
    val actionMessage: String? = null,
)

@HiltViewModel
class GuestDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val guestRepo: GuestRepository,
    private val getRrd: GetGuestRrdUseCase,
    private val powerAction: PowerActionUseCase,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<Route.GuestDetail>()
    val serverId: Long = route.serverId
    val node: String = route.node
    val vmid: Int = route.vmid
    val type: GuestType = GuestType.fromApiPath(route.type) ?: GuestType.QEMU

    private val _state = MutableStateFlow(GuestDetailUiState())
    val state = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val guestResult = guestRepo.getGuest(serverId, node, vmid, type)
            val rrdResult = getRrd(serverId, node, vmid, type, _state.value.timeframe)
            _state.update {
                it.copy(
                    isLoading = false,
                    guest = (guestResult as? ApiResult.Success)?.value,
                    rrd = (rrdResult as? ApiResult.Success)?.value ?: emptyList(),
                    error = (guestResult as? ApiResult.Failure)?.error
                        ?: (rrdResult as? ApiResult.Failure)?.error,
                )
            }
        }
    }

    fun triggerAction(action: PowerAction) {
        _state.update { it.copy(actionRunning = action, actionMessage = null) }
        viewModelScope.launch {
            when (val result = powerAction(serverId, node, vmid, type, action)) {
                is ApiResult.Success ->
                    _state.update {
                        it.copy(actionRunning = null, actionMessage = "OK: ${result.value}")
                    }
                is ApiResult.Failure ->
                    _state.update {
                        it.copy(actionRunning = null, actionMessage = result.error.message)
                    }
            }
            refresh()
        }
    }
}
