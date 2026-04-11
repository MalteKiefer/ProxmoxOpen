package app.proxmoxopen.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.proxmoxopen.R
import app.proxmoxopen.core.ui.component.ErrorState
import app.proxmoxopen.core.ui.component.MetricBar
import app.proxmoxopen.domain.model.Guest
import app.proxmoxopen.domain.model.GuestStatus
import app.proxmoxopen.domain.model.GuestType
import app.proxmoxopen.domain.model.Node

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onBack: () -> Unit,
    onOpenNode: (String) -> Unit,
    onOpenGuest: (Guest) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.serverName.ifBlank { stringResource(R.string.dashboard_title) }) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(stringResource(R.string.tab_nodes)) },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(stringResource(R.string.tab_vms)) },
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text(stringResource(R.string.tab_containers)) },
                )
            }
            when {
                state.isLoading && state.cluster == null -> Loading()
                state.error != null && state.cluster == null ->
                    ErrorState(
                        message = state.error?.message ?: "error",
                        retryLabel = stringResource(R.string.retry),
                        onRetry = viewModel::refresh,
                    )
                else -> when (selectedTab) {
                    0 -> NodesList(state.cluster?.nodes.orEmpty(), onOpenNode)
                    1 -> GuestList(state.guests.filter { it.type == GuestType.QEMU }, onOpenGuest)
                    2 -> GuestList(state.guests.filter { it.type == GuestType.LXC }, onOpenGuest)
                    else -> Unit
                }
            }
        }
    }
}

@Composable
private fun Loading() {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun NodesList(nodes: List<Node>, onOpen: (String) -> Unit) {
    if (nodes.isEmpty()) {
        Text(stringResource(R.string.server_list_empty), Modifier.padding(16.dp))
        return
    }
    LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
        items(nodes, key = { it.name }) { node ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onOpen(node.name) },
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(node.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        nodeStatusLabel(node),
                        style = MaterialTheme.typography.labelMedium,
                    )
                    MetricBar(
                        label = stringResource(R.string.metric_cpu),
                        value = node.cpuUsage.toFloat(),
                        caption = "${(node.cpuUsage * 100).toInt()}% of ${node.cpuCount} cores",
                    )
                    val memFrac = if (node.memTotal > 0) {
                        node.memUsed.toFloat() / node.memTotal.toFloat()
                    } else {
                        0f
                    }
                    MetricBar(
                        label = stringResource(R.string.metric_memory),
                        value = memFrac,
                        caption = "${formatBytes(node.memUsed)} / ${formatBytes(node.memTotal)}",
                    )
                }
            }
        }
    }
}

@Composable
private fun nodeStatusLabel(node: Node): String = stringResource(
    when (node.status) {
        app.proxmoxopen.domain.model.NodeStatus.ONLINE -> R.string.node_status_online
        app.proxmoxopen.domain.model.NodeStatus.OFFLINE -> R.string.node_status_offline
        app.proxmoxopen.domain.model.NodeStatus.UNKNOWN -> R.string.node_status_unknown
    },
)

@Composable
private fun GuestList(guests: List<Guest>, onOpen: (Guest) -> Unit) {
    if (guests.isEmpty()) {
        Text(stringResource(R.string.server_list_empty), Modifier.padding(16.dp))
        return
    }
    LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
        items(guests, key = { "${it.node}/${it.type}/${it.vmid}" }) { guest ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onOpen(guest) },
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(guest.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${guest.node} · ${stringResource(R.string.vm_id, guest.vmid)} · ${guestStatusLabel(guest.status)}",
                        style = MaterialTheme.typography.labelMedium,
                    )
                    if (guest.status == GuestStatus.RUNNING) {
                        MetricBar(
                            label = stringResource(R.string.metric_cpu),
                            value = guest.cpuUsage.toFloat(),
                            caption = "${(guest.cpuUsage * 100).toInt()}%",
                        )
                        val memFrac = if (guest.memTotal > 0) {
                            guest.memUsed.toFloat() / guest.memTotal.toFloat()
                        } else {
                            0f
                        }
                        MetricBar(
                            label = stringResource(R.string.metric_memory),
                            value = memFrac,
                            caption = "${formatBytes(guest.memUsed)} / ${formatBytes(guest.memTotal)}",
                        )
                    }
                }
            }
        }
    }
}

private fun guestStatusLabel(status: GuestStatus): String = status.name.lowercase()

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KiB", "MiB", "GiB", "TiB")
    var value = bytes.toDouble()
    var i = 0
    while (value >= 1024 && i < units.lastIndex) {
        value /= 1024
        i++
    }
    return "%.1f %s".format(value, units[i])
}
