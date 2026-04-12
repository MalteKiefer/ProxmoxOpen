package app.proxmoxopen.ui.guestdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.NetworkCheck
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.proxmoxopen.R
import app.proxmoxopen.core.ui.component.BadgeTone
import app.proxmoxopen.core.ui.component.ChartCard
import app.proxmoxopen.core.ui.component.ErrorState
import app.proxmoxopen.core.ui.component.HeroHeader
import app.proxmoxopen.core.ui.component.LoadingState
import app.proxmoxopen.core.ui.component.MetricCard
import app.proxmoxopen.core.ui.component.SectionLabel
import app.proxmoxopen.core.ui.component.StatusBadge
import app.proxmoxopen.domain.model.GuestStatus
import app.proxmoxopen.domain.model.GuestType
import app.proxmoxopen.domain.model.RrdPoint
import app.proxmoxopen.ui.format.formatBytes
import app.proxmoxopen.ui.format.formatUptime
import app.proxmoxopen.ui.power.PowerActionSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuestDetailScreen(
    onBack: () -> Unit,
    onSettings: () -> Unit = {},
    onActivity: () -> Unit = {},
    onEditConfig: () -> Unit = {},
    viewModel: GuestDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var sheetOpen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.guest?.name ?: "…") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = onEditConfig) {
                        Icon(Icons.Outlined.Edit, contentDescription = null)
                    }
                    IconButton(onClick = onActivity) {
                        Icon(Icons.Outlined.History, contentDescription = null)
                    }
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text(stringResource(R.string.power_actions)) },
                icon = { Icon(Icons.Outlined.PowerSettingsNew, contentDescription = null) },
                onClick = { sheetOpen = true },
            )
        },
    ) { padding ->
        when {
            state.isLoading && state.guest == null -> LoadingState(Modifier.padding(padding))
            state.error != null && state.guest == null ->
                ErrorState(
                    message = state.error?.message ?: "",
                    retryLabel = stringResource(R.string.retry),
                    onRetry = viewModel::refresh,
                    modifier = Modifier.padding(padding),
                )
            else -> Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                state.guest?.let { guest ->
                    val tone = when (guest.status) {
                        GuestStatus.RUNNING -> BadgeTone.Running
                        GuestStatus.STOPPED -> BadgeTone.Stopped
                        GuestStatus.PAUSED, GuestStatus.SUSPENDED -> BadgeTone.Paused
                        GuestStatus.UNKNOWN -> BadgeTone.Neutral
                    }
                    HeroHeader(
                        title = guest.name,
                        subtitle = "${guest.node} · ${stringResource(R.string.vm_id, guest.vmid)}",
                        icon = if (guest.type == GuestType.QEMU) Icons.Outlined.Computer else Icons.Outlined.Inventory2,
                        trailing = {
                            StatusBadge(
                                label = guest.status.name.lowercase().replaceFirstChar { it.titlecase() },
                                tone = tone,
                            )
                        },
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MetricCard(
                            icon = Icons.Outlined.Memory,
                            label = stringResource(R.string.metric_cpu),
                            primaryValue = "${(guest.cpuUsage * 100).toInt()}%",
                            secondaryValue = "${guest.cpuCount} cores",
                            progress = guest.cpuUsage.toFloat(),
                            modifier = Modifier.weight(1f),
                        )
                        val memFraction = if (guest.memTotal > 0) {
                            guest.memUsed.toFloat() / guest.memTotal.toFloat()
                        } else {
                            0f
                        }
                        MetricCard(
                            icon = Icons.Outlined.Storage,
                            label = stringResource(R.string.metric_memory),
                            primaryValue = formatBytes(guest.memUsed),
                            secondaryValue = "of ${formatBytes(guest.memTotal)}",
                            progress = memFraction,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MetricCard(
                            icon = Icons.Outlined.Schedule,
                            label = stringResource(R.string.metric_uptime),
                            primaryValue = formatUptime(guest.uptimeSeconds),
                            secondaryValue = if (guest.tags.isEmpty()) null else guest.tags.joinToString(", "),
                            modifier = Modifier.weight(1f),
                        )
                    }

                    if (guest.status == GuestStatus.RUNNING) {
                        SectionLabel(stringResource(R.string.charts_title))
                        MetricCharts(state.rrd, guest.memTotal)
                    }

                    state.actionMessage?.let { msg ->
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    if (sheetOpen) {
        PowerActionSheet(
            guestName = state.guest?.name ?: "",
            onDismiss = { sheetOpen = false },
            onSelect = { action ->
                sheetOpen = false
                viewModel.triggerAction(action)
            },
        )
    }
}

@Composable
private fun MetricCharts(rrd: List<RrdPoint>, memTotal: Long) {
    val cpu = rrd.mapNotNull { it.cpu }
    val mem = rrd.mapNotNull { it.memUsed }
    val netIn = rrd.mapNotNull { it.netIn }
    val diskRead = rrd.mapNotNull { it.diskRead }

    val cpuCurrent = cpu.lastOrNull() ?: 0.0
    val memCurrent = mem.lastOrNull() ?: 0.0
    val netCurrent = netIn.lastOrNull() ?: 0.0
    val diskCurrent = diskRead.lastOrNull() ?: 0.0

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ChartCard(
            icon = Icons.Outlined.Speed,
            title = stringResource(R.string.metric_cpu),
            currentValue = "${(cpuCurrent * 100).toInt()}%",
            values = cpu,
        )
        ChartCard(
            icon = Icons.Outlined.Memory,
            title = stringResource(R.string.metric_memory),
            currentValue = formatBytes(memCurrent.toLong()),
            secondaryValue = if (memTotal > 0) "of ${formatBytes(memTotal)}" else null,
            values = mem,
        )
        ChartCard(
            icon = Icons.Outlined.NetworkCheck,
            title = stringResource(R.string.metric_network),
            currentValue = "${formatBytes(netCurrent.toLong())}/s",
            values = netIn,
        )
        ChartCard(
            icon = Icons.Outlined.Storage,
            title = stringResource(R.string.metric_disk),
            currentValue = "${formatBytes(diskCurrent.toLong())}/s",
            values = diskRead,
        )
    }
}
