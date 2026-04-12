package app.proxmoxopen.ui.storage

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import app.proxmoxopen.domain.model.StorageInfo
import app.proxmoxopen.domain.result.ApiError
import app.proxmoxopen.domain.result.ApiResult
import app.proxmoxopen.domain.usecase.ListStoragesUseCase
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

data class StorageUiState(
    val storages: List<StorageInfo> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: ApiError? = null,
)

@HiltViewModel
class StorageViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val listStorages: ListStoragesUseCase,
    private val prefsRepo: UserPreferencesRepository,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<Route.StorageOverview>()
    val serverId: Long = route.serverId
    val node: String = route.node

    private val _state = MutableStateFlow(StorageUiState())
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
            else it.copy(isLoading = it.storages.isEmpty(), isRefreshing = true, error = null)
        }
        viewModelScope.launch {
            val result = listStorages(serverId, node)
            _state.update {
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    storages = (result as? ApiResult.Success)?.value ?: it.storages,
                    error = (result as? ApiResult.Failure)?.error,
                )
            }
        }
    }
}
