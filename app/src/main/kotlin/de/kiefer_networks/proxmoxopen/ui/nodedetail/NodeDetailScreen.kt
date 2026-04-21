package de.kiefer_networks.proxmoxopen.ui.nodedetail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lan
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.NetworkCheck
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import de.kiefer_networks.proxmoxopen.core.ui.theme.ResourceCpu
import de.kiefer_networks.proxmoxopen.core.ui.theme.ResourceDisk
import de.kiefer_networks.proxmoxopen.core.ui.theme.ResourceRam
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
import de.kiefer_networks.proxmoxopen.core.ui.component.StatusDot
import de.kiefer_networks.proxmoxopen.domain.model.Node
import de.kiefer_networks.proxmoxopen.domain.model.NodeStatus
import de.kiefer_networks.proxmoxopen.domain.model.RrdPoint
import de.kiefer_networks.proxmoxopen.domain.model.RrdTimeframe
import de.kiefer_networks.proxmoxopen.ui.format.formatBytes
import de.kiefer_networks.proxmoxopen.ui.format.formatUptime

private data class NodeTab(val label: String, val icon: ImageVector)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NodeDetailScreen(
    onBack: () -> Unit,
    onSettings: () -> Unit = {},
    onActivity: () -> Unit = {},
    onConsole: () -> Unit = {},
    onStorage: () -> Unit = {},
    onOpenApt: (serverId: Long, node: String) -> Unit = { _, _ -> },
    onOpenDisks: () -> Unit = {},
    onOpenNetwork: () -> Unit = {},
    onOpenTask: (node: String, upid: String) -> Unit = { _, _ -> },
    viewModel: NodeDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    val tabs = listOf(
        NodeTab("Summary", Icons.Outlined.Info),
        NodeTab("Charts", Icons.AutoMirrored.Outlined.ShowChart),
        NodeTab("Tasks", Icons.Outlined.History),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(viewModel.nodeName, style = MaterialTheme.typography.titleMedium)
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
                            contentDescription = stringResource(R.string.retry),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
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
                            viewModel.onTabChanged()
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (state.isRefreshing) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            when {
                state.isLoading && state.node == null -> LoadingState()
                state.error != null && state.node == null -> ErrorState(
                    state.error?.message ?: "",
                    stringResource(R.string.retry),
                    viewModel::refresh,
                )
                else -> {
                    when (selectedTab) {
                        0 -> SummaryTab(
                            node = state.node,
                            onConsole = onConsole,
                            onStorage = onStorage,
                            onApt = { onOpenApt(viewModel.serverId, viewModel.nodeName) },
                            onOpenDisks = onOpenDisks,
                            onOpenNetwork = onOpenNetwork,
                            viewModel = viewModel,
                        )
                        1 -> ChartsTab(
                            node = state.node,
                            rrd = state.rrd,
                            timeframe = state.timeframe,
                            onTimeframeChange = viewModel::setTimeframe,
                        )
                        2 -> TasksTab(state = state, onOpenTask = onOpenTask)
                    }
                }
            }
        }
    }
}

// ===== TAB COMPOSABLES =====

@Composable
private fun SummaryTab(
    node: Node?,
    onConsole: () -> Unit,
    onStorage: () -> Unit,
    onApt: () -> Unit,
    onOpenDisks: () -> Unit,
    onOpenNetwork: () -> Unit,
    viewModel: NodeDetailViewModel,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        node?.let { n ->
            // ---- INFOS section ----
            DetailSectionHeader("INFOS")
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
            ) {
                Column {
                    val tone = when (n.status) {
                        NodeStatus.ONLINE -> BadgeTone.Running
                        NodeStatus.OFFLINE -> BadgeTone.Error
                        NodeStatus.UNKNOWN -> BadgeTone.Neutral
                    }

                    // Status row with pill badge
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Status",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.weight(1f))
                        StatusDot(tone, modifier = Modifier.size(10.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            n.status.name.lowercase().replaceFirstChar { it.titlecase() },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    DetailInfoDivider()

                    // Uptime
                    DetailInfoRow("Uptime", formatUptime(n.uptimeSeconds))
                    DetailInfoDivider()

                    // PVE Version
                    n.pveVersion?.let {
                        DetailInfoRow("PVE Version", it)
                        DetailInfoDivider()
                    }

                    // Kernel
                    n.kernelVersion?.let {
                        DetailInfoRow("Kernel", it)
                        DetailInfoDivider()
                    }

                    // CPU Model
                    n.cpuModel?.let {
                        DetailInfoRow("CPU Model", "${n.cpuCount} x $it")
                        DetailInfoDivider()
                    }

                    // Load Average
                    DetailInfoRow(
                        "Load Average",
                        n.loadAverage.joinToString(", ") { "%.2f".format(it) }.ifBlank { "\u2014" },
                    )

                    // KSM Sharing (if available)
                    n.ksmShared?.let {
                        if (it > 0) {
                            DetailInfoDivider()
                            DetailInfoRow("KSM Sharing", formatBytes(it))
                        }
                    }
                }
            }

            // ---- RESOURCES section ----
            DetailSectionHeader("RESOURCES")
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
            ) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    // CPU
                    DetailResourceBar(
                        icon = Icons.Outlined.Speed,
                        label = "CPU",
                        valueText = "%.1f%% of %d CPU(s)".format(n.cpuUsage * 100, n.cpuCount),
                        fraction = n.cpuUsage.toFloat(),
                        barColor = ResourceCpu,
                    )

                    // RAM
                    DetailResourceBar(
                        icon = Icons.Outlined.Memory,
                        label = "RAM",
                        valueText = "${formatBytes(n.memUsed)} / ${formatBytes(n.memTotal)}",
                        fraction = if (n.memTotal > 0) n.memUsed.toFloat() / n.memTotal else 0f,
                        barColor = ResourceRam,
                    )

                    // Swap
                    DetailResourceBar(
                        icon = Icons.Outlined.Memory,
                        label = "Swap",
                        valueText = "${formatBytes(n.swapUsed)} / ${formatBytes(n.swapTotal)}",
                        fraction = if (n.swapTotal > 0) n.swapUsed.toFloat() / n.swapTotal else 0f,
                        barColor = ResourceRam.copy(alpha = 0.7f),
                    )

                    // HD
                    DetailResourceBar(
                        icon = Icons.Outlined.Storage,
                        label = "HD",
                        valueText = "${formatBytes(n.diskUsed)} / ${formatBytes(n.diskTotal)}",
                        fraction = if (n.diskTotal > 0) n.diskUsed.toFloat() / n.diskTotal else 0f,
                        barColor = ResourceDisk,
                    )

                    // IO Delay
                    n.ioDelay?.let {
                        DetailResourceBar(
                            icon = Icons.Outlined.Speed,
                            label = "IO Delay",
                            valueText = "%.1f%%".format(it * 100),
                            fraction = it.toFloat(),
                            barColor = ResourceDisk.copy(alpha = 0.7f),
                        )
                    }
                }
            }

            // ---- ACTIONS section ----
            DetailSectionHeader("ACTIONS")
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
            ) {
                Column {
                    DetailActionRow(Icons.Outlined.Terminal, stringResource(R.string.console_title), onClick = onConsole)
                    DetailActionRow(Icons.Outlined.Storage, "Storage", onClick = onStorage)
                    DetailActionRow(
                        Icons.Outlined.SystemUpdate,
                        stringResource(R.string.nav_apt_updates),
                        onClick = onApt,
                    )
                    DetailActionRow(
                        Icons.Outlined.Storage,
                        stringResource(R.string.nav_disks),
                        onClick = onOpenDisks,
                    )
                    DetailActionRow(
                        Icons.Outlined.Lan,
                        stringResource(R.string.nav_network),
                        onClick = onOpenNetwork,
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    )

                    DetailActionRow(
                        Icons.Outlined.Refresh,
                        "Reboot",
                        onClick = { viewModel.nodeAction("reboot") },
                    )
                    DetailActionRow(
                        Icons.Outlined.PowerSettingsNew,
                        "Shutdown",
                        tint = MaterialTheme.colorScheme.error,
                        onClick = { viewModel.nodeAction("shutdown") },
                    )
                }
            }
        }

        // Bottom spacing
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ChartsTab(
    node: Node?,
    rrd: List<RrdPoint>,
    timeframe: RrdTimeframe,
    onTimeframeChange: (RrdTimeframe) -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        DetailSectionHeader("CHARTS")

        // Timeframe filter chips
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RrdTimeframe.entries.take(4).forEach { t ->
                FilterChip(
                    selected = t == timeframe,
                    onClick = { onTimeframeChange(t) },
                    label = {
                        Text(
                            stringResource(
                                when (t) {
                                    RrdTimeframe.HOUR -> R.string.timeframe_1h
                                    RrdTimeframe.DAY -> R.string.timeframe_24h
                                    RrdTimeframe.WEEK -> R.string.timeframe_7d
                                    RrdTimeframe.MONTH -> R.string.timeframe_30d
                                    else -> R.string.timeframe_1h
                                },
                            ),
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Chart cards
        val cpu = rrd.mapNotNull { it.cpu }
        val mem = rrd.mapNotNull { it.memUsed }
        val net = rrd.mapNotNull { it.netIn }
        val ioWait = rrd.mapNotNull { it.ioWait }

        Column(
            Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ChartCard(
                Icons.Outlined.Speed,
                "CPU",
                "${((cpu.lastOrNull() ?: 0.0) * 100).toInt()}%",
                values = cpu,
            )
            ChartCard(
                Icons.Outlined.Memory,
                "Memory",
                formatBytes((mem.lastOrNull() ?: 0.0).toLong()),
                secondaryValue = node?.let { "of ${formatBytes(it.memTotal)}" },
                values = mem,
            )
            ChartCard(
                Icons.Outlined.NetworkCheck,
                "Network",
                "${formatBytes((net.lastOrNull() ?: 0.0).toLong())}/s",
                values = net,
            )
            ChartCard(
                Icons.Outlined.Storage,
                "IO Delay",
                "${((ioWait.lastOrNull() ?: 0.0) * 100).toInt()}%",
                values = ioWait,
            )
        }

        // Bottom spacing
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun TasksTab(state: NodeDetailUiState, onOpenTask: (String, String) -> Unit) {
    val df = remember { java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT, java.text.DateFormat.SHORT) }
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (state.tasks.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.History, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                        Spacer(Modifier.height(12.dp))
                        Text(stringResource(R.string.node_no_recent_tasks), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        items(state.tasks, key = { "task_${it.upid}" }) { task ->
            val tone = when (task.state) {
                de.kiefer_networks.proxmoxopen.domain.model.TaskState.RUNNING -> de.kiefer_networks.proxmoxopen.core.ui.component.BadgeTone.Running
                de.kiefer_networks.proxmoxopen.domain.model.TaskState.OK -> de.kiefer_networks.proxmoxopen.core.ui.component.BadgeTone.Running
                de.kiefer_networks.proxmoxopen.domain.model.TaskState.FAILED -> de.kiefer_networks.proxmoxopen.core.ui.component.BadgeTone.Error
                de.kiefer_networks.proxmoxopen.domain.model.TaskState.UNKNOWN -> de.kiefer_networks.proxmoxopen.core.ui.component.BadgeTone.Neutral
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
                            Text("${task.user} \u00b7 ${df.format(java.util.Date(task.startTime * 1000))}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        de.kiefer_networks.proxmoxopen.core.ui.component.StatusBadge(task.state.name, tone)
                    }
                    task.exitStatus?.let {
                        Text(stringResource(R.string.task_exit_prefix, it), style = MaterialTheme.typography.bodySmall, color = if (task.state == de.kiefer_networks.proxmoxopen.domain.model.TaskState.FAILED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

