package de.kiefer_networks.proxmoxopen.ui.dashboard

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import de.kiefer_networks.proxmoxopen.domain.model.BackupJob
import de.kiefer_networks.proxmoxopen.domain.model.Cluster
import de.kiefer_networks.proxmoxopen.domain.model.Guest
import de.kiefer_networks.proxmoxopen.domain.repository.BackupJobRepository
import de.kiefer_networks.proxmoxopen.domain.repository.ServerRepository
import de.kiefer_networks.proxmoxopen.domain.result.ApiError
import de.kiefer_networks.proxmoxopen.domain.result.ApiResult
import de.kiefer_networks.proxmoxopen.domain.repository.ClusterRepository
import de.kiefer_networks.proxmoxopen.domain.usecase.GetClusterUseCase
import de.kiefer_networks.proxmoxopen.domain.usecase.ListGuestsUseCase
import de.kiefer_networks.proxmoxopen.preferences.RefreshInterval
import de.kiefer_networks.proxmoxopen.preferences.UserPreferencesRepository
import de.kiefer_networks.proxmoxopen.ui.nav.Route
import timber.log.Timber
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardUiState(
    val serverName: String = "",
    val cluster: Cluster? = null,
    val guests: List<Guest> = emptyList(),
    val backupJobs: List<BackupJob> = emptyList(),
    val backupStorages: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val error: ApiError? = null,
    val searchQuery: String = "",
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getCluster: GetClusterUseCase,
    private val listGuests: ListGuestsUseCase,
    private val serverRepository: ServerRepository,
    private val clusterRepository: ClusterRepository,
    private val prefsRepo: UserPreferencesRepository,
    private val backupJobRepository: BackupJobRepository,
) : ViewModel() {

    val serverId: Long = savedStateHandle.toRoute<Route.Dashboard>().serverId

    private val _state = MutableStateFlow(DashboardUiState())
    val state = _state.asStateFlow()

    val filteredGuests: StateFlow<List<Guest>> = _state.map { state ->
        if (state.searchQuery.isBlank()) state.guests
        else state.guests.filter {
            it.name.contains(state.searchQuery, ignoreCase = true) ||
                it.vmid.toString().contains(state.searchQuery) ||
                it.node.contains(state.searchQuery, ignoreCase = true) ||
                it.tags.any { tag -> tag.contains(state.searchQuery, ignoreCase = true) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var autoRefreshJob: Job? = null

    init {
        refresh()
        startAutoRefresh()
    }

    private fun startAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = viewModelScope.launch {
            prefsRepo.preferences.collectLatest { prefs ->
                val interval = prefs.refreshInterval
                if (interval == RefreshInterval.OFF) return@collectLatest
                while (true) {
                    delay(interval.seconds * 1000L)
                    refreshSilent()
                }
            }
        }
    }

    private fun refreshSilent() {
        viewModelScope.launch {
            serverRepository.getById(serverId) ?: return@launch
            val (clusterResult, guestsResult) = coroutineScope {
                val clusterDeferred = async { getCluster(serverId) }
                val guestsDeferred = async { listGuests(serverId) }
                Pair(clusterDeferred.await(), guestsDeferred.await())
            }
            val cluster = (clusterResult as? ApiResult.Success)?.value
            val detailedNodes = coroutineScope {
                cluster?.nodes?.map { node ->
                    async {
                        val d = clusterRepository.getNode(serverId, node.name)
                        (d as? ApiResult.Success)?.value ?: node
                    }
                }?.awaitAll()
            }
            val enrichedCluster = cluster?.copy(nodes = detailedNodes ?: cluster.nodes)
            _state.update {
                it.copy(
                    cluster = enrichedCluster ?: it.cluster,
                    guests = (guestsResult as? ApiResult.Success)?.value ?: it.guests,
                )
            }
        }
    }

    fun onSearch(query: String) { _state.update { it.copy(searchQuery = query) } }

    fun refresh() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val server = serverRepository.getById(serverId)
            if (server == null) {
                _state.update {
                    it.copy(isLoading = false, error = ApiError.Unknown("server not found"))
                }
                return@launch
            }
            _state.update { it.copy(serverName = server.name) }

            val (clusterResult, guestsResult) = coroutineScope {
                val clusterDeferred = async { getCluster(serverId) }
                val guestsDeferred = async { listGuests(serverId) }
                Pair(clusterDeferred.await(), guestsDeferred.await())
            }
            // Fetch detailed status for each node (CPU model, kernel, swap, etc.)
            val cluster = (clusterResult as? ApiResult.Success)?.value
            val detailedNodes = coroutineScope {
                cluster?.nodes?.map { node ->
                    async {
                        val detail = clusterRepository.getNode(serverId, node.name)
                        (detail as? ApiResult.Success)?.value ?: node
                    }
                }?.awaitAll()
            }
            val enrichedCluster = cluster?.copy(nodes = detailedNodes ?: cluster.nodes)
            _state.update {
                it.copy(
                    isLoading = false,
                    cluster = enrichedCluster,
                    guests = (guestsResult as? ApiResult.Success)?.value ?: emptyList(),
                    error = (clusterResult as? ApiResult.Failure)?.error
                        ?: (guestsResult as? ApiResult.Failure)?.error,
                )
            }
        }
    }

    fun loadBackupJobs() {
        viewModelScope.launch {
            try {
                val jobs = backupJobRepository.listJobs(serverId)
                // Load backup storages from first available node
                val firstNode = _state.value.cluster?.nodes?.firstOrNull()?.name
                val storages = if (firstNode != null) {
                    try { backupJobRepository.listBackupStorages(serverId, firstNode) } catch (e: Exception) { Timber.d(e, "Failed to load backup storages"); emptyList() }
                } else emptyList()
                _state.update { it.copy(backupJobs = jobs, backupStorages = storages) }
            } catch (e: Exception) { Timber.d(e, "Failed to load backup jobs") }
        }
    }

    fun runBackupJob(id: String) {
        viewModelScope.launch {
            try {
                backupJobRepository.runJob(serverId, id)
                Timber.d("Backup job started")
                loadBackupJobs()
            } catch (e: Exception) {
                Timber.d(e, "Backup job action failed")
            }
        }
    }

    fun createBackupJob(params: Map<String, String>) {
        viewModelScope.launch {
            try {
                backupJobRepository.createJob(serverId, params)
                Timber.d("Backup job created")
                loadBackupJobs()
            } catch (e: Exception) {
                Timber.d(e, "Backup job action failed")
            }
        }
    }

    fun updateBackupJob(id: String, params: Map<String, String>) {
        viewModelScope.launch {
            try {
                backupJobRepository.updateJob(serverId, id, params)
                Timber.d("Backup job updated")
                loadBackupJobs()
            } catch (e: Exception) {
                Timber.d(e, "Backup job action failed")
            }
        }
    }

    fun deleteBackupJob(id: String) {
        viewModelScope.launch {
            try {
                backupJobRepository.deleteJob(serverId, id)
                Timber.d("Backup job deleted")
                loadBackupJobs()
            } catch (e: Exception) {
                Timber.d(e, "Backup job action failed")
            }
        }
    }

}
