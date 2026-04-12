package app.proxmoxopen.ui.guestdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.NetworkCheck
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Storage
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.proxmoxopen.R
import app.proxmoxopen.core.ui.component.BadgeTone
import app.proxmoxopen.core.ui.component.ChartCard
import app.proxmoxopen.core.ui.component.ErrorState
import app.proxmoxopen.core.ui.component.LoadingState
import app.proxmoxopen.core.ui.component.SectionLabel
import app.proxmoxopen.core.ui.component.StatusBadge
import app.proxmoxopen.domain.model.Backup
import app.proxmoxopen.domain.model.GuestStatus
import app.proxmoxopen.domain.model.ProxmoxTask
import app.proxmoxopen.domain.model.RrdPoint
import app.proxmoxopen.domain.model.RrdTimeframe
import app.proxmoxopen.domain.model.Snapshot
import app.proxmoxopen.domain.model.TaskState
import app.proxmoxopen.domain.model.VmConfig
import app.proxmoxopen.domain.model.VmStatus
import app.proxmoxopen.ui.format.formatBytes
import app.proxmoxopen.ui.format.formatUptime
import app.proxmoxopen.ui.power.PowerActionSheet
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VmDetailScreen(
    onBack: () -> Unit,
    onSettings: () -> Unit = {},
    onConsole: () -> Unit = {},
    onOpenTask: (node: String, upid: String) -> Unit = { _, _ -> },
    viewModel: VmHubViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    var sheetOpen by remember { mutableStateOf(false) }
    var createSnapDialog by remember { mutableStateOf(false) }
    var backupDialog by remember { mutableStateOf(false) }

    LaunchedEffect(selectedTab) { viewModel.onTabChanged(selectedTab) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Column { Text(state.status?.name ?: "VM ${viewModel.vmid}", style = MaterialTheme.typography.titleMedium); state.status?.let { Text("${it.node} · QEMU ${it.vmid}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } } },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) } },
                actions = {
                    IconButton(onClick = onConsole) { Icon(Icons.Outlined.Terminal, contentDescription = stringResource(R.string.console_title), tint = MaterialTheme.colorScheme.primary) }
                    IconButton(onClick = { sheetOpen = true }) { Icon(Icons.Outlined.PowerSettingsNew, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                    IconButton(onClick = onSettings) { Icon(Icons.Outlined.Settings, contentDescription = null) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                listOf(Icons.Outlined.Info to R.string.ct_tab_summary, Icons.Outlined.ShowChart to R.string.ct_tab_charts, Icons.Outlined.PhotoCamera to R.string.ct_tab_snapshots, Icons.Outlined.Backup to R.string.ct_tab_backup, Icons.Outlined.History to R.string.ct_tab_tasks)
                    .forEachIndexed { i, (icon, labelRes) -> NavigationBarItem(selected = selectedTab == i, onClick = { selectedTab = i }, icon = { Icon(icon, contentDescription = null) }, label = { Text(stringResource(labelRes), style = MaterialTheme.typography.labelSmall) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = MaterialTheme.colorScheme.primary, selectedTextColor = MaterialTheme.colorScheme.primary, indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))) }
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (state.isRefreshing) LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp), color = MaterialTheme.colorScheme.primary)
            when {
                state.isLoading && state.status == null -> LoadingState()
                state.error != null && state.status == null -> ErrorState(state.error?.message ?: "", stringResource(R.string.retry), viewModel::refresh)
                else -> Box(Modifier.fillMaxSize()) {
                    when (selectedTab) {
                        0 -> VmSummary(state.status, state.config, state.actionMessage)
                        1 -> VmCharts(state.rrd, state.status, state.timeframe, viewModel::setTimeframe)
                        2 -> VmSnapshots(state.snapshots, { createSnapDialog = true }, viewModel::rollbackSnapshot, viewModel::deleteSnapshot)
                        3 -> VmBackups(state.backupStorages, state.backups, state.selectedBackupStorage, viewModel::selectBackupStorage, { backupDialog = true }, viewModel::restoreBackup, state.actionMessage)
                        4 -> VmTasks(state.tasks) { task -> onOpenTask(task.node, task.upid) }
                    }
                }
            }
        }
    }
    if (sheetOpen) PowerActionSheet(guestName = state.status?.name ?: "", onDismiss = { sheetOpen = false }, onSelect = { sheetOpen = false; viewModel.triggerAction(it) })
    if (createSnapDialog) { var n by remember { mutableStateOf("") }; var d by remember { mutableStateOf("") }; AlertDialog(onDismissRequest = { createSnapDialog = false }, title = { Text(stringResource(R.string.snap_create)) }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(n, { n = it }, label = { Text(stringResource(R.string.snap_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth()); OutlinedTextField(d, { d = it }, label = { Text(stringResource(R.string.config_description)) }, modifier = Modifier.fillMaxWidth()) } }, confirmButton = { TextButton(onClick = { createSnapDialog = false; viewModel.createSnapshot(n, d.ifBlank { null }) }, enabled = n.isNotBlank()) { Text(stringResource(R.string.snap_create)) } }, dismissButton = { TextButton(onClick = { createSnapDialog = false }) { Text(stringResource(R.string.cancel)) } }) }
    if (backupDialog) { var s by remember { mutableStateOf(state.backupStorages.firstOrNull() ?: "local") }; var m by remember { mutableStateOf("snapshot") }; var c by remember { mutableStateOf("zstd") }; var p by remember { mutableStateOf(false) }; var notes by remember { mutableStateOf("{{guestname}}") }; AlertDialog(onDismissRequest = { backupDialog = false }, title = { Text(stringResource(R.string.backup_dialog_title)) }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(s, { s = it }, label = { Text("Storage") }, singleLine = true, modifier = Modifier.fillMaxWidth()); OutlinedTextField(m, { m = it }, label = { Text("Mode") }, singleLine = true, modifier = Modifier.fillMaxWidth()); OutlinedTextField(c, { c = it }, label = { Text("Compression") }, singleLine = true, modifier = Modifier.fillMaxWidth()); Row(Modifier.fillMaxWidth().clickable { p = !p }, verticalAlignment = Alignment.CenterVertically) { Checkbox(p, { p = it }); Text("Protected") }; OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth()) } }, confirmButton = { TextButton(onClick = { backupDialog = false; viewModel.createBackup(s, m, c, p, notes.ifBlank { null }) }) { Text(stringResource(R.string.backup_now)) } }, dismissButton = { TextButton(onClick = { backupDialog = false }) { Text(stringResource(R.string.cancel)) } }) }
}

// ---- VM Summary ----
@Composable private fun VmSummary(st: VmStatus?, cfg: VmConfig?, msg: String?) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        st?.let { vm ->
            val tone = when (vm.status) { GuestStatus.RUNNING -> BadgeTone.Running; GuestStatus.STOPPED -> BadgeTone.Stopped; GuestStatus.PAUSED -> BadgeTone.Paused; else -> BadgeTone.Neutral }
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Text("${vm.name} (${formatUptime(vm.uptime)})", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f)); StatusBadge(vm.status.name.lowercase().replaceFirstChar { it.titlecase() }, tone) }
                    IR("Status", vm.qmpStatus ?: vm.status.name.lowercase())
                    IR("HA State", vm.haState ?: "none")
                    IR("Node", vm.node)
                    IR("Guest Agent", if (vm.agentEnabled) "Enabled" else "Disabled")
                    cfg?.let { c ->
                        c.bios?.let { IR("BIOS", it) }
                        c.machine?.let { IR("Machine", it) }
                        c.cpuType?.let { IR("CPU Type", it) }
                        IR("Sockets / Cores", "${c.sockets ?: 1} / ${c.cores ?: 1}")
                        c.ostype?.let { IR("OS Type", it) }
                        c.vga?.let { IR("Display", it) }
                        c.scsihw?.let { IR("SCSI HW", it) }
                        c.bootOrder?.let { IR("Boot Order", it) }
                    }
                    vm.runningQemu?.let { IR("QEMU", it) }
                }
            }
            MB("CPU", vm.cpuUsage.toFloat(), "%.1f%% of %d CPU(s)".format(vm.cpuUsage * 100, vm.cpuCount))
            MB("Memory", if (vm.memTotal > 0) vm.memUsed.toFloat() / vm.memTotal else 0f, "${formatBytes(vm.memUsed)} / ${formatBytes(vm.memTotal)}")
            // Disk: use config size if status reports 0
            val diskSize = if (vm.diskTotal > 0) vm.diskTotal else {
                cfg?.disks?.firstOrNull()?.second?.let { raw ->
                    val sizeStr = app.proxmoxopen.ui.format.parseDiskString(raw).size
                    sizeStr?.replace("G", "")?.toDoubleOrNull()?.let { (it * 1073741824).toLong() }
                } ?: 0L
            }
            val diskFrac = if (diskSize > 0) vm.diskUsed.toFloat() / diskSize.toFloat() else 0f
            MB("Bootdisk", diskFrac, "${formatBytes(vm.diskUsed)} / ${formatBytes(diskSize)}")
            // Disks
            cfg?.disks?.let { disks ->
                if (disks.isNotEmpty()) {
                    SectionLabel("Disks")
                    disks.forEach { (id, raw) ->
                        val parsed = app.proxmoxopen.ui.format.parseDiskString(raw)
                        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(id, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                                    parsed.size?.let { Text(it, style = MaterialTheme.typography.labelMedium) }
                                }
                                Text(parsed.storage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                val badges = mutableListOf<String>()
                                parsed.format?.let { badges += it }
                                if (parsed.discard) badges += "TRIM"
                                if (parsed.ssd) badges += "SSD"
                                if (parsed.iothread) badges += "IOthread"
                                parsed.cache?.let { badges += "cache=$it" }
                                if (badges.isNotEmpty()) Text(badges.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            // IPs
            if (vm.ipAddresses.any { it.name != "lo" }) {
                SectionLabel("IPs (Guest Agent)")
                vm.ipAddresses.filter { it.name != "lo" }.forEach { iface ->
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) { Text(iface.name, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 12.dp)); Column { iface.inet?.let { Text(it) }; iface.inet6?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
                    }
                }
            } else if (st.agentEnabled) {
                Text("Guest Agent enabled but no IPs received", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        msg?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable private fun IR(l: String, v: String) { Row(Modifier.fillMaxWidth()) { Text(l, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f)); Text(v, style = MaterialTheme.typography.bodySmall) } }
@Composable private fun MB(l: String, f: Float, c: String) { Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) { Column(Modifier.padding(10.dp)) { Row { Text(l, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f)); Text(c, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; LinearProgressIndicator(progress = { f.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().padding(top = 4.dp), color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.surfaceContainerHighest) } } }

// ---- Charts ----
@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun VmCharts(rrd: List<RrdPoint>, st: VmStatus?, tf: RrdTimeframe, onTf: (RrdTimeframe) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { RrdTimeframe.entries.take(4).forEach { t -> FilterChip(selected = t == tf, onClick = { onTf(t) }, label = { Text(stringResource(when (t) { RrdTimeframe.HOUR -> R.string.timeframe_1h; RrdTimeframe.DAY -> R.string.timeframe_24h; RrdTimeframe.WEEK -> R.string.timeframe_7d; RrdTimeframe.MONTH -> R.string.timeframe_30d; else -> R.string.timeframe_1h })) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primaryContainer)) } }
        val cpu = rrd.mapNotNull { it.cpu }; val mem = rrd.mapNotNull { it.memUsed }; val net = rrd.mapNotNull { it.netIn }; val disk = rrd.mapNotNull { it.diskRead }
        ChartCard(Icons.Outlined.Speed, "CPU", "${((cpu.lastOrNull() ?: 0.0) * 100).toInt()}%", values = cpu)
        ChartCard(Icons.Outlined.Memory, "Memory", formatBytes((mem.lastOrNull() ?: 0.0).toLong()), secondaryValue = st?.let { "of ${formatBytes(it.memTotal)}" }, values = mem)
        ChartCard(Icons.Outlined.NetworkCheck, "Network", "${formatBytes((net.lastOrNull() ?: 0.0).toLong())}/s", values = net)
        ChartCard(Icons.Outlined.Storage, "Disk I/O", "${formatBytes((disk.lastOrNull() ?: 0.0).toLong())}/s", values = disk)
    }
}

// ---- Snapshots / Backup / Tasks — reuse from CT hub via shared composables ----
@Composable private fun VmSnapshots(snaps: List<Snapshot>, onCreate: () -> Unit, onRollback: (String) -> Unit, onDelete: (String) -> Unit) {
    val df = remember { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onCreate, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Outlined.CameraAlt, null, Modifier.padding(end = 8.dp)); Text(stringResource(R.string.snap_create)) }
        if (snaps.isEmpty()) Text(stringResource(R.string.snap_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
        snaps.forEach { s -> Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) { Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Text(s.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f)); s.snaptime?.let { Text(df.format(Date(it * 1000)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }; s.description?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { TextButton(onClick = { onRollback(s.name) }) { Icon(Icons.Outlined.Restore, null, Modifier.size(16.dp)); Text(stringResource(R.string.snap_rollback), Modifier.padding(start = 4.dp)) }; TextButton(onClick = { onDelete(s.name) }) { Icon(Icons.Outlined.Delete, null, Modifier.size(16.dp)); Text(stringResource(R.string.snap_delete), Modifier.padding(start = 4.dp)) } } } } }
    }
}

@Composable private fun VmBackups(storages: List<String>, backups: List<Backup>, selectedStorage: String?, onSelectStorage: (String) -> Unit, onBackup: () -> Unit, onRestore: (String) -> Unit, msg: String?) {
    val df = remember { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (storages.size > 1) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { storages.forEach { s -> FilterChip(selected = s == selectedStorage, onClick = { onSelectStorage(s) }, label = { Text(s) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primaryContainer)) } }
        else if (storages.size == 1) Text("Storage: ${storages[0]}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        OutlinedButton(onClick = onBackup, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Outlined.Backup, null, Modifier.padding(end = 8.dp)); Text(stringResource(R.string.backup_now)) }
        if (backups.isEmpty()) Text(stringResource(R.string.backup_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
        backups.sortedByDescending { it.createdAt }.forEach { b -> Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) { Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(b.format ?: "backup", style = MaterialTheme.typography.titleSmall); Text(df.format(Date(b.createdAt * 1000)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Text(formatBytes(b.size), style = MaterialTheme.typography.labelMedium) }; b.notes?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; if (b.protected) StatusBadge("Protected", BadgeTone.Paused); TextButton(onClick = { onRestore(b.volid) }) { Icon(Icons.Outlined.Restore, null, Modifier.size(16.dp)); Text("Restore", Modifier.padding(start = 4.dp)) } } } }
        msg?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable private fun VmTasks(tasks: List<ProxmoxTask>, onTaskClick: (ProxmoxTask) -> Unit = {}) {
    val df = remember { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT) }
    if (tasks.isEmpty()) { Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) { Text(stringResource(R.string.activity_empty_title), style = MaterialTheme.typography.titleMedium) }; return }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { items(tasks, key = { it.upid }) { task -> val tone = when (task.state) { TaskState.RUNNING -> BadgeTone.Running; TaskState.OK -> BadgeTone.Running; TaskState.FAILED -> BadgeTone.Error; TaskState.UNKNOWN -> BadgeTone.Neutral }; Card(Modifier.fillMaxWidth().clickable { onTaskClick(task) }, shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) { Column(Modifier.padding(10.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(task.type, style = MaterialTheme.typography.titleSmall); Text("${task.user} · ${df.format(Date(task.startTime * 1000))}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; StatusBadge(task.state.name, tone) }; task.exitStatus?.let { Text("Exit: $it", style = MaterialTheme.typography.bodySmall, color = if (task.state == TaskState.FAILED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant) } } } } }
}
