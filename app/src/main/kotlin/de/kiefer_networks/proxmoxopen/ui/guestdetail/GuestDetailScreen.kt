package de.kiefer_networks.proxmoxopen.ui.guestdetail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.MoveUp
import androidx.compose.material.icons.outlined.NetworkCheck
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.kiefer_networks.proxmoxopen.R
import de.kiefer_networks.proxmoxopen.core.ui.theme.ResourceCpu
import de.kiefer_networks.proxmoxopen.core.ui.theme.ResourceDisk
import de.kiefer_networks.proxmoxopen.core.ui.theme.ResourceRam
import de.kiefer_networks.proxmoxopen.core.ui.component.DetailActionRow
import de.kiefer_networks.proxmoxopen.core.ui.component.DetailInfoDivider
import de.kiefer_networks.proxmoxopen.core.ui.component.DetailInfoRow
import de.kiefer_networks.proxmoxopen.core.ui.component.DetailResourceBar
import de.kiefer_networks.proxmoxopen.core.ui.component.BadgeTone
import de.kiefer_networks.proxmoxopen.core.ui.component.ChartCard
import de.kiefer_networks.proxmoxopen.core.ui.component.ErrorState
import de.kiefer_networks.proxmoxopen.core.ui.component.LoadingState
import de.kiefer_networks.proxmoxopen.core.ui.component.DetailSectionHeader
import de.kiefer_networks.proxmoxopen.core.ui.component.StatusBadge
import de.kiefer_networks.proxmoxopen.domain.model.GuestStatus
import de.kiefer_networks.proxmoxopen.domain.model.RrdTimeframe
import de.kiefer_networks.proxmoxopen.domain.model.TaskState
import de.kiefer_networks.proxmoxopen.domain.result.ApiError
import de.kiefer_networks.proxmoxopen.ui.common.FingerprintMismatchDialog
import de.kiefer_networks.proxmoxopen.ui.format.formatBytes
import de.kiefer_networks.proxmoxopen.ui.format.formatUptime
import de.kiefer_networks.proxmoxopen.ui.power.PowerActionSheet
import java.text.DateFormat
import java.util.Date

private data class DetailTab(val label: String, val icon: ImageVector)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuestDetailScreen(
    onBack: () -> Unit,
    onSettings: () -> Unit = {},
    onActivity: () -> Unit = {},
    onEditConfig: () -> Unit = {},
    onConsole: () -> Unit = {},
    onOpenTask: (node: String, upid: String) -> Unit = { _, _ -> },
    onMigrate: () -> Unit = {},
    onClone: () -> Unit = {},
    viewModel: GuestHubViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var sheetOpen by remember { mutableStateOf(false) }
    var createSnapDialog by remember { mutableStateOf(false) }
    var backupDialog by remember { mutableStateOf(false) }
    var deleteDialog by remember { mutableStateOf(false) }

    val df = remember { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT) }

    val tabs = remember {
        listOf(
            DetailTab("Summary", Icons.Outlined.Info),
            DetailTab("Charts", Icons.Outlined.ShowChart),
            DetailTab("Snapshots", Icons.Outlined.PhotoCamera),
            DetailTab("Backups", Icons.Outlined.Backup),
            DetailTab("Tasks", Icons.Outlined.History),
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            state.containerStatus?.name ?: "CT ${viewModel.vmid}",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            "CT ${viewModel.vmid}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            Icons.Outlined.Refresh,
                            contentDescription = "Refresh",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 0.dp,
            ) {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = {
                            selectedTab = index
                            viewModel.loadTab(index)
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (state.isRefreshing) LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = MaterialTheme.colorScheme.primary,
            )
            when {
                state.isLoading && !state.hasStatus -> LoadingState()
                state.error is ApiError.FingerprintMismatch && !state.hasStatus -> {
                    val fpError = state.error as ApiError.FingerprintMismatch
                    FingerprintMismatchDialog(
                        expected = fpError.expected,
                        actual = fpError.actual,
                        onDismiss = onBack,
                    )
                }
                state.error != null && !state.hasStatus -> ErrorState(
                    state.error?.message ?: "",
                    stringResource(R.string.retry),
                    viewModel::refresh,
                )
                else -> {
                    when (selectedTab) {
                        0 -> SummaryTab(
                            state = state,
                            onEditConfig = onEditConfig,
                            onClone = onClone,
                            onMigrate = onMigrate,
                            onConsole = onConsole,
                            onStartStop = { sheetOpen = true },
                            onDelete = { deleteDialog = true },
                        )
                        1 -> ChartsTab(state = state, viewModel = viewModel)
                        2 -> SnapshotsTab(
                            state = state,
                            df = df,
                            onCreateSnapshot = { createSnapDialog = true },
                            onRollback = viewModel::rollbackSnapshot,
                            onDeleteSnapshot = viewModel::deleteSnapshot,
                        )
                        3 -> BackupsTab(
                            state = state,
                            df = df,
                            onCreateBackup = { backupDialog = true },
                            onSelectStorage = viewModel::selectBackupStorage,
                            onRestore = viewModel::restoreBackup,
                        )
                        4 -> TasksTab(
                            state = state,
                            df = df,
                            onOpenTask = onOpenTask,
                        )
                    }
                }
            }
        }
    }

    // Dialogs
    if (sheetOpen) PowerActionSheet(
        guestName = state.containerStatus?.name ?: "",
        guestType = "lxc",
        onDismiss = { sheetOpen = false },
        onSelect = { sheetOpen = false; viewModel.triggerAction(it) },
    )
    if (createSnapDialog) CreateSnapshotDialog(
        onDismiss = { createSnapDialog = false },
        onCreate = { n, d -> createSnapDialog = false; viewModel.createSnapshot(n, d) },
    )
    if (backupDialog) BackupDialog(
        storages = state.backupStorages,
        onDismiss = { backupDialog = false },
        onBackup = { s, m, c, p, n -> backupDialog = false; viewModel.createBackup(s, m, c, p, n) },
    )
    if (deleteDialog) DeleteGuestDialog(
        guestTypeLabel = stringResource(R.string.guest_type_lxc),
        vmid = viewModel.vmid,
        onDismiss = { deleteDialog = false },
        onConfirm = { purge, destroyDisks ->
            deleteDialog = false
            viewModel.deleteGuest(purge, destroyDisks) { onBack() }
        },
    )
}

// ===== TAB COMPOSABLES =====

@Composable
private fun SummaryTab(
    state: GuestHubUiState,
    onEditConfig: () -> Unit,
    onClone: () -> Unit,
    onMigrate: () -> Unit,
    onConsole: () -> Unit,
    onStartStop: () -> Unit,
    onDelete: () -> Unit,
) {
    val ct = state.containerStatus ?: return
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // ===== INFOS SECTION =====
        DetailSectionHeader("INFOS")
        val tone = when (ct.status) {
            GuestStatus.RUNNING -> BadgeTone.Running
            GuestStatus.STOPPED -> BadgeTone.Stopped
            else -> BadgeTone.Neutral
        }
        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            Column(Modifier.padding(16.dp)) {
                DetailInfoRow("Status", ct.status.name.lowercase()) {
                    StatusBadge(
                        ct.status.name.lowercase().replaceFirstChar { it.titlecase() },
                        tone,
                    )
                }
                DetailInfoDivider()
                DetailInfoRow("Uptime", formatUptime(ct.uptime))
                DetailInfoDivider()
                DetailInfoRow("Node", ct.node)
                DetailInfoDivider()
                DetailInfoRow("Unprivileged", if (ct.unprivileged) "Yes" else "No")
                ct.ostype?.let {
                    DetailInfoDivider()
                    DetailInfoRow("OS Type", it)
                }
                ct.haState?.let {
                    DetailInfoDivider()
                    DetailInfoRow("HA State", it)
                }
            }
        }

        // ===== RESOURCES SECTION =====
        Spacer(Modifier.height(8.dp))
        DetailSectionHeader("RESOURCES")
        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                DetailResourceBar(
                    icon = Icons.Outlined.Speed,
                    label = "CPU",
                    fraction = ct.cpuUsage.toFloat(),
                    valueText = "%.1f%% of %d CPU(s)".format(ct.cpuUsage * 100, ct.cpuCount),
                    barColor = ResourceCpu,
                )
                DetailResourceBar(
                    icon = Icons.Outlined.Memory,
                    label = "RAM",
                    fraction = if (ct.memTotal > 0) ct.memUsed.toFloat() / ct.memTotal else 0f,
                    valueText = "${formatBytes(ct.memUsed)} / ${formatBytes(ct.memTotal)}",
                    barColor = ResourceRam,
                )
                DetailResourceBar(
                    icon = Icons.Outlined.Memory,
                    label = "Swap",
                    fraction = if (ct.swapTotal > 0) ct.swapUsed.toFloat() / ct.swapTotal else 0f,
                    valueText = "${formatBytes(ct.swapUsed)} / ${formatBytes(ct.swapTotal)}",
                    barColor = ResourceRam,
                )
                DetailResourceBar(
                    icon = Icons.Outlined.Storage,
                    label = "Bootdisk",
                    fraction = if (ct.diskTotal > 0) ct.diskUsed.toFloat() / ct.diskTotal else 0f,
                    valueText = "${formatBytes(ct.diskUsed)} / ${formatBytes(ct.diskTotal)}",
                    barColor = ResourceDisk,
                )
            }
        }

        // ===== NETWORK SECTION =====
        val interfaces = ct.ipAddresses.filter { it.name != "lo" }
        if (interfaces.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            DetailSectionHeader("NETWORK")
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                Column(Modifier.padding(16.dp)) {
                    interfaces.forEachIndexed { index, iface ->
                        if (index > 0) DetailInfoDivider()
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                iface.name,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 12.dp),
                            )
                            Column {
                                iface.inet?.let {
                                    Text(it, style = MaterialTheme.typography.bodySmall)
                                }
                                iface.inet6?.let {
                                    Text(
                                        it,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ===== ACTIONS SECTION =====
        Spacer(Modifier.height(8.dp))
        DetailSectionHeader("ACTIONS")
        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            Column {
                if (ct.status == GuestStatus.STOPPED) {
                    DetailActionRow(Icons.Filled.PlayArrow, "Start", onClick = onStartStop)
                }
                DetailActionRow(Icons.Outlined.PowerSettingsNew, "Shutdown", onClick = onStartStop)
                DetailActionRow(Icons.Filled.Stop, "Stop", tint = MaterialTheme.colorScheme.error, onClick = onStartStop)
                DetailActionRow(Icons.Outlined.Refresh, "Reboot", onClick = onStartStop)

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                )

                DetailActionRow(Icons.Outlined.Edit, "Edit Configuration", onClick = onEditConfig)
                DetailActionRow(Icons.Outlined.ContentCopy, "Clone", onClick = onClone)
                DetailActionRow(Icons.Outlined.MoveUp, "Migrate", onClick = onMigrate)
                DetailActionRow(Icons.Outlined.Terminal, stringResource(R.string.console_title), onClick = onConsole)

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                )

                DetailActionRow(Icons.Outlined.Delete, "Delete", tint = MaterialTheme.colorScheme.error, onClick = onDelete)
            }
        }

        // Action message
        state.actionMessage?.let { msg ->
            Text(
                msg,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ChartsTab(
    state: GuestHubUiState,
    viewModel: GuestHubViewModel,
) {
    val ct = state.containerStatus
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item { DetailSectionHeader("CHARTS") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RrdTimeframe.entries.take(4).forEach { t ->
                    FilterChip(
                        selected = t == state.timeframe,
                        onClick = { viewModel.setTimeframe(t) },
                        label = {
                            Text(
                                stringResource(
                                    when (t) {
                                        RrdTimeframe.HOUR -> R.string.timeframe_1h
                                        RrdTimeframe.DAY -> R.string.timeframe_24h
                                        RrdTimeframe.WEEK -> R.string.timeframe_7d
                                        RrdTimeframe.MONTH -> R.string.timeframe_30d
                                        else -> R.string.timeframe_1h
                                    }
                                )
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    )
                }
            }
        }
        item {
            val cpu = state.rrd.mapNotNull { it.cpu }
            ChartCard(
                Icons.Outlined.Speed, "CPU",
                "${((cpu.lastOrNull() ?: 0.0) * 100).toInt()}%",
                values = cpu,
            )
        }
        item {
            val mem = state.rrd.mapNotNull { it.memUsed }
            ChartCard(
                Icons.Outlined.Memory, "Memory",
                formatBytes((mem.lastOrNull() ?: 0.0).toLong()),
                secondaryValue = ct?.let { "of ${formatBytes(it.memTotal)}" },
                values = mem,
            )
        }
        item {
            val net = state.rrd.mapNotNull { it.netIn }
            ChartCard(
                Icons.Outlined.NetworkCheck, "Network",
                "${formatBytes((net.lastOrNull() ?: 0.0).toLong())}/s",
                values = net,
            )
        }
        item {
            val disk = state.rrd.mapNotNull { it.diskRead }
            ChartCard(
                Icons.Outlined.Storage, "Disk I/O",
                "${formatBytes((disk.lastOrNull() ?: 0.0).toLong())}/s",
                values = disk,
            )
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun SnapshotsTab(
    state: GuestHubUiState,
    df: DateFormat,
    onCreateSnapshot: () -> Unit,
    onRollback: (String) -> Unit,
    onDeleteSnapshot: (String) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item { DetailSectionHeader("SNAPSHOTS") }
        item {
            OutlinedButton(
                onClick = onCreateSnapshot,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(Icons.Outlined.CameraAlt, null, Modifier.padding(end = 8.dp))
                Text(stringResource(R.string.snap_create))
            }
        }
        if (state.snapshots.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.snap_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(state.snapshots, key = { "snap_${it.name}" }) { s ->
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(s.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        s.snaptime?.let {
                            Text(
                                df.format(Date(it * 1000)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    s.description?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = { onRollback(s.name) }, modifier = Modifier.defaultMinSize(minHeight = 48.dp)) {
                            Icon(Icons.Outlined.Restore, null, Modifier.size(16.dp))
                            Text(stringResource(R.string.snap_rollback), Modifier.padding(start = 4.dp))
                        }
                        TextButton(onClick = { onDeleteSnapshot(s.name) }, modifier = Modifier.defaultMinSize(minHeight = 48.dp)) {
                            Icon(Icons.Outlined.Delete, null, Modifier.size(16.dp))
                            Text(stringResource(R.string.snap_delete), Modifier.padding(start = 4.dp))
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun BackupsTab(
    state: GuestHubUiState,
    df: DateFormat,
    onCreateBackup: () -> Unit,
    onSelectStorage: (String) -> Unit,
    onRestore: (String) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item { DetailSectionHeader("BACKUPS") }
        if (state.backupStorages.size > 1) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.backupStorages.forEach { s ->
                        FilterChip(
                            selected = s == state.selectedBackupStorage,
                            onClick = { onSelectStorage(s) },
                            label = { Text(s) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                        )
                    }
                }
            }
        } else if (state.backupStorages.size == 1) {
            item {
                Text(
                    "Storage: ${state.backupStorages[0]}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        item {
            OutlinedButton(
                onClick = onCreateBackup,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(Icons.Outlined.Backup, null, Modifier.padding(end = 8.dp))
                Text(stringResource(R.string.backup_now))
            }
        }
        if (state.backups.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.backup_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(
            state.backups.sortedByDescending { it.createdAt },
            key = { "bk_${it.volid}" },
        ) { b ->
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(b.format ?: "backup", style = MaterialTheme.typography.titleSmall)
                            Text(
                                df.format(Date(b.createdAt * 1000)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(formatBytes(b.size), style = MaterialTheme.typography.labelMedium)
                    }
                    b.notes?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (b.protected) StatusBadge(stringResource(R.string.backup_dialog_protected), BadgeTone.Paused)
                    TextButton(onClick = { onRestore(b.volid) }, modifier = Modifier.defaultMinSize(minHeight = 48.dp)) {
                        Icon(Icons.Outlined.Restore, null, Modifier.size(16.dp))
                        Text(stringResource(R.string.backup_dialog_restore), Modifier.padding(start = 4.dp))
                    }
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun TasksTab(
    state: GuestHubUiState,
    df: DateFormat,
    onOpenTask: (node: String, upid: String) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item { DetailSectionHeader("TASKS") }
        if (state.tasks.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.activity_empty_title),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(state.tasks, key = { "task_${it.upid}" }) { task ->
            val tone = when (task.state) {
                TaskState.RUNNING -> BadgeTone.Running
                TaskState.OK -> BadgeTone.Running
                TaskState.FAILED -> BadgeTone.Error
                TaskState.UNKNOWN -> BadgeTone.Neutral
            }
            Card(
                Modifier.fillMaxWidth().clickable { onOpenTask(task.node, task.upid) },
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            ) {
                Column(Modifier.padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(task.type, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${task.user} \u00b7 ${df.format(Date(task.startTime * 1000))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        StatusBadge(task.state.name, tone)
                    }
                    task.exitStatus?.let {
                        Text(
                            "Exit: $it",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (task.state == TaskState.FAILED)
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

// ---- Dialogs ----
@Composable
private fun CreateSnapshotDialog(onDismiss: () -> Unit, onCreate: (String, String?) -> Unit) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.snap_create)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.snap_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text(stringResource(R.string.config_description)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(name, desc.ifBlank { null }) },
                enabled = name.isNotBlank(),
            ) { Text(stringResource(R.string.snap_create)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BackupDialog(
    storages: List<String>,
    onDismiss: () -> Unit,
    onBackup: (String?, String, String?, Boolean, String?) -> Unit,
) {
    var storage by remember { mutableStateOf(storages.firstOrNull() ?: "local") }
    var mode by remember { mutableStateOf("snapshot") }
    var compress by remember { mutableStateOf("zstd") }
    var prot by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("{{guestname}}") }
    var sExp by remember { mutableStateOf(false) }
    var mExp by remember { mutableStateOf(false) }
    var cExp by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.backup_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ExposedDropdownMenuBox(expanded = sExp, onExpandedChange = { sExp = !sExp }) {
                    OutlinedTextField(value = storage, onValueChange = {}, readOnly = true, label = { Text(stringResource(R.string.backup_dialog_storage)) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(sExp) }, modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable))
                    androidx.compose.material3.DropdownMenu(expanded = sExp, onDismissRequest = { sExp = false }) { (storages.ifEmpty { listOf("local") }).forEach { s -> DropdownMenuItem(text = { Text(s) }, onClick = { storage = s; sExp = false }) } }
                }
                ExposedDropdownMenuBox(expanded = mExp, onExpandedChange = { mExp = !mExp }) {
                    OutlinedTextField(value = mode.replaceFirstChar { it.titlecase() }, onValueChange = {}, readOnly = true, label = { Text(stringResource(R.string.backup_dialog_mode)) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(mExp) }, modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable))
                    androidx.compose.material3.DropdownMenu(expanded = mExp, onDismissRequest = { mExp = false }) { listOf("snapshot", "suspend", "stop").forEach { m -> DropdownMenuItem(text = { Text(m.replaceFirstChar { it.titlecase() }) }, onClick = { mode = m; mExp = false }) } }
                }
                ExposedDropdownMenuBox(expanded = cExp, onExpandedChange = { cExp = !cExp }) {
                    OutlinedTextField(value = cLabel(compress), onValueChange = {}, readOnly = true, label = { Text(stringResource(R.string.backup_dialog_compression)) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(cExp) }, modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable))
                    androidx.compose.material3.DropdownMenu(expanded = cExp, onDismissRequest = { cExp = false }) { listOf("zstd" to "ZSTD", "lzo" to "LZO", "gzip" to "GZIP", "0" to "None").forEach { (v, l) -> DropdownMenuItem(text = { Text(l) }, onClick = { compress = v; cExp = false }) } }
                }
                Row(Modifier.fillMaxWidth().clickable { prot = !prot }, verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = prot, onCheckedChange = { prot = it })
                    Text(stringResource(R.string.backup_dialog_protected), Modifier.padding(start = 4.dp))
                }
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text(stringResource(R.string.backup_dialog_notes)) }, modifier = Modifier.fillMaxWidth())
                Text("{{cluster}}, {{guestname}}, {{node}}, {{vmid}}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { TextButton(onClick = { onBackup(storage, mode, compress, prot, notes.ifBlank { null }) }) { Text(stringResource(R.string.backup_now)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

private fun cLabel(v: String) = when (v) { "zstd" -> "ZSTD"; "lzo" -> "LZO"; "gzip" -> "GZIP"; "0" -> "None"; else -> v }
