package app.proxmoxopen.ui.nodedetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.NetworkCheck
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.Storage
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import app.proxmoxopen.domain.model.Node
import app.proxmoxopen.domain.model.NodeStatus
import app.proxmoxopen.domain.model.RrdPoint
import app.proxmoxopen.domain.model.RrdTimeframe
import app.proxmoxopen.ui.format.formatBytes
import app.proxmoxopen.ui.format.formatUptime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NodeDetailScreen(
    onBack: () -> Unit,
    onSettings: () -> Unit = {},
    onActivity: () -> Unit = {},
    onConsole: () -> Unit = {},
    viewModel: NodeDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(selectedTab) { viewModel.onTabChanged() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(viewModel.nodeName, style = MaterialTheme.typography.titleMedium)
                        state.node?.let { Text("${stringResource(R.string.metric_uptime)}: ${formatUptime(it.uptimeSeconds)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) } },
                actions = {
                    IconButton(onClick = onConsole) { Icon(Icons.Outlined.Terminal, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                    IconButton(onClick = onActivity) { Icon(Icons.Outlined.History, contentDescription = null) }
                    IconButton(onClick = onSettings) { Icon(Icons.Outlined.Settings, contentDescription = null) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                listOf(
                    Icons.Outlined.Info to R.string.ct_tab_summary,
                    Icons.Outlined.ShowChart to R.string.ct_tab_charts,
                ).forEachIndexed { i, (icon, labelRes) ->
                    NavigationBarItem(selected = selectedTab == i, onClick = { selectedTab = i }, icon = { Icon(icon, contentDescription = null) }, label = { Text(stringResource(labelRes)) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = MaterialTheme.colorScheme.primary, selectedTextColor = MaterialTheme.colorScheme.primary, indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)))
                }
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (state.isRefreshing) LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp), color = MaterialTheme.colorScheme.primary)
            when {
                state.isLoading && state.node == null -> LoadingState()
                state.error != null && state.node == null -> ErrorState(state.error?.message ?: "", stringResource(R.string.retry), viewModel::refresh)
                else -> Box(Modifier.fillMaxSize()) {
                    when (selectedTab) {
                        0 -> SummaryTab(state.node)
                        1 -> ChartsTab(state.rrd, state.node, state.timeframe, viewModel::setTimeframe)
                    }
                }
            }
        }
    }
}

// ---- Summary ----

@Composable
private fun SummaryTab(node: Node?) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        node?.let { n ->
            val tone = when (n.status) { NodeStatus.ONLINE -> BadgeTone.Running; NodeStatus.OFFLINE -> BadgeTone.Error; NodeStatus.UNKNOWN -> BadgeTone.Neutral }
            // Info card
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${n.name} (${formatUptime(n.uptimeSeconds)})", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                        StatusBadge(n.status.name.lowercase().replaceFirstChar { it.titlecase() }, tone)
                    }
                    IR("Status", n.status.name.lowercase())
                    n.cpuModel?.let { IR("CPU(s)", "${n.cpuCount} x $it") }
                    IR("Load Average", n.loadAverage.joinToString(", ") { "%.2f".format(it) }.ifBlank { "—" })
                    n.kernelVersion?.let { IR("Kernel", it) }
                    n.pveVersion?.let { IR("PVE Manager", it) }
                }
            }
            // Resource bars
            MB("CPU", n.cpuUsage.toFloat(), "%.1f%% of %d CPU(s)".format(n.cpuUsage * 100, n.cpuCount))
            MB("RAM", if (n.memTotal > 0) n.memUsed.toFloat() / n.memTotal else 0f, "${formatBytes(n.memUsed)} / ${formatBytes(n.memTotal)}")
            MB("HD", if (n.diskTotal > 0) n.diskUsed.toFloat() / n.diskTotal else 0f, "${formatBytes(n.diskUsed)} / ${formatBytes(n.diskTotal)}")
            MB("Swap", if (n.swapTotal > 0) n.swapUsed.toFloat() / n.swapTotal else 0f, "${formatBytes(n.swapUsed)} / ${formatBytes(n.swapTotal)}")
            n.ioDelay?.let { MB("IO Delay", it.toFloat(), "%.1f%%".format(it * 100)) }
            n.ksmShared?.let { if (it > 0) IR("KSM Sharing", formatBytes(it)) }
        }
    }
}

@Composable private fun IR(l: String, v: String) { Row(Modifier.fillMaxWidth()) { Text(l, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f)); Text(v, style = MaterialTheme.typography.bodySmall) } }
@Composable private fun MB(l: String, f: Float, c: String) { Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) { Column(Modifier.padding(10.dp)) { Row { Text(l, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f)); Text(c, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; LinearProgressIndicator(progress = { f.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().padding(top = 4.dp), color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.surfaceContainerHighest) } } }

// ---- Charts ----

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChartsTab(rrd: List<RrdPoint>, node: Node?, tf: RrdTimeframe, onTf: (RrdTimeframe) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RrdTimeframe.entries.take(4).forEach { t ->
                FilterChip(selected = t == tf, onClick = { onTf(t) }, label = { Text(stringResource(when (t) { RrdTimeframe.HOUR -> R.string.timeframe_1h; RrdTimeframe.DAY -> R.string.timeframe_24h; RrdTimeframe.WEEK -> R.string.timeframe_7d; RrdTimeframe.MONTH -> R.string.timeframe_30d; else -> R.string.timeframe_1h })) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primaryContainer))
            }
        }
        val cpu = rrd.mapNotNull { it.cpu }; val mem = rrd.mapNotNull { it.memUsed }; val net = rrd.mapNotNull { it.netIn }; val disk = rrd.mapNotNull { it.diskRead }
        ChartCard(Icons.Outlined.Speed, "CPU", "${((cpu.lastOrNull() ?: 0.0) * 100).toInt()}%", values = cpu)
        ChartCard(Icons.Outlined.Memory, "Memory", formatBytes((mem.lastOrNull() ?: 0.0).toLong()), secondaryValue = node?.let { "of ${formatBytes(it.memTotal)}" }, values = mem)
        ChartCard(Icons.Outlined.NetworkCheck, "Network", "${formatBytes((net.lastOrNull() ?: 0.0).toLong())}/s", values = net)
        ChartCard(Icons.Outlined.Storage, "Disk I/O", "${formatBytes((disk.lastOrNull() ?: 0.0).toLong())}/s", values = disk)
    }
}
