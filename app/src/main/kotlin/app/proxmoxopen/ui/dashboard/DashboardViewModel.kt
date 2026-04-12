package app.proxmoxopen.ui.dashboard

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import app.proxmoxopen.domain.model.Cluster
import app.proxmoxopen.domain.model.Guest
import app.proxmoxopen.domain.repository.ServerRepository
import app.proxmoxopen.domain.result.ApiError
import app.proxmoxopen.domain.result.ApiResult
import app.proxmoxopen.domain.usecase.GetClusterUseCase
import app.proxmoxopen.domain.usecase.ListGuestsUseCase
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

data class DashboardUiState(
    val serverName: String = "",
    val cluster: Cluster? = null,
    val guests: List<Guest> = emptyList(),
    val isLoading: Boolean = false,
    val error: ApiError? = null,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getCluster: GetClusterUseCase,
    private val listGuests: ListGuestsUseCase,
    private val serverRepository: ServerRepository,
    private val prefsRepo: UserPreferencesRepository,
) : ViewModel() {

    val serverId: Long = savedStateHandle.toRoute<Route.Dashboard>().serverId

    private val _state = MutableStateFlow(DashboardUiState())
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
                val interval = prefs.refreshInterval
                if (interval == RefreshInterval.OFF) return@collectLatest
                while (true) {
                    delay(interval.seconds * 1000L)
                    refreshSilent()
                }
            }
        }
    }

    private fun refreshSilent() {
        viewModelScope.launch {
            val server = serverRepository.getById(serverId) ?: return@launch
            val clusterResult = getCluster(serverId)
            val guestsResult = listGuests(serverId)
            _state.update {
                it.copy(
                    cluster = (clusterResult as? ApiResult.Success)?.value ?: it.cluster,
                    guests = (guestsResult as? ApiResult.Success)?.value ?: it.guests,
                )
            }
        }
    }

    fun refresh() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val server = serverRepository.getById(serverId)
            if (server == null) {
                _state.update {
                    it.copy(isLoading = false, error = ApiError.Unknown("server not found"))
                }
                return@launch
            }
            _state.update { it.copy(serverName = server.name) }

            val clusterResult = getCluster(serverId)
            val guestsResult = listGuests(serverId)
            _state.update {
                it.copy(
                    isLoading = false,
                    cluster = (clusterResult as? ApiResult.Success)?.value,
                    guests = (guestsResult as? ApiResult.Success)?.value ?: emptyList(),
                    error = (clusterResult as? ApiResult.Failure)?.error
                        ?: (guestsResult as? ApiResult.Failure)?.error,
                )
            }
        }
    }
}
