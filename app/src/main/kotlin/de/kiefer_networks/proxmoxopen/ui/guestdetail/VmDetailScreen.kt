package de.kiefer_networks.proxmoxopen.ui.guestdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.MoveUp
import androidx.compose.material.icons.outlined.NetworkCheck
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import de.kiefer_networks.proxmoxopen.core.ui.theme.ResourceCpu
import de.kiefer_networks.proxmoxopen.core.ui.theme.ResourceRam
import de.kiefer_networks.proxmoxopen.core.ui.theme.StatusPaused
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.kiefer_networks.proxmoxopen.R
import de.kiefer_networks.proxmoxopen.core.ui.component.DetailActionRow
import de.kiefer_networks.proxmoxopen.core.ui.component.DetailInfoDivider
import de.kiefer_networks.proxmoxopen.core.ui.component.DetailInfoRow
import de.kiefer_networks.proxmoxopen.core.ui.component.DetailResourceBar
import de.kiefer_networks.proxmoxopen.core.ui.component.DetailSectionHeader
import de.kiefer_networks.proxmoxopen.core.ui.component.BadgeTone
import de.kiefer_networks.proxmoxopen.core.ui.component.ChartCard
import de.kiefer_networks.proxmoxopen.core.ui.component.ErrorState
import de.kiefer_networks.proxmoxopen.core.ui.component.LoadingState
import de.kiefer_networks.proxmoxopen.core.ui.component.StatusBadge
import de.kiefer_networks.proxmoxopen.domain.model.GuestStatus
import de.kiefer_networks.proxmoxopen.domain.model.PowerAction
import de.kiefer_networks.proxmoxopen.domain.model.RrdTimeframe
import de.kiefer_networks.proxmoxopen.domain.model.TaskState
import de.kiefer_networks.proxmoxopen.domain.result.ApiError
import de.kiefer_networks.proxmoxopen.domain.util.parseTags
import de.kiefer_networks.proxmoxopen.ui.common.FingerprintMismatchDialog
import de.kiefer_networks.proxmoxopen.ui.format.formatBytes
import de.kiefer_networks.proxmoxopen.ui.format.formatUptime
import de.kiefer_networks.proxmoxopen.ui.format.parseDiskString
import java.text.DateFormat
import java.util.Date

// Design palette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VmDetailScreen(
    onBack: () -> Unit,
    onConsole: () -> Unit = {},
    onOpenTask: (node: String, upid: String) -> Unit = { _, _ -> },
    onMigrate: () -> Unit = {},
    onClone: () -> Unit = {},
    viewModel: GuestHubViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var createSnapDialog by remember { mutableStateOf(false) }
    var backupDialog by remember { mutableStateOf(false) }
    var deleteDialog by remember { mutableStateOf(false) }

    val df = remember { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            state.vmStatus?.name ?: "VM ${viewModel.vmid}",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        state.vmStatus?.let {
                            Text(
                                "${it.node} \u00b7 QEMU ${it.vmid}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
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
            if (state.hasStatus) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                    TabDef.entries.forEachIndexed { index, tab ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = {
                                selectedTab = index
                                viewModel.loadTab(index)
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            ),
                        )
                    }
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
                    val vm = state.vmStatus
                    val cfg = state.vmConfig
                    when (selectedTab) {
                        0 -> SummaryTab(
                            state = state,
                            vm = vm,
                            cfg = cfg,
                            df = df,
                            onAction = { viewModel.triggerAction(it) },
                            onClone = onClone,
                            onMigrate = onMigrate,
                            onConsole = onConsole,
                            onDelete = { deleteDialog = true },
                        )
                        1 -> ChartsTab(state = state, vm = vm, viewModel = viewModel)
                        2 -> SnapshotsTab(
                            state = state,
                            df = df,
                            viewModel = viewModel,
                            onCreateSnapshot = { createSnapDialog = true },
                        )
                        3 -> BackupsTab(
                            state = state,
                            df = df,
                            viewModel = viewModel,
                            onCreateBackup = { backupDialog = true },
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
    if (createSnapDialog) {
        var n by remember { mutableStateOf("") }
        var d by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { createSnapDialog = false },
            title = { Text(stringResource(R.string.snap_create)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        n, { n = it },
                        label = { Text(stringResource(R.string.snap_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        d, { d = it },
                        label = { Text(stringResource(R.string.config_description)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { createSnapDialog = false; viewModel.createSnapshot(n, d.ifBlank { null }) },
                    enabled = n.isNotBlank(),
                ) { Text(stringResource(R.string.snap_create)) }
            },
            dismissButton = {
                TextButton(onClick = { createSnapDialog = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    if (backupDialog) {
        var s by remember { mutableStateOf(state.backupStorages.firstOrNull() ?: "local") }
        var m by remember { mutableStateOf("snapshot") }
        var c by remember { mutableStateOf("zstd") }
        var p by remember { mutableStateOf(false) }
        var notes by remember { mutableStateOf("{{guestname}}") }
        AlertDialog(
            onDismissRequest = { backupDialog = false },
            title = { Text(stringResource(R.string.backup_dialog_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(s, { s = it }, label = { Text(stringResource(R.string.backup_dialog_storage)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(m, { m = it }, label = { Text(stringResource(R.string.backup_dialog_mode)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(c, { c = it }, label = { Text(stringResource(R.string.backup_dialog_compression)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Row(Modifier.fillMaxWidth().clickable { p = !p }, verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(p, { p = it })
                        Text(stringResource(R.string.backup_dialog_protected))
                    }
                    OutlinedTextField(notes, { notes = it }, label = { Text(stringResource(R.string.backup_dialog_notes)) }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    backupDialog = false
                    viewModel.createBackup(s, m, c, p, notes.ifBlank { null })
                }) { Text(stringResource(R.string.backup_now)) }
            },
            dismissButton = {
                TextButton(onClick = { backupDialog = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    if (deleteDialog) DeleteGuestDialog(
        guestTypeLabel = stringResource(R.string.guest_type_qemu),
        vmid = viewModel.vmid,
        expectedName = state.vmStatus?.name?.takeIf { it.isNotBlank() }
            ?: viewModel.vmid.toString(),
        onDismiss = { deleteDialog = false },
        onConfirm = { purge, destroyDisks ->
            deleteDialog = false
            viewModel.deleteGuest(purge, destroyDisks) { onBack() }
        },
    )
}

// ---- Tab definitions ----

private enum class TabDef(val label: String, val icon: ImageVector) {
    Summary("Summary", Icons.Outlined.Info),
    Charts("Charts", Icons.AutoMirrored.Outlined.ShowChart),
    Snapshots("Snapshots", Icons.Outlined.PhotoCamera),
    Backups("Backups", Icons.Outlined.Backup),
    Tasks("Tasks", Icons.Outlined.History),
}

// ---- Summary Tab ----

@Composable
private fun SummaryTab(
    state: GuestHubUiState,
    vm: de.kiefer_networks.proxmoxopen.domain.model.VmStatus?,
    cfg: de.kiefer_networks.proxmoxopen.domain.model.VmConfig?,
    df: DateFormat,
    onAction: (PowerAction) -> Unit,
    onClone: () -> Unit,
    onMigrate: () -> Unit,
    onConsole: () -> Unit,
    onDelete: () -> Unit,
) {
    val scrollState = rememberScrollState()
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        if (vm != null) {
            // ===== TAGS SECTION =====
            val tagList = parseTags(vm.tags ?: cfg?.tags)
            if (tagList.isNotEmpty()) {
                DetailSectionHeader(stringResource(R.string.tags_label).uppercase())
                SectionCard {
                    TagChips(tagList)
                }
            }

            // ===== INFOS SECTION =====
            DetailSectionHeader("INFOS")
            val statusColor = when (vm.status) {
                GuestStatus.RUNNING -> ResourceCpu
                GuestStatus.STOPPED -> MaterialTheme.colorScheme.error
                GuestStatus.PAUSED -> StatusPaused
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            val statusLabel = vm.status.name.lowercase().replaceFirstChar { it.titlecase() }

            SectionCard {
                DetailInfoRow("Status", vm.qmpStatus ?: statusLabel) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = statusColor.copy(alpha = 0.15f),
                    ) {
                        Row(
                            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier
                                    .size(8.dp)
                                    .background(statusColor, CircleShape)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                statusLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = statusColor,
                            )
                        }
                    }
                }
                DetailInfoDivider()
                DetailInfoRow("Uptime", formatUptime(vm.uptime))
                DetailInfoDivider()
                DetailInfoRow("Node", vm.node)
                DetailInfoDivider()
                DetailInfoRow("Guest Agent", if (vm.agentEnabled) "Active" else "Inactive")
                vm.haState?.let {
                    DetailInfoDivider()
                    DetailInfoRow("HA State", it)
                }
                cfg?.let { c ->
                    c.bios?.let { DetailInfoDivider(); DetailInfoRow("BIOS", it) }
                    c.machine?.let { DetailInfoDivider(); DetailInfoRow("Machine", it) }
                    c.cpuType?.let { DetailInfoDivider(); DetailInfoRow("CPU Type", it) }
                    DetailInfoDivider(); DetailInfoRow("Sockets / Cores", "${c.sockets ?: 1} / ${c.cores ?: 1}")
                    c.ostype?.let { DetailInfoDivider(); DetailInfoRow("OS Type", it) }
                    c.vga?.let { DetailInfoDivider(); DetailInfoRow("Display", it) }
                    c.scsihw?.let { DetailInfoDivider(); DetailInfoRow("SCSI HW", it) }
                    c.bootOrder?.let { DetailInfoDivider(); DetailInfoRow("Boot Order", it) }
                }
                vm.runningQemu?.let { DetailInfoDivider(); DetailInfoRow("QEMU", it) }
            }

            // ===== RESOURCES SECTION =====
            DetailSectionHeader("RESOURCES")
            SectionCard {
                DetailResourceBar(
                    icon = Icons.Outlined.Speed,
                    label = "CPU",
                    fraction = vm.cpuUsage.toFloat(),
                    valueText = "%.1f%% of %d CPU(s)".format(vm.cpuUsage * 100, vm.cpuCount),
                    barColor = ResourceCpu,
                )
                DetailResourceBar(
                    icon = Icons.Outlined.Memory,
                    label = "Memory",
                    fraction = if (vm.memTotal > 0) vm.memUsed.toFloat() / vm.memTotal else 0f,
                    valueText = "${formatBytes(vm.memUsed)} / ${formatBytes(vm.memTotal)}",
                    barColor = ResourceRam,
                )
                val diskSize = if (vm.diskTotal > 0) vm.diskTotal else {
                    cfg?.disks?.firstOrNull()?.second?.let { raw ->
                        val sizeStr = parseDiskString(raw).size
                        sizeStr?.replace("G", "")?.toDoubleOrNull()
                            ?.let { (it * 1073741824).toLong() }
                    } ?: 0L
                }
                val diskFrac = if (diskSize > 0) vm.diskUsed.toFloat() / diskSize.toFloat() else 0f
                DetailResourceBar(
                    icon = Icons.Outlined.Storage,
                    label = "Bootdisk",
                    fraction = diskFrac,
                    valueText = "${formatBytes(vm.diskUsed)} / ${formatBytes(diskSize)}",
                    barColor = MaterialTheme.colorScheme.primary,
                )
            }

            // Disks inline
            cfg?.disks?.forEach { (id, raw) ->
                val parsed = parseDiskString(raw)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(id, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                            parsed.size?.let { Text(it, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium) }
                        }
                        Text(parsed.storage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val badges = mutableListOf<String>()
                        parsed.format?.let { badges += it }
                        if (parsed.discard) badges += "TRIM"
                        if (parsed.ssd) badges += "SSD"
                        if (parsed.iothread) badges += "IOthread"
                        parsed.cache?.let { badges += "cache=$it" }
                        if (badges.isNotEmpty()) Text(
                            badges.joinToString(" \u00b7 "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // ===== NETWORK SECTION =====
            val interfaces = vm.ipAddresses.filter { it.name != "lo" }
            if (interfaces.isNotEmpty()) {
                DetailSectionHeader("NETWORK")
                SectionCard {
                    interfaces.forEachIndexed { index, iface ->
                        if (index > 0) DetailInfoDivider()
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                iface.name,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(end = 12.dp),
                            )
                            Column {
                                iface.inet?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
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
            } else if (vm.agentEnabled) {
                DetailSectionHeader("NETWORK")
                SectionCard {
                    Text(
                        "Guest Agent enabled but no IPs received",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // ===== ACTIONS SECTION =====
            DetailSectionHeader("ACTIONS")
            SectionCard {
                val isStopped = vm.status == GuestStatus.STOPPED
                val isRunning = vm.status == GuestStatus.RUNNING
                val isPaused = vm.status == GuestStatus.PAUSED

                if (isStopped || isPaused) {
                    DetailActionRow(Icons.Outlined.PlayArrow, "Start") { onAction(PowerAction.START) }
                    DetailInfoDivider()
                }
                if (isPaused) {
                    DetailActionRow(Icons.Outlined.PlayArrow, "Resume") { onAction(PowerAction.RESUME) }
                    DetailInfoDivider()
                }
                if (isRunning) {
                    DetailActionRow(Icons.Outlined.PowerSettingsNew, "Shutdown") { onAction(PowerAction.SHUTDOWN) }
                    DetailInfoDivider()
                }
                DetailActionRow(Icons.Outlined.Stop, "Stop", tint = MaterialTheme.colorScheme.error) { onAction(PowerAction.STOP) }
                DetailInfoDivider()
                if (isRunning) {
                    DetailActionRow(Icons.Outlined.Refresh, "Reboot") { onAction(PowerAction.REBOOT) }
                    DetailInfoDivider()
                    DetailActionRow(Icons.Outlined.RestartAlt, "Reset") { onAction(PowerAction.RESET) }
                    DetailInfoDivider()
                    DetailActionRow(Icons.Outlined.Pause, "Suspend") { onAction(PowerAction.SUSPEND) }
                    DetailInfoDivider()
                }
                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                DetailActionRow(Icons.Outlined.ContentCopy, "Clone") { onClone() }
                DetailInfoDivider()
                DetailActionRow(Icons.Outlined.MoveUp, "Migrate") { onMigrate() }
                DetailInfoDivider()
                DetailActionRow(Icons.Outlined.Terminal, stringResource(R.string.console_title)) { onConsole() }
                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                DetailActionRow(Icons.Outlined.Delete, "Delete", tint = MaterialTheme.colorScheme.error) { onDelete() }
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
        }

        Spacer(Modifier.height(24.dp))
    }
}

// ---- Charts Tab ----

@Composable
private fun ChartsTab(
    state: GuestHubUiState,
    vm: de.kiefer_networks.proxmoxopen.domain.model.VmStatus?,
    viewModel: GuestHubViewModel,
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
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
                secondaryValue = vm?.let { "of ${formatBytes(it.memTotal)}" },
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
        item { Spacer(Modifier.height(24.dp)) }
    }
}

// ---- Snapshots Tab ----

@Composable
private fun SnapshotsTab(
    state: GuestHubUiState,
    df: DateFormat,
    viewModel: GuestHubViewModel,
    onCreateSnapshot: () -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
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
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
        }
        items(state.snapshots, key = { "snap_${it.name}" }) { s ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(s.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
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
                        TextButton(onClick = { viewModel.rollbackSnapshot(s.name) }, modifier = Modifier.defaultMinSize(minHeight = 48.dp)) {
                            Icon(Icons.Outlined.Restore, null, Modifier.size(16.dp))
                            Text(stringResource(R.string.snap_rollback), Modifier.padding(start = 4.dp))
                        }
                        TextButton(onClick = { viewModel.deleteSnapshot(s.name) }, modifier = Modifier.defaultMinSize(minHeight = 48.dp)) {
                            Icon(Icons.Outlined.Delete, null, Modifier.size(16.dp))
                            Text(stringResource(R.string.snap_delete), Modifier.padding(start = 4.dp))
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

// ---- Backups Tab ----

@Composable
private fun BackupsTab(
    state: GuestHubUiState,
    df: DateFormat,
    viewModel: GuestHubViewModel,
    onCreateBackup: () -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (state.backupStorages.size > 1) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.backupStorages.forEach { s ->
                        FilterChip(
                            selected = s == state.selectedBackupStorage,
                            onClick = { viewModel.selectBackupStorage(s) },
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
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
        }
        items(
            state.backups.sortedByDescending { it.createdAt },
            key = { "bkp_${it.volid}" },
        ) { b ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(b.format ?: "backup", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(
                                df.format(Date(b.createdAt * 1000)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(formatBytes(b.size), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
                    }
                    b.notes?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (b.protected) StatusBadge(stringResource(R.string.backup_dialog_protected), BadgeTone.Paused)
                    TextButton(onClick = { viewModel.restoreBackup(b.volid) }, modifier = Modifier.defaultMinSize(minHeight = 48.dp)) {
                        Icon(Icons.Outlined.Restore, null, Modifier.size(16.dp))
                        Text(stringResource(R.string.backup_dialog_restore), Modifier.padding(start = 4.dp))
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

// ---- Tasks Tab ----

@Composable
private fun TasksTab(
    state: GuestHubUiState,
    df: DateFormat,
    onOpenTask: (node: String, upid: String) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (state.tasks.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.activity_empty_title),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(state.tasks, key = { it.upid }) { task ->
            val tone = when (task.state) {
                TaskState.RUNNING -> BadgeTone.Running
                TaskState.OK -> BadgeTone.Running
                TaskState.FAILED -> BadgeTone.Error
                TaskState.UNKNOWN -> BadgeTone.Neutral
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .clickable { onOpenTask(task.node, task.upid) },
            ) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(task.type, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
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
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

// ---- Helper composables ----

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            content()
        }
    }
}

