package app.proxmoxopen.ui.guestdetail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.proxmoxopen.R
import app.proxmoxopen.core.ui.component.BadgeTone
import app.proxmoxopen.core.ui.component.ChartCard
import app.proxmoxopen.core.ui.component.ErrorState
import app.proxmoxopen.core.ui.component.LoadingState
import app.proxmoxopen.core.ui.component.SectionLabel
import app.proxmoxopen.core.ui.component.StatusBadge
import app.proxmoxopen.domain.model.ContainerStatus
import app.proxmoxopen.domain.model.GuestStatus
import app.proxmoxopen.domain.model.ProxmoxTask
import app.proxmoxopen.domain.model.RrdPoint
import app.proxmoxopen.domain.model.RrdTimeframe
import app.proxmoxopen.domain.model.Snapshot
import app.proxmoxopen.domain.model.TaskState
import app.proxmoxopen.ui.format.formatBytes
import app.proxmoxopen.ui.format.formatUptime
import app.proxmoxopen.ui.power.PowerActionSheet
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuestDetailScreen(
    onBack: () -> Unit,
    onSettings: () -> Unit = {},
    onActivity: () -> Unit = {},
    onEditConfig: () -> Unit = {},
    viewModel: ContainerHubViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    var sheetOpen by remember { mutableStateOf(false) }
    var createSnapDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(state.status?.name ?: "CT ${viewModel.vmid}", style = MaterialTheme.typography.titleMedium)
                        state.status?.let {
                            Text(
                                "${it.node} · LXC ${it.vmid}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
                },
                actions = {
                    IconButton(onClick = { sheetOpen = true }) { Icon(Icons.Outlined.PowerSettingsNew, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                    IconButton(onClick = onEditConfig) { Icon(Icons.Outlined.Edit, contentDescription = null) }
                    IconButton(onClick = onSettings) { Icon(Icons.Outlined.Settings, contentDescription = null) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                val tabs = listOf(
                    Icons.Outlined.Info to R.string.ct_tab_summary,
                    Icons.Outlined.ShowChart to R.string.ct_tab_charts,
                    Icons.Outlined.PhotoCamera to R.string.ct_tab_snapshots,
                    Icons.Outlined.Backup to R.string.ct_tab_backup,
                    Icons.Outlined.History to R.string.ct_tab_tasks,
                )
                tabs.forEachIndexed { i, (icon, labelRes) ->
                    NavigationBarItem(
                        selected = selectedTab == i,
                        onClick = { selectedTab = i },
                        icon = { Icon(icon, contentDescription = null) },
                        label = { Text(stringResource(labelRes), style = MaterialTheme.typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        ),
                    )
                }
            }
        },
    ) { padding ->
        when {
            state.isLoading && state.status == null -> LoadingState(Modifier.padding(padding))
            state.error != null && state.status == null ->
                ErrorState(state.error?.message ?: "", stringResource(R.string.retry), viewModel::refresh, Modifier.padding(padding))
            else -> Box(Modifier.padding(padding).fillMaxSize()) {
                when (selectedTab) {
                    0 -> SummaryTab(state.status, state.actionMessage)
                    1 -> ChartsTab(state.rrd, state.status, state.timeframe, viewModel::setTimeframe)
                    2 -> SnapshotsTab(state.snapshots, onCreateSnapshot = { createSnapDialog = true }, onRollback = viewModel::rollbackSnapshot, onDelete = viewModel::deleteSnapshot)
                    3 -> BackupTab(onBackup = { viewModel.createBackup(null, "snapshot", "zstd") }, message = state.actionMessage)
                    4 -> TasksTab(state.tasks)
                }
            }
        }
    }

    if (sheetOpen) PowerActionSheet(guestName = state.status?.name ?: "", onDismiss = { sheetOpen = false }, onSelect = { sheetOpen = false; viewModel.triggerAction(it) })
    if (createSnapDialog) CreateSnapshotDialog(onDismiss = { createSnapDialog = false }, onCreate = { n, d -> createSnapDialog = false; viewModel.createSnapshot(n, d) })
}

// ---- Summary ----

@Composable
private fun SummaryTab(status: ContainerStatus?, message: String?) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        status?.let { ct ->
            val tone = when (ct.status) { GuestStatus.RUNNING -> BadgeTone.Running; GuestStatus.STOPPED -> BadgeTone.Stopped; else -> BadgeTone.Neutral }
            // Compact info rows
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${ct.name} (${formatUptime(ct.uptime)})", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                        StatusBadge(ct.status.name.lowercase().replaceFirstChar { it.titlecase() }, tone)
                    }
                    InfoRow("Status", ct.status.name.lowercase())
                    InfoRow("HA State", ct.haState ?: "none")
                    InfoRow("Node", ct.node)
                    InfoRow("Unprivileged", if (ct.unprivileged) "Yes" else "No")
                    ct.ostype?.let { InfoRow("OS Type", it) }
                }
            }
            // Bars
            MetricBar2("CPU", ct.cpuUsage.toFloat(), "%.1f%% of %d CPU(s)".format(ct.cpuUsage * 100, ct.cpuCount))
            MetricBar2("Memory", if (ct.memTotal > 0) ct.memUsed.toFloat() / ct.memTotal else 0f, "${formatBytes(ct.memUsed)} / ${formatBytes(ct.memTotal)}")
            MetricBar2("Swap", if (ct.swapTotal > 0) ct.swapUsed.toFloat() / ct.swapTotal else 0f, "${formatBytes(ct.swapUsed)} / ${formatBytes(ct.swapTotal)}")
            MetricBar2("Bootdisk", if (ct.diskTotal > 0) ct.diskUsed.toFloat() / ct.diskTotal else 0f, "${formatBytes(ct.diskUsed)} / ${formatBytes(ct.diskTotal)}")
            // IPs
            if (ct.ipAddresses.any { it.name != "lo" }) {
                SectionLabel("IPs")
                ct.ipAddresses.filter { it.name != "lo" }.forEach { iface ->
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(iface.name, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp).padding(end = 8.dp))
                            Column {
                                iface.inet?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                                iface.inet6?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            }
                        }
                    }
                }
            }
        }
        message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable private fun InfoRow(label: String, value: String) { Row(Modifier.fillMaxWidth()) { Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f)); Text(value, style = MaterialTheme.typography.bodySmall) } }

@Composable
private fun MetricBar2(label: String, fraction: Float, caption: String) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(Modifier.padding(10.dp)) {
            Row { Text(label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f)); Text(caption, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            LinearProgressIndicator(progress = { fraction.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().padding(top = 4.dp), color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.surfaceContainerHighest)
        }
    }
}

// ---- Charts ----

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChartsTab(rrd: List<RrdPoint>, status: ContainerStatus?, timeframe: RrdTimeframe, onTimeframe: (RrdTimeframe) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RrdTimeframe.entries.take(4).forEach { tf ->
                FilterChip(selected = tf == timeframe, onClick = { onTimeframe(tf) }, label = { Text(stringResource(when (tf) { RrdTimeframe.HOUR -> R.string.timeframe_1h; RrdTimeframe.DAY -> R.string.timeframe_24h; RrdTimeframe.WEEK -> R.string.timeframe_7d; RrdTimeframe.MONTH -> R.string.timeframe_30d; else -> R.string.timeframe_1h })) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primaryContainer))
            }
        }
        val cpu = rrd.mapNotNull { it.cpu }; val mem = rrd.mapNotNull { it.memUsed }; val netIn = rrd.mapNotNull { it.netIn }; val diskRead = rrd.mapNotNull { it.diskRead }
        ChartCard(Icons.Outlined.Speed, "CPU", "${((cpu.lastOrNull() ?: 0.0) * 100).toInt()}%", values = cpu)
        ChartCard(Icons.Outlined.Memory, "Memory", formatBytes((mem.lastOrNull() ?: 0.0).toLong()), secondaryValue = status?.let { "of ${formatBytes(it.memTotal)}" }, values = mem)
        ChartCard(Icons.Outlined.NetworkCheck, "Network", "${formatBytes((netIn.lastOrNull() ?: 0.0).toLong())}/s", values = netIn)
        ChartCard(Icons.Outlined.Storage, "Disk I/O", "${formatBytes((diskRead.lastOrNull() ?: 0.0).toLong())}/s", values = diskRead)
    }
}

// ---- Snapshots ----

@Composable
private fun SnapshotsTab(snapshots: List<Snapshot>, onCreateSnapshot: () -> Unit, onRollback: (String) -> Unit, onDelete: (String) -> Unit) {
    val dateFormat = remember { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onCreateSnapshot, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.CameraAlt, contentDescription = null, modifier = Modifier.padding(end = 8.dp)); Text(stringResource(R.string.snap_create))
        }
        if (snapshots.isEmpty()) Text(stringResource(R.string.snap_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
        snapshots.forEach { snap ->
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(snap.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        snap.snaptime?.let { Text(dateFormat.format(Date(it * 1000)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    snap.description?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = { onRollback(snap.name) }) { Icon(Icons.Outlined.Restore, contentDescription = null, modifier = Modifier.size(16.dp)); Text(stringResource(R.string.snap_rollback), modifier = Modifier.padding(start = 4.dp)) }
                        TextButton(onClick = { onDelete(snap.name) }) { Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(16.dp)); Text(stringResource(R.string.snap_delete), modifier = Modifier.padding(start = 4.dp)) }
                    }
                }
            }
        }
    }
}

// ---- Backup ----

@Composable
private fun BackupTab(onBackup: () -> Unit, message: String?) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.backup_title), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.backup_description), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(onClick = onBackup, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.Backup, contentDescription = null, modifier = Modifier.padding(end = 8.dp)); Text(stringResource(R.string.backup_now))
        }
        // TODO: List existing backups from storage content API
        Text(stringResource(R.string.backup_list_todo), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    }
}

// ---- Tasks ----

@Composable
private fun TasksTab(tasks: List<ProxmoxTask>) {
    val dateFormat = remember { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT) }
    if (tasks.isEmpty()) {
        Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.activity_empty_title), style = MaterialTheme.typography.titleMedium)
        }
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items(tasks, key = { it.upid }) { task ->
            val tone = when (task.state) { TaskState.RUNNING -> BadgeTone.Running; TaskState.OK -> BadgeTone.Running; TaskState.FAILED -> BadgeTone.Error; TaskState.UNKNOWN -> BadgeTone.Neutral }
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                Column(Modifier.padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(task.type, style = MaterialTheme.typography.titleSmall)
                            Text("${task.user} · ${dateFormat.format(Date(task.startTime * 1000))}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        StatusBadge(task.state.name, tone)
                    }
                    task.exitStatus?.let { Text("Exit: $it", style = MaterialTheme.typography.bodySmall, color = if (task.state == TaskState.FAILED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }
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
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.snap_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text(stringResource(R.string.config_description)) }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { TextButton(onClick = { onCreate(name, desc.ifBlank { null }) }, enabled = name.isNotBlank()) { Text(stringResource(R.string.snap_create)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
