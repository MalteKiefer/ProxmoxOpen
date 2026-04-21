package de.kiefer_networks.proxmoxopen.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Checkbox
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.kiefer_networks.proxmoxopen.R
import de.kiefer_networks.proxmoxopen.core.ui.component.ErrorState
import de.kiefer_networks.proxmoxopen.core.ui.component.LoadingState
import de.kiefer_networks.proxmoxopen.core.ui.theme.ResourceCpu
import de.kiefer_networks.proxmoxopen.core.ui.theme.ResourceDisk
import de.kiefer_networks.proxmoxopen.core.ui.theme.ResourceRam
import de.kiefer_networks.proxmoxopen.core.ui.theme.StatusError
import de.kiefer_networks.proxmoxopen.core.ui.theme.StatusPaused
import de.kiefer_networks.proxmoxopen.core.ui.theme.StatusRunning
import de.kiefer_networks.proxmoxopen.core.ui.theme.StatusStopped
import de.kiefer_networks.proxmoxopen.domain.model.BackupJob
import de.kiefer_networks.proxmoxopen.domain.model.Guest
import de.kiefer_networks.proxmoxopen.domain.model.GuestStatus
import de.kiefer_networks.proxmoxopen.domain.model.GuestType
import de.kiefer_networks.proxmoxopen.domain.model.Node
import de.kiefer_networks.proxmoxopen.domain.model.NodeStatus
import de.kiefer_networks.proxmoxopen.domain.result.ApiError
import de.kiefer_networks.proxmoxopen.ui.common.FingerprintMismatchDialog
import de.kiefer_networks.proxmoxopen.ui.format.formatUptime
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onBack: () -> Unit,
    onOpenNode: (String) -> Unit,
    onOpenGuest: (Guest) -> Unit,
    onSettings: () -> Unit = {},
    onActivity: () -> Unit = {},
    onStorage: (String) -> Unit = {},
    onOpenSearch: () -> Unit = {},
    onOpenHa: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val filteredGuests by viewModel.filteredGuests.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        stringResource(R.string.tab_nodes),
        stringResource(R.string.tab_vms),
        stringResource(R.string.tab_containers),
        stringResource(R.string.tab_backups),
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 0.dp,
            ) {
                tabs.forEachIndexed { index, label ->
                    val icon = when (index) {
                        0 -> Icons.Outlined.Storage
                        1 -> Icons.Outlined.Computer
                        2 -> Icons.Outlined.Inventory2
                        else -> Icons.Outlined.Backup
                    }
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = {
                            selectedTab = index
                            if (index == 3) viewModel.loadBackupJobs()
                        },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label) },
                    )
                }
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        state.serverName.ifBlank { stringResource(R.string.dashboard_title) },
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSearch) {
                        Icon(Icons.Outlined.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = onOpenHa) {
                        Icon(Icons.Outlined.Shield, contentDescription = "HA")
                    }
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        when {
            state.isLoading && state.cluster == null -> LoadingState(Modifier.padding(padding))
            state.error is ApiError.FingerprintMismatch && state.cluster == null -> {
                val fpError = state.error as ApiError.FingerprintMismatch
                FingerprintMismatchDialog(
                    expected = fpError.expected,
                    actual = fpError.actual,
                    onDismiss = onBack,
                )
            }
            state.error != null && state.cluster == null ->
                ErrorState(
                    message = state.error?.message ?: "",
                    retryLabel = stringResource(R.string.retry),
                    onRetry = viewModel::refresh,
                    modifier = Modifier.padding(padding),
                )
            else -> {
                val runningGuests = state.guests.count { it.status == GuestStatus.RUNNING }
                val onlineNodes = state.cluster?.nodes?.count { it.status == NodeStatus.ONLINE } ?: 0
                val totalGuests = state.guests.size
                val totalNodes = state.cluster?.nodes?.size ?: 0

                Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                    // Offline banner — rendered whenever the dashboard is serving a
                    // cached snapshot instead of live data.
                    state.fromCacheCapturedAt?.let { capturedAt ->
                        OfflineCacheBanner(capturedAt = capturedAt)
                    }
                    // Summary Cards Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            ),
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Icon(
                                    Icons.Outlined.Storage,
                                    contentDescription = "Nodes",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Nodes",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = "$onlineNodes/$totalNodes online",
                                    style = MaterialTheme.typography.titleMedium,
                                )
                            }
                        }
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            ),
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Icon(
                                    Icons.Outlined.Computer,
                                    contentDescription = "Guests",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Guests",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = "$runningGuests/$totalGuests running",
                                    style = MaterialTheme.typography.titleMedium,
                                )
                            }
                        }
                    }

                    // Search bar (guests tabs only)
                    if (selectedTab in 1..2) {
                        OutlinedTextField(
                            value = state.searchQuery,
                            onValueChange = viewModel::onSearch,
                            placeholder = {
                                Text(
                                    stringResource(R.string.search_hint),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .height(48.dp),
                            textStyle = MaterialTheme.typography.bodySmall,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            ),
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.Search,
                                    contentDescription = "Search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                        )
                    }

                    when (selectedTab) {
                        0 -> NodesList(state.cluster?.nodes.orEmpty(), onOpenNode)
                        1 -> GuestList(
                            filteredGuests.filter { it.type == GuestType.QEMU },
                            onOpenGuest,
                        )
                        2 -> GuestList(
                            filteredGuests.filter { it.type == GuestType.LXC },
                            onOpenGuest,
                        )
                        3 -> BackupJobsList(
                            jobs = state.backupJobs,
                            nodeNames = state.cluster?.nodes?.map { it.name } ?: emptyList(),
                            storageNames = state.backupStorages,
                            guests = state.guests,
                            onRunJob = viewModel::runBackupJob,
                            onCreateJob = viewModel::createBackupJob,
                            onUpdateJob = viewModel::updateBackupJob,
                            onDeleteJob = viewModel::deleteBackupJob,
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Offline banner
// ---------------------------------------------------------------------------

@Composable
private fun OfflineCacheBanner(capturedAt: Long) {
    val formatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val timestamp = remember(capturedAt) { formatter.format(Date(capturedAt)) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        ),
    ) {
        Text(
            text = stringResource(R.string.offline_banner, timestamp),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

// ---------------------------------------------------------------------------
// Nodes Tab
// ---------------------------------------------------------------------------

@Composable
private fun NodesList(nodes: List<Node>, onOpen: (String) -> Unit) {
    if (nodes.isEmpty()) {
        Text(
            stringResource(R.string.dashboard_no_guests),
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    Text(
        text = "NODES",
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 8.dp),
    )

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(nodes, key = { it.name }) { node ->
            NodeCard(node) { onOpen(node.name) }
        }
    }
}

@Composable
private fun NodeCard(node: Node, onOpen: () -> Unit) {
    val isOnline = node.status == NodeStatus.ONLINE
    val statusColor = if (isOnline) StatusRunning else StatusError
    val statusLabel = if (isOnline) "Online" else "Offline"

    val cpuPct = (node.cpuUsage * 100).toInt()
    val memF = if (node.memTotal > 0) node.memUsed.toFloat() / node.memTotal else 0f
    val memPct = (memF * 100).toInt()
    val diskF = if (node.diskTotal > 0) node.diskUsed.toFloat() / node.diskTotal else 0f
    val diskPct = (diskF * 100).toInt()

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = node.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Uptime: ${formatUptime(node.uptimeSeconds)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                StatusPill(label = statusLabel, color = statusColor)
            }

            Spacer(Modifier.height(16.dp))

            ResourceRow(
                icon = Icons.Outlined.Speed,
                label = "CPU",
                percent = cpuPct,
                color = StatusRunning,
            )
            Spacer(Modifier.height(8.dp))
            ResourceRow(
                icon = Icons.Outlined.Memory,
                label = "Memory",
                percent = memPct,
                color = ResourceRam,
            )
            Spacer(Modifier.height(8.dp))
            ResourceRow(
                icon = Icons.Outlined.Storage,
                label = "Disk",
                percent = diskPct,
                color = ResourceDisk,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Guest Tab (VMs & Containers)
// ---------------------------------------------------------------------------

@Composable
private fun GuestList(guests: List<Guest>, onOpen: (Guest) -> Unit) {
    if (guests.isEmpty()) {
        Text(
            stringResource(R.string.dashboard_no_guests),
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    Text(
        text = "GUESTS",
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 8.dp),
    )

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(guests, key = { "${it.node}/${it.type}/${it.vmid}" }) { guest ->
            GuestCard(guest) { onOpen(guest) }
        }
    }
}

@Composable
private fun GuestCard(guest: Guest, onOpen: () -> Unit) {
    val isRunning = guest.status == GuestStatus.RUNNING
    val statusColor = when (guest.status) {
        GuestStatus.RUNNING -> StatusRunning
        GuestStatus.STOPPED -> StatusError
        GuestStatus.PAUSED, GuestStatus.SUSPENDED -> StatusPaused
        GuestStatus.UNKNOWN -> StatusStopped
    }

    val cpuPct = (guest.cpuUsage * 100).toInt()
    val memF = if (guest.memTotal > 0) guest.memUsed.toFloat() / guest.memTotal else 0f
    val memPct = (memF * 100).toInt()

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Status dot
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(statusColor, CircleShape),
            )
            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${guest.vmid}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = guest.name,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                Text(
                    text = guest.node,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (isRunning) {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Mini CPU bar
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "CPU $cpuPct%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(2.dp))
                            LinearProgressIndicator(
                                progress = { guest.cpuUsage.toFloat().coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = StatusRunning,
                                trackColor = StatusRunning.copy(alpha = 0.15f),
                                strokeCap = StrokeCap.Round,
                            )
                        }
                        // Mini RAM bar
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "RAM $memPct%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(2.dp))
                            LinearProgressIndicator(
                                progress = { memF.coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = ResourceRam,
                                trackColor = ResourceRam.copy(alpha = 0.15f),
                                strokeCap = StrokeCap.Round,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.width(4.dp))
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Open",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Backups Tab
// ---------------------------------------------------------------------------

@Composable
private fun BackupJobsList(
    jobs: List<BackupJob>,
    nodeNames: List<String> = emptyList(),
    storageNames: List<String> = emptyList(),
    guests: List<de.kiefer_networks.proxmoxopen.domain.model.Guest> = emptyList(),
    onRunJob: (String) -> Unit,
    onCreateJob: (Map<String, String>) -> Unit,
    onUpdateJob: (String, Map<String, String>) -> Unit,
    onDeleteJob: (String) -> Unit,
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingJob by remember { mutableStateOf<BackupJob?>(null) }
    var deletingJobId by remember { mutableStateOf<String?>(null) }

    // Create dialog
    if (showCreateDialog) {
        BackupJobDialog(
            editJob = null,
            nodeNames = nodeNames,
            storageNames = storageNames,
            guests = guests,
            onDismiss = { showCreateDialog = false },
            onSave = { params ->
                onCreateJob(params)
                showCreateDialog = false
            },
        )
    }

    // Edit dialog
    editingJob?.let { job ->
        BackupJobDialog(
            editJob = job,
            nodeNames = nodeNames,
            storageNames = storageNames,
            guests = guests,
            onDismiss = { editingJob = null },
            onSave = { params ->
                onUpdateJob(job.id, params)
                editingJob = null
            },
        )
    }

    // Delete confirmation dialog
    deletingJobId?.let { id ->
        AlertDialog(
            onDismissRequest = { deletingJobId = null },
            title = { Text(stringResource(R.string.backup_job_delete_title)) },
            text = { Text(stringResource(R.string.backup_job_delete_body, id)) },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteJob(id)
                    deletingJobId = null
                }) { Text(stringResource(R.string.delete_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { deletingJobId = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    // Create button
    OutlinedButton(
        onClick = { showCreateDialog = true },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Icon(Icons.Outlined.Add, contentDescription = "Add", modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.backup_job_create))
    }

    if (jobs.isEmpty()) {
        Text(
            stringResource(R.string.backup_no_jobs),
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    Text(
        text = "BACKUP JOBS",
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 8.dp),
    )

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(jobs, key = { it.id }) { job ->
            BackupJobCard(
                job = job,
                onRun = { onRunJob(job.id) },
                onEdit = { editingJob = job },
                onDelete = { deletingJobId = job.id },
            )
        }
    }
}

@Composable
private fun BackupJobCard(
    job: BackupJob,
    onRun: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val isEnabled = job.enabled
    val statusColor = if (isEnabled) StatusRunning else StatusStopped
    val statusLabel = if (isEnabled) stringResource(R.string.backup_job_enabled) else stringResource(R.string.backup_job_disabled)
    val title = if (job.allGuests) stringResource(R.string.backup_job_all_guests) else job.id

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: title + enabled badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                StatusPill(label = statusLabel, color = statusColor)
            }

            Spacer(Modifier.height(12.dp))

            // Info rows
            job.storage?.let {
                BackupInfoRow(label = stringResource(R.string.backup_job_storage), value = it)
            }
            job.schedule?.let {
                BackupInfoRow(label = stringResource(R.string.backup_job_schedule), value = it)
            }
            job.mode?.let {
                BackupInfoRow(label = stringResource(R.string.backup_job_mode), value = it.replaceFirstChar { c -> c.uppercase() })
            }
            val coveredVms = when {
                job.allGuests -> "All"
                !job.vmid.isNullOrBlank() -> job.vmid!!
                !job.pool.isNullOrBlank() -> "Pool: ${job.pool}"
                else -> "None"
            }
            BackupInfoRow(label = "Covered VMs", value = coveredVms)

            job.nextRun?.let { epoch ->
                val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                    .format(Date(epoch * 1000))
                BackupInfoRow(label = "Next run", value = dateStr)
            }

            Spacer(Modifier.height(12.dp))

            // Action buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onRun,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(
                        Icons.Outlined.PlayArrow,
                        contentDescription = "Run backup",
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.backup_job_run))
                }
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Outlined.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
private fun BackupJobDialog(
    editJob: BackupJob?,
    nodeNames: List<String>,
    storageNames: List<String>,
    guests: List<de.kiefer_networks.proxmoxopen.domain.model.Guest>,
    onDismiss: () -> Unit,
    onSave: (Map<String, String>) -> Unit,
) {
    // Step state
    var step by remember { mutableIntStateOf(0) }
    val stepTitles = listOf("Schedule", "Guests", "Target", "Options")

    // Form state
    var enabled by remember { mutableStateOf(editJob?.enabled != false) }
    var storage by remember { mutableStateOf(editJob?.storage ?: storageNames.firstOrNull() ?: "local") }
    var schedule by remember { mutableStateOf(editJob?.schedule ?: "") }
    var mode by remember { mutableStateOf(editJob?.mode ?: "snapshot") }
    var compress by remember { mutableStateOf(editJob?.compress ?: "zstd") }
    var allGuests by remember { mutableStateOf(editJob?.allGuests == true) }
    val selectedVmids = remember {
        mutableStateListOf<Int>().apply {
            editJob?.vmid?.split(",")?.mapNotNull { it.trim().toIntOrNull() }?.let { addAll(it) }
        }
    }
    var mailto by remember { mutableStateOf(editJob?.mailto ?: "") }
    var mailnotification by remember { mutableStateOf(editJob?.mailnotification ?: "always") }
    var selectedNode by remember { mutableStateOf(editJob?.node ?: "") }
    var notes by remember { mutableStateOf(editJob?.notes ?: "") }

    val schedulePresets = listOf(
        "Daily 2:00" to "*/1 02:00", "Daily 3:00" to "*/1 03:00",
        "Weekly Sat" to "sat 02:00", "Weekly Sun" to "sun 03:00",
        "Mon-Fri" to "mon..fri 22:00", "Monthly 1st" to "1 03:00",
    )

    val isValid = schedule.isNotBlank() && storage.isNotBlank() && (allGuests || selectedVmids.isNotEmpty())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(if (editJob != null) stringResource(R.string.backup_job_edit) else stringResource(R.string.backup_job_create), style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))
                // Stepper indicator
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    stepTitles.forEachIndexed { i, title ->
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                Modifier.size(28.dp).background(
                                    if (i <= step) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                                    CircleShape
                                ), contentAlignment = Alignment.Center
                            ) {
                                Text("${i + 1}", style = MaterialTheme.typography.labelSmall,
                                    color = if (i <= step) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(title, style = MaterialTheme.typography.labelSmall,
                                color = if (i <= step) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1)
                        }
                    }
                }
            }
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()).heightIn(min = 300.dp)) {
                when (step) {
                    // --- Step 1: Schedule ---
                    0 -> {
                        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.backup_job_enabled), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            Switch(checked = enabled, onCheckedChange = { enabled = it })
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(stringResource(R.string.backup_job_schedule), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(8.dp))
                        androidx.compose.foundation.layout.FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            schedulePresets.forEach { (label, cron) ->
                                FilterChip(selected = schedule == cron, onClick = { schedule = cron },
                                    label = { Text(label, style = MaterialTheme.typography.labelSmall) })
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(value = schedule, onValueChange = { schedule = it },
                            label = { Text(stringResource(R.string.backup_job_cron)) }, placeholder = { Text("sat 02:00") },
                            isError = schedule.isBlank(), modifier = Modifier.fillMaxWidth(), singleLine = true,
                            shape = RoundedCornerShape(12.dp))
                    }

                    // --- Step 2: Guest Selection ---
                    1 -> {
                        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.backup_job_all_guests), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            Switch(checked = allGuests, onCheckedChange = { allGuests = it })
                        }
                        if (!allGuests) {
                            Spacer(Modifier.height(8.dp))
                            Text(stringResource(R.string.backup_job_select_guests), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(8.dp))
                            guests.sortedBy { it.vmid }.forEach { guest ->
                                val checked = guest.vmid in selectedVmids
                                Row(
                                    Modifier.fillMaxWidth().clickable {
                                        if (checked) selectedVmids.remove(guest.vmid) else selectedVmids.add(guest.vmid)
                                    }.padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Checkbox(checked = checked, onCheckedChange = {
                                        if (it) selectedVmids.add(guest.vmid) else selectedVmids.remove(guest.vmid)
                                    })
                                    Spacer(Modifier.width(8.dp))
                                    Text("${guest.vmid}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(8.dp))
                                    Text(guest.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                    Text(guest.type.name, style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            if (!allGuests && selectedVmids.isEmpty()) {
                                Text(stringResource(R.string.backup_job_select_hint), style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    }

                    // --- Step 3: Target ---
                    2 -> {
                        Text(stringResource(R.string.backup_job_storage), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(8.dp))
                        if (storageNames.isNotEmpty()) {
                            storageNames.forEach { s ->
                                Row(Modifier.fillMaxWidth().clickable { storage = s }.padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = storage == s, onClick = { storage = s })
                                    Spacer(Modifier.width(8.dp))
                                    Text(s, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        } else {
                            OutlinedTextField(value = storage, onValueChange = { storage = it },
                                label = { Text(stringResource(R.string.backup_job_storage)) }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                                shape = RoundedCornerShape(12.dp))
                        }

                        Spacer(Modifier.height(16.dp))
                        Text(stringResource(R.string.backup_job_node), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(8.dp))
                        val nodeOptions = listOf("" to stringResource(R.string.backup_job_all_nodes)) + nodeNames.map { it to it }
                        nodeOptions.forEach { (value, label) ->
                            Row(Modifier.fillMaxWidth().clickable { selectedNode = value }.padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = selectedNode == value, onClick = { selectedNode = value })
                                Spacer(Modifier.width(8.dp))
                                Text(label, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }

                    // --- Step 4: Options ---
                    3 -> {
                        Text(stringResource(R.string.backup_job_mode), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(8.dp))
                        listOf("snapshot" to "Snapshot", "suspend" to "Suspend", "stop" to "Stop").forEach { (v, l) ->
                            Row(Modifier.fillMaxWidth().clickable { mode = v }.padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = mode == v, onClick = { mode = v })
                                Spacer(Modifier.width(8.dp))
                                Text(l, style = MaterialTheme.typography.bodyMedium)
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        Text(stringResource(R.string.backup_job_compression), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("zstd", "lzo", "gzip", "none").forEach { c ->
                                FilterChip(selected = compress == c, onClick = { compress = c },
                                    label = { Text(c.uppercase()) })
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        Text(stringResource(R.string.backup_job_notification), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("always" to "Always", "failure" to "On Failure").forEach { (v, l) ->
                                FilterChip(selected = mailnotification == v, onClick = { mailnotification = v },
                                    label = { Text(l) })
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(value = mailto, onValueChange = { mailto = it },
                            label = { Text(stringResource(R.string.backup_job_mailto)) }, placeholder = { Text("admin@example.com") },
                            modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp))
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(value = notes, onValueChange = { notes = it },
                            label = { Text(stringResource(R.string.backup_job_notes)) }, modifier = Modifier.fillMaxWidth(), minLines = 2,
                            shape = RoundedCornerShape(12.dp))
                    }
                }
            }
        },
        confirmButton = {
            Row {
                if (step > 0) {
                    TextButton(onClick = { step-- }) { Text(stringResource(R.string.back)) }
                    Spacer(Modifier.width(8.dp))
                }
                if (step < 3) {
                    TextButton(onClick = { step++ }) { Text(stringResource(R.string.next)) }
                } else {
                    TextButton(
                        onClick = {
                            val params = mutableMapOf<String, String>()
                            params["enabled"] = if (enabled) "1" else "0"
                            if (allGuests) params["all"] = "1"
                            else if (selectedVmids.isNotEmpty()) params["vmid"] = selectedVmids.joinToString(",")
                            if (storage.isNotBlank()) params["storage"] = storage
                            if (schedule.isNotBlank()) params["schedule"] = schedule
                            params["mode"] = mode; params["compress"] = compress
                            if (selectedNode.isNotBlank()) params["node"] = selectedNode
                            if (mailto.isNotBlank()) params["mailto"] = mailto
                            params["mailnotification"] = mailnotification
                            if (notes.isNotBlank()) params["notes-template"] = notes
                            onSave(params)
                        },
                        enabled = isValid,
                    ) { Text(stringResource(R.string.save)) }
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun BackupInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
        )
    }
}

// ---------------------------------------------------------------------------
// Shared Components
// ---------------------------------------------------------------------------

@Composable
private fun ResourceRow(
    icon: ImageVector,
    label: String,
    percent: Int,
    color: Color,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "$percent%",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
            )
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { percent / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.15f),
            strokeCap = StrokeCap.Round,
        )
    }
}

@Composable
private fun StatusPill(label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.15f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color, CircleShape),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color,
            )
        }
    }
}
