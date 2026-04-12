package app.proxmoxopen.ui.guestdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import app.proxmoxopen.domain.model.ContainerStatus
import app.proxmoxopen.domain.model.GuestType
import app.proxmoxopen.domain.model.PowerAction
import app.proxmoxopen.domain.model.ProxmoxTask
import app.proxmoxopen.domain.model.RrdPoint
import app.proxmoxopen.domain.model.RrdTimeframe
import app.proxmoxopen.domain.model.Snapshot
import app.proxmoxopen.domain.repository.GuestRepository
import app.proxmoxopen.domain.repository.TaskRepository
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

data class ContainerHubUiState(
    val status: ContainerStatus? = null,
    val rrd: List<RrdPoint> = emptyList(),
    val timeframe: RrdTimeframe = RrdTimeframe.HOUR,
    val snapshots: List<Snapshot> = emptyList(),
    val tasks: List<ProxmoxTask> = emptyList(),
    val backupStorages: List<String> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: ApiError? = null,
    val actionMessage: String? = null,
)

@HiltViewModel
class ContainerHubViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val guestRepo: GuestRepository,
    private val taskRepo: TaskRepository,
    private val getRrd: GetGuestRrdUseCase,
    private val powerAction: PowerActionUseCase,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<Route.GuestDetail>()
    val serverId = route.serverId
    val node = route.node
    val vmid = route.vmid
    val type = GuestType.fromApiPath(route.type) ?: GuestType.LXC

    private val _state = MutableStateFlow(ContainerHubUiState())
    val state = _state.asStateFlow()

    init { refresh() }

    fun refresh(silent: Boolean = false) {
        _state.update {
            if (silent) it.copy(isRefreshing = true, error = null)
            else it.copy(isLoading = it.status == null, isRefreshing = true, error = null)
        }
        viewModelScope.launch {
            val statusResult = guestRepo.getContainerStatus(serverId, node, vmid)
            val rrdResult = getRrd(serverId, node, vmid, type, _state.value.timeframe)
            val snapResult = guestRepo.listSnapshots(serverId, node, vmid, type)
            val taskResult = taskRepo.listTasksForVmid(serverId, node, vmid, limit = 50)
            val storagesResult = guestRepo.listBackupStorages(serverId, node)

            _state.update {
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    status = (statusResult as? ApiResult.Success)?.value ?: it.status,
                    rrd = (rrdResult as? ApiResult.Success)?.value ?: it.rrd,
                    snapshots = (snapResult as? ApiResult.Success)?.value ?: it.snapshots,
                    tasks = (taskResult as? ApiResult.Success)?.value ?: it.tasks,
                    backupStorages = (storagesResult as? ApiResult.Success)?.value ?: it.backupStorages,
                    error = (statusResult as? ApiResult.Failure)?.error,
                )
            }
        }
    }

    fun onTabChanged() {
        refresh(silent = true)
    }

    fun setTimeframe(tf: RrdTimeframe) {
        _state.update { it.copy(timeframe = tf) }
        viewModelScope.launch {
            val rrdResult = getRrd(serverId, node, vmid, type, tf)
            _state.update { it.copy(rrd = (rrdResult as? ApiResult.Success)?.value ?: emptyList()) }
        }
    }

    fun triggerAction(action: PowerAction) {
        _state.update { it.copy(actionMessage = null) }
        viewModelScope.launch {
            when (val result = powerAction(serverId, node, vmid, type, action)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(actionMessage = "Task: ${result.value}") }
                    refresh()
                }
                is ApiResult.Failure ->
                    _state.update { it.copy(actionMessage = result.error.message) }
            }
        }
    }

    fun createSnapshot(name: String, description: String?) {
        viewModelScope.launch {
            when (val r = guestRepo.createSnapshot(serverId, node, vmid, type, name, description)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(actionMessage = "Snapshot created") }
                    refreshSnapshots()
                }
                is ApiResult.Failure -> _state.update { it.copy(actionMessage = r.error.message) }
            }
        }
    }

    fun rollbackSnapshot(name: String) {
        viewModelScope.launch {
            when (val r = guestRepo.rollbackSnapshot(serverId, node, vmid, type, name)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(actionMessage = "Rollback started") }
                    refresh()
                }
                is ApiResult.Failure -> _state.update { it.copy(actionMessage = r.error.message) }
            }
        }
    }

    fun deleteSnapshot(name: String) {
        viewModelScope.launch {
            when (val r = guestRepo.deleteSnapshot(serverId, node, vmid, type, name)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(actionMessage = "Snapshot deleted") }
                    refreshSnapshots()
                }
                is ApiResult.Failure -> _state.update { it.copy(actionMessage = r.error.message) }
            }
        }
    }

    fun createBackup(
        storage: String?,
        mode: String,
        compress: String?,
        protected: Boolean = false,
        notesTemplate: String? = null,
    ) {
        viewModelScope.launch {
            when (val r = guestRepo.createBackup(serverId, node, vmid, storage, mode, compress, protected, notesTemplate)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(actionMessage = "Backup started: ${r.value}") }
                    refresh(silent = true)
                }
                is ApiResult.Failure -> _state.update { it.copy(actionMessage = r.error.message) }
            }
        }
    }

    private suspend fun refreshSnapshots() {
        val snapResult = guestRepo.listSnapshots(serverId, node, vmid, type)
        _state.update { it.copy(snapshots = (snapResult as? ApiResult.Success)?.value ?: emptyList()) }
    }
}
