package de.kiefer_networks.proxmoxopen.ui.tasklog

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import de.kiefer_networks.proxmoxopen.domain.model.ProxmoxTask
import de.kiefer_networks.proxmoxopen.domain.model.TaskLogLine
import de.kiefer_networks.proxmoxopen.domain.repository.TaskRepository
import de.kiefer_networks.proxmoxopen.domain.result.ApiError
import de.kiefer_networks.proxmoxopen.domain.result.ApiResult
import de.kiefer_networks.proxmoxopen.ui.nav.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TaskDetailUiState(
    val task: ProxmoxTask? = null,
    val logLines: List<TaskLogLine> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: ApiError? = null,
)

@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val taskRepo: TaskRepository,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<Route.TaskDetail>()
    val serverId = route.serverId
    val node = route.node
    val upid = route.upid

    private val _state = MutableStateFlow(TaskDetailUiState())
    val state = _state.asStateFlow()

    init {
        load()
    }

    fun refresh() {
        _state.update { it.copy(isRefreshing = true) }
        load()
    }

    private fun load() {
        viewModelScope.launch {
            // Fetch task status
            when (val result = taskRepo.getTask(serverId, node, upid)) {
                is ApiResult.Success -> _state.update { it.copy(task = result.value) }
                is ApiResult.Failure -> _state.update { it.copy(error = result.error) }
            }

            // Fetch task log
            when (val result = taskRepo.getTaskLog(serverId, node, upid)) {
                is ApiResult.Success -> _state.update {
                    it.copy(
                        logLines = result.value,
                        isLoading = false,
                        isRefreshing = false,
                        error = null,
                    )
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(isLoading = false, isRefreshing = false, error = result.error)
                }
            }
        }
    }
}
