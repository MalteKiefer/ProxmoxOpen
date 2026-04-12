package app.proxmoxopen.ui.guestdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import app.proxmoxopen.domain.model.Backup
import app.proxmoxopen.domain.model.GuestType
import app.proxmoxopen.domain.model.PowerAction
import app.proxmoxopen.domain.model.ProxmoxTask
import app.proxmoxopen.domain.model.RrdPoint
import app.proxmoxopen.domain.model.RrdTimeframe
import app.proxmoxopen.domain.model.Snapshot
import app.proxmoxopen.domain.model.VmConfig
import app.proxmoxopen.domain.model.VmStatus
import app.proxmoxopen.domain.repository.GuestRepository
import app.proxmoxopen.domain.repository.TaskRepository
import app.proxmoxopen.domain.result.ApiError
import app.proxmoxopen.domain.result.ApiResult
import app.proxmoxopen.domain.usecase.GetGuestRrdUseCase
import app.proxmoxopen.domain.usecase.PowerActionUseCase
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

data class VmHubUiState(
    val status: VmStatus? = null,
    val config: VmConfig? = null,
    val rrd: List<RrdPoint> = emptyList(),
    val timeframe: RrdTimeframe = RrdTimeframe.HOUR,
    val snapshots: List<Snapshot> = emptyList(),
    val tasks: List<ProxmoxTask> = emptyList(),
    val backupStorages: List<String> = emptyList(),
    val backups: List<Backup> = emptyList(),
    val selectedBackupStorage: String? = null,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: ApiError? = null,
    val actionMessage: String? = null,
)

@HiltViewModel
class VmHubViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val guestRepo: GuestRepository,
    private val taskRepo: TaskRepository,
    private val getRrd: GetGuestRrdUseCase,
    private val powerAction: PowerActionUseCase,
    private val prefsRepo: UserPreferencesRepository,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<Route.GuestDetail>()
    val serverId = route.serverId
    val node = route.node
    val vmid = route.vmid
    val type = GuestType.QEMU

    private val _state = MutableStateFlow(VmHubUiState())
    val state = _state.asStateFlow()
    private var autoRefreshJob: Job? = null

    init { refresh(); startAutoRefresh() }

    private fun startAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = viewModelScope.launch {
            prefsRepo.preferences.collectLatest { prefs ->
                if (prefs.refreshInterval == RefreshInterval.OFF) return@collectLatest
                while (true) { delay(prefs.refreshInterval.seconds * 1000L); refresh(silent = true) }
            }
        }
    }

    fun refresh(silent: Boolean = false) {
        _state.update {
            if (silent) it.copy(isRefreshing = true, error = null)
            else it.copy(isLoading = it.status == null, isRefreshing = true, error = null)
        }
        viewModelScope.launch {
            val statusResult = guestRepo.getVmStatus(serverId, node, vmid)
            val configResult = guestRepo.getVmConfig(serverId, node, vmid)
            val rrdResult = getRrd(serverId, node, vmid, type, _state.value.timeframe)
            val snapResult = guestRepo.listSnapshots(serverId, node, vmid, type)
            val taskResult = taskRepo.listTasksForVmid(serverId, node, vmid, limit = 50)
            val storagesResult = guestRepo.listBackupStorages(serverId, node)

            val storages = (storagesResult as? ApiResult.Success)?.value ?: _state.value.backupStorages
            val selectedStorage = _state.value.selectedBackupStorage ?: storages.firstOrNull()
            val backupsResult = selectedStorage?.let { guestRepo.listBackups(serverId, node, it, vmid) }

            _state.update {
                it.copy(
                    isLoading = false, isRefreshing = false,
                    status = (statusResult as? ApiResult.Success)?.value ?: it.status,
                    config = (configResult as? ApiResult.Success)?.value ?: it.config,
                    rrd = (rrdResult as? ApiResult.Success)?.value ?: it.rrd,
                    snapshots = (snapResult as? ApiResult.Success)?.value ?: it.snapshots,
                    tasks = (taskResult as? ApiResult.Success)?.value ?: it.tasks,
                    backupStorages = storages,
                    selectedBackupStorage = selectedStorage,
                    backups = (backupsResult as? ApiResult.Success)?.value ?: it.backups,
                    error = (statusResult as? ApiResult.Failure)?.error,
                )
            }
        }
    }

    fun onTabChanged() { refresh(silent = true) }

    fun setTimeframe(tf: RrdTimeframe) {
        _state.update { it.copy(timeframe = tf) }
        viewModelScope.launch {
            val r = getRrd(serverId, node, vmid, type, tf)
            _state.update { it.copy(rrd = (r as? ApiResult.Success)?.value ?: it.rrd) }
        }
    }

    fun triggerAction(action: PowerAction) {
        viewModelScope.launch {
            when (val r = powerAction(serverId, node, vmid, type, action)) {
                is ApiResult.Success -> { _state.update { it.copy(actionMessage = "Task: ${r.value}") }; refresh(silent = true) }
                is ApiResult.Failure -> _state.update { it.copy(actionMessage = r.error.message) }
            }
        }
    }

    fun createSnapshot(name: String, description: String?) {
        viewModelScope.launch {
            when (val r = guestRepo.createSnapshot(serverId, node, vmid, type, name, description)) {
                is ApiResult.Success -> { _state.update { it.copy(actionMessage = "Snapshot created") }; refresh(silent = true) }
                is ApiResult.Failure -> _state.update { it.copy(actionMessage = r.error.message) }
            }
        }
    }

    fun rollbackSnapshot(name: String) { viewModelScope.launch { guestRepo.rollbackSnapshot(serverId, node, vmid, type, name); refresh(silent = true) } }
    fun deleteSnapshot(name: String) { viewModelScope.launch { guestRepo.deleteSnapshot(serverId, node, vmid, type, name); refresh(silent = true) } }
    fun selectBackupStorage(s: String) { _state.update { it.copy(selectedBackupStorage = s) }; viewModelScope.launch { val r = guestRepo.listBackups(serverId, node, s, vmid); _state.update { it.copy(backups = (r as? ApiResult.Success)?.value ?: emptyList()) } } }
    fun createBackup(storage: String?, mode: String, compress: String?, prot: Boolean, notes: String?) { viewModelScope.launch { guestRepo.createBackup(serverId, node, vmid, storage, mode, compress, prot, notes); refresh(silent = true) } }
    fun restoreBackup(volid: String) { viewModelScope.launch { guestRepo.restoreBackup(serverId, node, vmid, volid, null); refresh(silent = true) } }
}
