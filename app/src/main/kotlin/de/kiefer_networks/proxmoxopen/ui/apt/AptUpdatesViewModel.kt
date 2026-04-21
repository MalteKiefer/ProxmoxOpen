package de.kiefer_networks.proxmoxopen.ui.apt

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import de.kiefer_networks.proxmoxopen.domain.model.AptUpdate
import de.kiefer_networks.proxmoxopen.domain.repository.AptRepository
import de.kiefer_networks.proxmoxopen.domain.result.ApiError
import de.kiefer_networks.proxmoxopen.domain.result.ApiResult
import de.kiefer_networks.proxmoxopen.ui.nav.Route
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AptUpdatesUiState(
    val updates: List<AptUpdate> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isUpgrading: Boolean = false,
    val error: ApiError? = null,
    /** Filled after a successful upgrade POST so the screen can navigate to TaskDetail. */
    val pendingUpgradeUpid: String? = null,
)

@HiltViewModel
class AptUpdatesViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val aptRepo: AptRepository,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<Route.AptUpdates>()
    val serverId: Long = route.serverId
    val node: String = route.node

    private val _state = MutableStateFlow(AptUpdatesUiState())
    val state = _state.asStateFlow()

    init { load() }

    /** Fetch the current pending-updates list. */
    fun load(silent: Boolean = false) {
        _state.update {
            if (silent) it.copy(isRefreshing = true, error = null)
            else it.copy(isLoading = it.updates.isEmpty(), isRefreshing = true, error = null)
        }
        viewModelScope.launch {
            val result = aptRepo.listUpdates(serverId, node)
            _state.update {
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    updates = (result as? ApiResult.Success)?.value ?: it.updates,
                    error = (result as? ApiResult.Failure)?.error,
                )
            }
        }
    }

    /** Refresh the APT cache on the node (POST /apt/update), then re-fetch the list. */
    fun refreshCache() {
        _state.update { it.copy(isRefreshing = true, error = null) }
        viewModelScope.launch {
            when (val result = aptRepo.refresh(serverId, node)) {
                is ApiResult.Success -> load(silent = true)
                is ApiResult.Failure -> _state.update {
                    it.copy(isRefreshing = false, error = result.error)
                }
            }
        }
    }

    /** Trigger an upgrade of all pending packages. On success, UPID is exposed for navigation. */
    fun upgradeAll() {
        if (_state.value.isUpgrading) return
        _state.update { it.copy(isUpgrading = true, error = null) }
        viewModelScope.launch {
            val result = aptRepo.upgradeAll(serverId, node)
            _state.update {
                when (result) {
                    is ApiResult.Success -> it.copy(
                        isUpgrading = false,
                        pendingUpgradeUpid = result.value,
                    )
                    is ApiResult.Failure -> it.copy(
                        isUpgrading = false,
                        error = result.error,
                    )
                }
            }
        }
    }

    /** Called by the screen after navigation has been dispatched, to clear the one-shot UPID. */
    fun onUpgradeNavigated() {
        _state.update { it.copy(pendingUpgradeUpid = null) }
    }
}
