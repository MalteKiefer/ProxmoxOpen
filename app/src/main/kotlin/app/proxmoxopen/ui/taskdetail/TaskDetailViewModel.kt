package app.proxmoxopen.ui.taskdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import app.proxmoxopen.domain.model.ProxmoxTask
import app.proxmoxopen.domain.model.TaskLogLine
import app.proxmoxopen.domain.model.TaskState
import app.proxmoxopen.domain.repository.TaskRepository
import app.proxmoxopen.domain.result.ApiError
import app.proxmoxopen.domain.result.ApiResult
import app.proxmoxopen.ui.nav.Route
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

    init { refresh() }

    fun refresh() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val taskResult = taskRepo.getTask(serverId, node, upid)
            val logResult = taskRepo.getTaskLog(serverId, node, upid, start = 0, limit = 1000)
            _state.update {
                it.copy(
                    isLoading = false,
                    task = (taskResult as? ApiResult.Success)?.value,
                    logLines = (logResult as? ApiResult.Success)?.value ?: emptyList(),
                    error = (taskResult as? ApiResult.Failure)?.error ?: (logResult as? ApiResult.Failure)?.error,
                )
            }
        }
    }
}
