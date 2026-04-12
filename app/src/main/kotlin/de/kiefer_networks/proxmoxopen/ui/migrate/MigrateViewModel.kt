package de.kiefer_networks.proxmoxopen.ui.migrate

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import de.kiefer_networks.proxmoxopen.domain.model.GuestType
import de.kiefer_networks.proxmoxopen.domain.repository.GuestRepository
import de.kiefer_networks.proxmoxopen.domain.result.ApiError
import de.kiefer_networks.proxmoxopen.domain.result.ApiResult
import de.kiefer_networks.proxmoxopen.ui.nav.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MigrateUiState(
    val nodes: List<String> = emptyList(),
    val selectedNode: String? = null,
    val online: Boolean = false,
    val isLoading: Boolean = true,
    val isMigrating: Boolean = false,
    val error: ApiError? = null,
    val success: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class MigrateViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val guestRepo: GuestRepository,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<Route.MigrateGuest>()
    val serverId = route.serverId
    val node = route.node
    val vmid = route.vmid
    val type = GuestType.fromApiPath(route.type) ?: GuestType.LXC

    private val _state = MutableStateFlow(MigrateUiState(
        online = type == GuestType.QEMU,
    ))
    val state = _state.asStateFlow()

    init {
        loadNodes()
    }

    private fun loadNodes() {
        viewModelScope.launch {
            when (val result = guestRepo.listNodes(serverId)) {
                is ApiResult.Success -> {
                    val otherNodes = result.value.filter { it != node }
                    _state.update {
                        it.copy(
                            nodes = otherNodes,
                            selectedNode = otherNodes.firstOrNull(),
                            isLoading = false,
                        )
                    }
                }
                is ApiResult.Failure -> {
                    _state.update { it.copy(isLoading = false, error = result.error) }
                }
            }
        }
    }

    fun selectNode(nodeName: String) {
        _state.update { it.copy(selectedNode = nodeName) }
    }

    fun setOnline(online: Boolean) {
        _state.update { it.copy(online = online) }
    }

    fun migrate() {
        val target = _state.value.selectedNode ?: return
        _state.update { it.copy(isMigrating = true, message = null) }
        viewModelScope.launch {
            when (val result = guestRepo.migrateGuest(
                serverId, node, vmid, type, target, _state.value.online,
            )) {
                is ApiResult.Success -> {
                    _state.update {
                        it.copy(
                            isMigrating = false,
                            success = true,
                            message = result.value,
                        )
                    }
                }
                is ApiResult.Failure -> {
                    _state.update {
                        it.copy(
                            isMigrating = false,
                            error = result.error,
                            message = result.error.message,
                        )
                    }
                }
            }
        }
    }
}
