package de.kiefer_networks.proxmoxopen.ui.guestdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import de.kiefer_networks.proxmoxopen.domain.model.Backup
import de.kiefer_networks.proxmoxopen.domain.model.ContainerStatus
import de.kiefer_networks.proxmoxopen.domain.model.GuestType
import de.kiefer_networks.proxmoxopen.domain.model.PowerAction
import de.kiefer_networks.proxmoxopen.domain.model.ProxmoxTask
import de.kiefer_networks.proxmoxopen.domain.model.RrdPoint
import de.kiefer_networks.proxmoxopen.domain.model.RrdTimeframe
import de.kiefer_networks.proxmoxopen.domain.model.Snapshot
import de.kiefer_networks.proxmoxopen.domain.model.VmConfig
import de.kiefer_networks.proxmoxopen.domain.model.VmStatus
import de.kiefer_networks.proxmoxopen.domain.repository.GuestRepository
import de.kiefer_networks.proxmoxopen.domain.repository.TaskRepository
import de.kiefer_networks.proxmoxopen.domain.result.ApiError
import de.kiefer_networks.proxmoxopen.domain.result.ApiResult
import de.kiefer_networks.proxmoxopen.domain.usecase.GetGuestRrdUseCase
import de.kiefer_networks.proxmoxopen.domain.usecase.PowerActionUseCase
import de.kiefer_networks.proxmoxopen.preferences.RefreshInterval
import de.kiefer_networks.proxmoxopen.preferences.UserPreferencesRepository
import de.kiefer_networks.proxmoxopen.ui.nav.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GuestHubUiState(
    // Container status (CT only)
    val containerStatus: ContainerStatus? = null,
    // VM status (VM only)
    val vmStatus: VmStatus? = null,
    // VM config (VM only)
    val vmConfig: VmConfig? = null,
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
) {
    /** True when either status has been loaded. */
    val hasStatus: Boolean get() = containerStatus != null || vmStatus != null
}

@HiltViewModel
class GuestHubViewModel @Inject constructor(
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
    val type = GuestType.fromApiPath(route.type) ?: GuestType.LXC
    val isVm = type == GuestType.QEMU

    private val _state = MutableStateFlow(GuestHubUiState())
    val state = _state.asStateFlow()
    private var autoRefreshJob: Job? = null

    private var currentTab = 0

    init { loadTab(0); startAutoRefresh() }

    private fun startAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = viewModelScope.launch {
            prefsRepo.preferences.collectLatest { prefs ->
                if (prefs.refreshInterval == RefreshInterval.OFF) return@collectLatest
                while (true) { delay(prefs.refreshInterval.seconds * 1000L); loadTab(currentTab, silent = true) }
            }
        }
    }

    fun refresh(silent: Boolean = false) { loadTab(currentTab, silent) }

    fun loadTab(tab: Int, silent: Boolean = false) {
        currentTab = tab
        _state.update {
            if (silent) it.copy(isRefreshing = true, error = null)
            else it.copy(isLoading = !it.hasStatus, isRefreshing = true, error = null)
        }
        viewModelScope.launch {
            loadStatus()

            when (tab) {
                0 -> loadSummaryExtras()
                1 -> loadRrd()
                2 -> loadSnapshots()
                3 -> loadBackups()
                4 -> loadTasks()
            }

            _state.update { it.copy(isLoading = false, isRefreshing = false) }
        }
    }

    // ── Status loading ──────────────────────────────────────────────────

    private suspend fun loadStatus() {
        if (isVm) {
            val statusResult = guestRepo.getVmStatus(serverId, node, vmid)
            val configResult = if (_state.value.vmConfig == null) guestRepo.getVmConfig(serverId, node, vmid) else null
            _state.update {
                it.copy(
                    vmStatus = (statusResult as? ApiResult.Success)?.value ?: it.vmStatus,
                    vmConfig = (configResult as? ApiResult.Success)?.value ?: it.vmConfig,
                    error = (statusResult as? ApiResult.Failure)?.error,
                )
            }
        } else {
            val statusResult = guestRepo.getContainerStatus(serverId, node, vmid)
            _state.update {
                it.copy(
                    containerStatus = (statusResult as? ApiResult.Success)?.value ?: it.containerStatus,
                    error = (statusResult as? ApiResult.Failure)?.error,
                )
            }
        }
    }

    /** VM-only: fetch QEMU agent IPs when summary tab is selected. */
    private fun loadSummaryExtras() {
        if (!isVm) return
        val vmStatus = _state.value.vmStatus ?: return
        if (vmStatus.agentEnabled && vmStatus.ipAddresses.isEmpty()) {
            viewModelScope.launch {
                val ips = guestRepo.getVmAgentIps(serverId, node, vmid)
                if (ips is ApiResult.Success && ips.value.isNotEmpty()) {
                    _state.update { s ->
                        s.copy(vmStatus = s.vmStatus?.copy(ipAddresses = ips.value))
                    }
                }
            }
        }
    }

    // ── Tab data loading (shared) ───────────────────────────────────────

    private suspend fun loadRrd() {
        val rrdResult = getRrd(serverId, node, vmid, type, _state.value.timeframe)
        _state.update { it.copy(rrd = (rrdResult as? ApiResult.Success)?.value ?: it.rrd) }
    }

    private suspend fun loadSnapshots() {
        val snapResult = guestRepo.listSnapshots(serverId, node, vmid, type)
        val snaps = (snapResult as? ApiResult.Success)?.value?.filter { it.name != "current" }
        _state.update { it.copy(snapshots = snaps ?: it.snapshots) }
    }

    private suspend fun loadBackups() {
        val storages = (guestRepo.listBackupStorages(serverId, node) as? ApiResult.Success)?.value ?: _state.value.backupStorages
        val sel = _state.value.selectedBackupStorage ?: storages.firstOrNull()
        val backups = sel?.let { (guestRepo.listBackups(serverId, node, it, vmid) as? ApiResult.Success)?.value } ?: emptyList()
        _state.update { it.copy(backupStorages = storages, selectedBackupStorage = sel, backups = backups) }
    }

    private suspend fun loadTasks() {
        val taskResult = taskRepo.listTasksForVmid(serverId, node, vmid, limit = 50)
        _state.update { it.copy(tasks = (taskResult as? ApiResult.Success)?.value ?: it.tasks) }
    }

    // ── Actions (shared) ────────────────────────────────────────────────

    fun setTimeframe(tf: RrdTimeframe) {
        _state.update { it.copy(timeframe = tf) }
        viewModelScope.launch {
            val r = getRrd(serverId, node, vmid, type, tf)
            _state.update { it.copy(rrd = (r as? ApiResult.Success)?.value ?: emptyList()) }
        }
    }

    fun triggerAction(action: PowerAction) {
        _state.update { it.copy(actionMessage = null) }
        viewModelScope.launch {
            when (val r = powerAction(serverId, node, vmid, type, action)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(actionMessage = "Task: ${r.value}") }
                    refresh(silent = true)
                }
                is ApiResult.Failure -> _state.update { it.copy(actionMessage = r.error.message) }
            }
        }
    }

    fun createSnapshot(name: String, description: String?) {
        viewModelScope.launch {
            when (val r = guestRepo.createSnapshot(serverId, node, vmid, type, name, description)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(actionMessage = "Snapshot created") }
                    loadSnapshots()
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
                    refresh(silent = true)
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
                    loadSnapshots()
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

    fun selectBackupStorage(storage: String) {
        _state.update { it.copy(selectedBackupStorage = storage) }
        viewModelScope.launch {
            val result = guestRepo.listBackups(serverId, node, storage, vmid)
            _state.update { it.copy(backups = (result as? ApiResult.Success)?.value ?: emptyList()) }
        }
    }

    fun restoreBackup(volid: String) {
        viewModelScope.launch {
            when (val r = guestRepo.restoreBackup(serverId, node, vmid, volid, null)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(actionMessage = "Restore started: ${r.value}") }
                    refresh(silent = true)
                }
                is ApiResult.Failure -> _state.update { it.copy(actionMessage = r.error.message) }
            }
        }
    }

    fun deleteGuest(purge: Boolean, destroyDisks: Boolean, onSuccess: () -> Unit) {
        viewModelScope.launch {
            when (val r = guestRepo.deleteGuest(serverId, node, vmid, type, purge, destroyDisks)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(actionMessage = "Delete started: ${r.value}") }
                    onSuccess()
                }
                is ApiResult.Failure -> _state.update { it.copy(actionMessage = r.error.message) }
            }
        }
    }
}
