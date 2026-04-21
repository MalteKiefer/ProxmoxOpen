package de.kiefer_networks.proxmoxopen.ui.ha

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import de.kiefer_networks.proxmoxopen.domain.model.HaClusterStatus
import de.kiefer_networks.proxmoxopen.domain.model.HaGroup
import de.kiefer_networks.proxmoxopen.domain.model.HaResource
import de.kiefer_networks.proxmoxopen.domain.repository.HaRepository
import de.kiefer_networks.proxmoxopen.domain.result.ApiError
import de.kiefer_networks.proxmoxopen.domain.result.ApiResult
import de.kiefer_networks.proxmoxopen.preferences.RefreshInterval
import de.kiefer_networks.proxmoxopen.preferences.UserPreferencesRepository
import de.kiefer_networks.proxmoxopen.ui.nav.Route
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HaUiState(
    val status: HaClusterStatus? = null,
    val resources: List<HaResource> = emptyList(),
    val groups: List<HaGroup> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: ApiError? = null,
)

@HiltViewModel
class HaViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val haRepository: HaRepository,
    private val prefsRepo: UserPreferencesRepository,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<Route.Ha>()
    val serverId: Long = route.serverId

    private val _state = MutableStateFlow(HaUiState())
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
            else it.copy(
                isLoading = it.status == null && it.resources.isEmpty() && it.groups.isEmpty(),
                isRefreshing = true,
                error = null,
            )
        }
        viewModelScope.launch {
            val (statusResult, resourcesResult, groupsResult) = coroutineScope {
                val s = async { haRepository.getStatus(serverId) }
                val r = async { haRepository.getResources(serverId) }
                val g = async { haRepository.getGroups(serverId) }
                Triple(s.await(), r.await(), g.await())
            }
            _state.update {
                val firstError = listOf(statusResult, resourcesResult, groupsResult)
                    .filterIsInstance<ApiResult.Failure>()
                    .firstOrNull()
                    ?.error
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    status = (statusResult as? ApiResult.Success)?.value ?: it.status,
                    resources = (resourcesResult as? ApiResult.Success)?.value ?: it.resources,
                    groups = (groupsResult as? ApiResult.Success)?.value ?: it.groups,
                    error = firstError,
                )
            }
        }
    }
}
