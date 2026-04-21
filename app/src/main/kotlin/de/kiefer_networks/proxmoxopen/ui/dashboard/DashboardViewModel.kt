package de.kiefer_networks.proxmoxopen.ui.dashboard

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import de.kiefer_networks.proxmoxopen.domain.model.BackupJob
import de.kiefer_networks.proxmoxopen.domain.model.Cluster
import de.kiefer_networks.proxmoxopen.domain.model.Guest
import de.kiefer_networks.proxmoxopen.domain.model.GuestStatus
import de.kiefer_networks.proxmoxopen.domain.model.GuestType
import de.kiefer_networks.proxmoxopen.domain.repository.BackupJobRepository
import de.kiefer_networks.proxmoxopen.domain.repository.ClusterCacheRepository
import de.kiefer_networks.proxmoxopen.domain.repository.ClusterResourceSnapshot
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
import kotlinx.coroutines.flow.first
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
    /**
     * Epoch-millis timestamp of the cached snapshot currently being displayed, or
     * `null` when the UI is showing live data. The dashboard renders an "Offline —
     * cached HH:mm" banner whenever this is set.
     */
    val fromCacheCapturedAt: Long? = null,
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
    private val clusterCache: ClusterCacheRepository,
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
            val liveGuests = (guestsResult as? ApiResult.Success)?.value
            if (liveGuests != null) {
                persistCacheIfEnabled(liveGuests)
            }
            _state.update {
                it.copy(
                    cluster = enrichedCluster ?: it.cluster,
                    guests = liveGuests ?: it.guests,
                    // A successful silent refresh clears the cache banner; if only the
                    // network call failed we leave the previous banner state alone.
                    fromCacheCapturedAt = if (liveGuests != null) null else it.fromCacheCapturedAt,
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

            // Success path: persist the fresh guest list so offline fallback has data.
            val liveGuests = (guestsResult as? ApiResult.Success)?.value
            if (liveGuests != null) {
                persistCacheIfEnabled(liveGuests)
            }

            // Offline fallback: when the guest list failed with a network error and the
            // user has opted-in to the cache, serve the last-known snapshot.
            val guestsError = (guestsResult as? ApiResult.Failure)?.error
            val (fallbackGuests, capturedAt) = if (liveGuests == null && guestsError is ApiError.Network) {
                loadCacheIfEnabled()
            } else {
                null to null
            }

            _state.update {
                it.copy(
                    isLoading = false,
                    cluster = enrichedCluster ?: it.cluster,
                    guests = liveGuests ?: fallbackGuests ?: emptyList(),
                    error = (clusterResult as? ApiResult.Failure)?.error
                        ?: guestsError,
                    fromCacheCapturedAt = capturedAt,
                )
            }
        }
    }

    /**
     * Saves the live guest list into the offline cache, unless the user disabled the
     * feature in settings. Failures are swallowed — caching must never break the UI.
     */
    private suspend fun persistCacheIfEnabled(guests: List<Guest>) {
        try {
            val enabled = prefsRepo.preferences.first().offlineCacheEnabled
            if (!enabled) return
            clusterCache.save(serverId, guests.map { it.toSnapshot() })
        } catch (e: Exception) {
            Timber.d(e, "Failed to persist cluster cache")
        }
    }

    /**
     * Reads the last-known cached snapshot, returning the reconstructed guest list and
     * its capture timestamp. Returns `(null, null)` when the feature is disabled, the
     * cache is empty, or an error occurs reading the DB.
     */
    private suspend fun loadCacheIfEnabled(): Pair<List<Guest>?, Long?> = try {
        val enabled = prefsRepo.preferences.first().offlineCacheEnabled
        if (!enabled) {
            null to null
        } else {
            val snap = clusterCache.snapshot(serverId)
            if (snap == null) {
                null to null
            } else {
                snap.resources.mapNotNull { it.toGuestOrNull() } to snap.capturedAt
            }
        }
    } catch (e: Exception) {
        Timber.d(e, "Failed to read cluster cache")
        null to null
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

// ---------------------------------------------------------------------------
// Guest <-> ClusterResourceSnapshot mapping
// ---------------------------------------------------------------------------

private fun Guest.toSnapshot(): ClusterResourceSnapshot = ClusterResourceSnapshot(
    type = type.apiPath,
    node = node,
    vmid = vmid,
    name = name,
    status = when (status) {
        GuestStatus.RUNNING -> "running"
        GuestStatus.STOPPED -> "stopped"
        GuestStatus.PAUSED -> "paused"
        GuestStatus.SUSPENDED -> "suspended"
        GuestStatus.UNKNOWN -> null
    },
    cpu = cpuUsage,
    mem = memUsed,
    maxmem = memTotal,
    diskUsed = diskUsed,
    maxdisk = diskTotal,
    tags = tags.takeIf { it.isNotEmpty() }?.joinToString(","),
)

private fun ClusterResourceSnapshot.toGuestOrNull(): Guest? {
    val vm = vmid ?: return null
    val typeEnum = GuestType.fromApiPath(type) ?: return null
    return Guest(
        vmid = vm,
        name = name ?: "vm-$vm",
        node = node,
        type = typeEnum,
        status = GuestStatus.fromProxmox(status),
        cpuUsage = cpu ?: 0.0,
        cpuCount = 0,
        memUsed = mem ?: 0,
        memTotal = maxmem ?: 0,
        diskUsed = diskUsed ?: 0,
        diskTotal = maxdisk ?: 0,
        uptimeSeconds = 0,
        tags = tags?.split(';', ',')?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList(),
    )
}
