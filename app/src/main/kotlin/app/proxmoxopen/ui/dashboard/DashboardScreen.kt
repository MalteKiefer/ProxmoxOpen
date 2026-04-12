package app.proxmoxopen.ui.dashboard

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.proxmoxopen.R
import app.proxmoxopen.core.ui.component.BadgeTone
import app.proxmoxopen.core.ui.component.ErrorState
import app.proxmoxopen.core.ui.component.HeroHeader
import app.proxmoxopen.core.ui.component.LoadingState
import app.proxmoxopen.core.ui.component.StatusBadge
import app.proxmoxopen.domain.model.Guest
import app.proxmoxopen.domain.model.GuestStatus
import app.proxmoxopen.domain.model.GuestType
import app.proxmoxopen.domain.model.Node
import app.proxmoxopen.domain.model.NodeStatus
import app.proxmoxopen.ui.format.formatBytes
import app.proxmoxopen.ui.format.formatUptime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onBack: () -> Unit,
    onOpenNode: (String) -> Unit,
    onOpenGuest: (Guest) -> Unit,
    onSettings: () -> Unit = {},
    onActivity: () -> Unit = {},
    onStorage: (String) -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        stringResource(R.string.tab_nodes),
        stringResource(R.string.tab_vms),
        stringResource(R.string.tab_containers),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.serverName.ifBlank { stringResource(R.string.dashboard_title) }) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val firstNode = state.cluster?.nodes?.firstOrNull()?.name
                        if (firstNode != null) onStorage(firstNode)
                    }) {
                        Icon(Icons.Outlined.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Outlined.Refresh, contentDescription = null)
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
    ) { padding ->
        when {
            state.isLoading && state.cluster == null -> LoadingState(Modifier.padding(padding))
            state.error != null && state.cluster == null ->
                ErrorState(
                    message = state.error?.message ?: "",
                    retryLabel = stringResource(R.string.retry),
                    onRetry = viewModel::refresh,
                    modifier = Modifier.padding(padding),
                )
            else -> {
                val runningGuests = state.guests.count { it.status == GuestStatus.RUNNING }
                val totalGuests = state.guests.size
                val onlineNodes = state.cluster?.nodes?.count { it.status == NodeStatus.ONLINE } ?: 0
                val totalNodes = state.cluster?.nodes?.size ?: 0

                Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        HeroHeader(
                            title = state.serverName.ifBlank { stringResource(R.string.dashboard_title) },
                            subtitle = state.cluster?.let {
                                "${it.name} · ${if (it.quorate) "quorate" else "no quorum"}"
                            },
                            icon = Icons.Outlined.Cloud,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            QuickStat(
                                label = stringResource(R.string.tab_nodes),
                                value = "$onlineNodes / $totalNodes",
                                icon = Icons.Outlined.Storage,
                                modifier = Modifier.weight(1f),
                            )
                            QuickStat(
                                label = stringResource(R.string.dashboard_running),
                                value = "$runningGuests / $totalGuests",
                                icon = Icons.Outlined.Memory,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            tabs.forEachIndexed { index, label ->
                                SegmentedButton(
                                    selected = selectedTab == index,
                                    onClick = { selectedTab = index },
                                    shape = SegmentedButtonDefaults.itemShape(index, tabs.size),
                                ) { Text(label) }
                            }
                        }
                        if (selectedTab > 0) {
                            androidx.compose.material3.OutlinedTextField(
                                value = state.searchQuery,
                                onValueChange = viewModel::onSearch,
                                placeholder = { Text(stringResource(R.string.search_hint), style = MaterialTheme.typography.bodySmall) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                    when (selectedTab) {
                        0 -> NodesList(state.cluster?.nodes.orEmpty(), onOpenNode)
                        1 -> GuestList(state.filteredGuests.filter { it.type == GuestType.QEMU }, onOpenGuest)
                        else -> GuestList(state.filteredGuests.filter { it.type == GuestType.LXC }, onOpenGuest)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickStat(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 8.dp))
            }
            Text(value, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold))
        }
    }
}

@Composable
private fun NodesList(nodes: List<Node>, onOpen: (String) -> Unit) {
    if (nodes.isEmpty()) { Text(stringResource(R.string.dashboard_no_guests), modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant); return }
    LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(nodes, key = { it.name }) { node -> NodeRow(node) { onOpen(node.name) } }
    }
}

@Composable
private fun NodeRow(node: Node, onOpen: () -> Unit) {
    val tone = when (node.status) { NodeStatus.ONLINE -> BadgeTone.Running; NodeStatus.OFFLINE -> BadgeTone.Error; NodeStatus.UNKNOWN -> BadgeTone.Neutral }
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(40.dp)) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer) } }
                Column(Modifier.padding(start = 12.dp).weight(1f)) {
                    Text(node.name, style = MaterialTheme.typography.titleMedium)
                    Text("${stringResource(R.string.metric_uptime)}: ${formatUptime(node.uptimeSeconds)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                StatusBadge(label = node.status.name.lowercase().replaceFirstChar { it.titlecase() }, tone = tone)
            }
            MetricRow(stringResource(R.string.metric_cpu), node.cpuUsage.toFloat(), "${(node.cpuUsage * 100).toInt()}% · ${node.cpuCount} cores")
            val memF = if (node.memTotal > 0) node.memUsed.toFloat() / node.memTotal else 0f
            MetricRow(stringResource(R.string.metric_memory), memF, "${formatBytes(node.memUsed)} / ${formatBytes(node.memTotal)}")
            val diskF = if (node.diskTotal > 0) node.diskUsed.toFloat() / node.diskTotal else 0f
            MetricRow("HD", diskF, "${formatBytes(node.diskUsed)} / ${formatBytes(node.diskTotal)}")
            val swapF = if (node.swapTotal > 0) node.swapUsed.toFloat() / node.swapTotal else 0f
            MetricRow("Swap", swapF, "${formatBytes(node.swapUsed)} / ${formatBytes(node.swapTotal)}")
            // Info rows
            node.cpuModel?.let { InfoR("CPU(s)", "${node.cpuCount} x $it") }
            InfoR("Load", node.loadAverage.joinToString(", ") { "%.2f".format(it) }.ifBlank { "—" })
            node.kernelVersion?.let { InfoR("Kernel", it) }
            node.pveVersion?.let { InfoR("Manager", it) }
        }
    }
}

@Composable
private fun GuestList(guests: List<Guest>, onOpen: (Guest) -> Unit) {
    if (guests.isEmpty()) { Text(stringResource(R.string.dashboard_no_guests), modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant); return }
    LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(guests, key = { "${it.node}/${it.type}/${it.vmid}" }) { guest -> GuestRow(guest) { onOpen(guest) } }
    }
}

@Composable
private fun GuestRow(guest: Guest, onOpen: () -> Unit) {
    val tone = when (guest.status) { GuestStatus.RUNNING -> BadgeTone.Running; GuestStatus.STOPPED -> BadgeTone.Stopped; GuestStatus.PAUSED, GuestStatus.SUSPENDED -> BadgeTone.Paused; GuestStatus.UNKNOWN -> BadgeTone.Neutral }
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(40.dp)) { Box(contentAlignment = Alignment.Center) { Icon(if (guest.type == GuestType.QEMU) Icons.Outlined.Computer else Icons.Outlined.Inventory2, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer) } }
                Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) { Text(guest.name, style = MaterialTheme.typography.titleMedium); Text("${guest.node} · ${stringResource(R.string.vm_id, guest.vmid)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                StatusBadge(label = guest.status.name.lowercase().replaceFirstChar { it.titlecase() }, tone = tone)
            }
            if (guest.status == GuestStatus.RUNNING) {
                MetricRow(stringResource(R.string.metric_cpu), guest.cpuUsage.toFloat(), "${(guest.cpuUsage * 100).toInt()}%")
                val memF = if (guest.memTotal > 0) guest.memUsed.toFloat() / guest.memTotal else 0f
                MetricRow(stringResource(R.string.metric_memory), memF, "${formatBytes(guest.memUsed)} / ${formatBytes(guest.memTotal)}")
            }
        }
    }
}

@Composable
private fun InfoR(l: String, v: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(l, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(v, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun MetricRow(label: String, fraction: Float, caption: String) {
    Column {
        Row { Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f)); Text(caption, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        LinearProgressIndicator(progress = { fraction.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().padding(top = 4.dp), color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.surfaceContainerHighest)
    }
}
