package app.proxmoxopen.ui.clone

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import app.proxmoxopen.domain.model.GuestType
import app.proxmoxopen.domain.repository.GuestRepository
import app.proxmoxopen.domain.result.ApiError
import app.proxmoxopen.domain.result.ApiResult
import app.proxmoxopen.ui.nav.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CloneUiState(
    val nodes: List<String> = emptyList(),
    val storages: List<String> = emptyList(),
    val newId: String = "",
    val name: String = "",
    val fullClone: Boolean = true,
    val targetNode: String? = null,
    val targetStorage: String? = null,
    val isLoading: Boolean = true,
    val isCloning: Boolean = false,
    val error: ApiError? = null,
    val success: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class CloneViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val guestRepo: GuestRepository,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<Route.CloneGuest>()
    val serverId = route.serverId
    val node = route.node
    val vmid = route.vmid
    val type = GuestType.fromApiPath(route.type) ?: GuestType.LXC

    private val _state = MutableStateFlow(CloneUiState())
    val state = _state.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val nodesResult = guestRepo.listNodes(serverId)
            val storagesResult = guestRepo.listStorages(serverId, node)
            _state.update {
                it.copy(
                    nodes = (nodesResult as? ApiResult.Success)?.value ?: emptyList(),
                    storages = (storagesResult as? ApiResult.Success)?.value ?: emptyList(),
                    isLoading = false,
                    error = (nodesResult as? ApiResult.Failure)?.error,
                )
            }
        }
    }

    fun setNewId(value: String) {
        _state.update { it.copy(newId = value) }
    }

    fun setName(value: String) {
        _state.update { it.copy(name = value) }
    }

    fun setFullClone(value: Boolean) {
        _state.update { it.copy(fullClone = value) }
    }

    fun setTargetNode(value: String?) {
        _state.update { it.copy(targetNode = value) }
        // Reload storages for selected target node
        value?.let { targetNode ->
            viewModelScope.launch {
                val result = guestRepo.listStorages(serverId, targetNode)
                _state.update {
                    it.copy(storages = (result as? ApiResult.Success)?.value ?: it.storages)
                }
            }
        }
    }

    fun setTargetStorage(value: String?) {
        _state.update { it.copy(targetStorage = value) }
    }

    fun clone() {
        val newid = _state.value.newId.toIntOrNull() ?: return
        _state.update { it.copy(isCloning = true, message = null) }
        viewModelScope.launch {
            when (val result = guestRepo.cloneGuest(
                serverId = serverId,
                node = node,
                vmid = vmid,
                type = type,
                newid = newid,
                name = _state.value.name.ifBlank { null },
                full = _state.value.fullClone,
                target = _state.value.targetNode,
                storage = _state.value.targetStorage,
            )) {
                is ApiResult.Success -> {
                    _state.update {
                        it.copy(
                            isCloning = false,
                            success = true,
                            message = result.value,
                        )
                    }
                }
                is ApiResult.Failure -> {
                    _state.update {
                        it.copy(
                            isCloning = false,
                            error = result.error,
                            message = result.error.message,
                        )
                    }
                }
            }
        }
    }
}
