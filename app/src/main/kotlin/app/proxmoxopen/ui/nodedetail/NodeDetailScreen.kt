package app.proxmoxopen.ui.nodedetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.NetworkCheck
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
import app.proxmoxopen.core.ui.component.MetricCard
import app.proxmoxopen.core.ui.component.PxoLineChart
import app.proxmoxopen.core.ui.component.SectionLabel
import app.proxmoxopen.core.ui.component.StatusBadge
import app.proxmoxopen.domain.model.NodeStatus
import app.proxmoxopen.domain.model.RrdTimeframe
import app.proxmoxopen.ui.format.formatBytes
import app.proxmoxopen.ui.format.formatUptime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NodeDetailScreen(
    onBack: () -> Unit,
    viewModel: NodeDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(viewModel.nodeName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        when {
            state.isLoading && state.node == null -> LoadingState(Modifier.padding(padding))
            state.error != null && state.node == null ->
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
                state.node?.let { node ->
                    val tone = when (node.status) {
                        NodeStatus.ONLINE -> BadgeTone.Running
                        NodeStatus.OFFLINE -> BadgeTone.Error
                        NodeStatus.UNKNOWN -> BadgeTone.Neutral
                    }
                    HeroHeader(
                        title = node.name,
                        subtitle = "${node.cpuCount} cores · ${formatBytes(node.memTotal)} RAM",
                        icon = Icons.Outlined.Storage,
                        trailing = {
                            StatusBadge(
                                label = node.status.name.lowercase().replaceFirstChar { it.titlecase() },
                                tone = tone,
                            )
                        },
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MetricCard(
                            icon = Icons.Outlined.Memory,
                            label = stringResource(R.string.metric_cpu),
                            primaryValue = "${(node.cpuUsage * 100).toInt()}%",
                            secondaryValue = "${node.cpuCount} cores",
                            progress = node.cpuUsage.toFloat(),
                            modifier = Modifier.weight(1f),
                        )
                        val memFraction = if (node.memTotal > 0) {
                            node.memUsed.toFloat() / node.memTotal.toFloat()
                        } else {
                            0f
                        }
                        MetricCard(
                            icon = Icons.Outlined.Storage,
                            label = stringResource(R.string.metric_memory),
                            primaryValue = formatBytes(node.memUsed),
                            secondaryValue = "of ${formatBytes(node.memTotal)}",
                            progress = memFraction,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        val diskFraction = if (node.diskTotal > 0) {
                            node.diskUsed.toFloat() / node.diskTotal.toFloat()
                        } else {
                            0f
                        }
                        MetricCard(
                            icon = Icons.Outlined.Storage,
                            label = stringResource(R.string.metric_disk),
                            primaryValue = formatBytes(node.diskUsed),
                            secondaryValue = "of ${formatBytes(node.diskTotal)}",
                            progress = diskFraction,
                            modifier = Modifier.weight(1f),
                        )
                        MetricCard(
                            icon = Icons.Outlined.Schedule,
                            label = stringResource(R.string.metric_uptime),
                            primaryValue = formatUptime(node.uptimeSeconds),
                            secondaryValue = node.loadAverage.joinToString(" ") { "%.2f".format(it) }
                                .ifBlank { null },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                SectionLabel(stringResource(R.string.metric_cpu))
                TimeframeRow(state.timeframe, viewModel::setTimeframe)
                ChartCard {
                    val cpuSeries = state.rrd.mapNotNull { it.cpu }
                    PxoLineChart(values = cpuSeries)
                }

                SectionLabel(stringResource(R.string.metric_network))
                ChartCard {
                    val netSeries = state.rrd.mapNotNull { it.netIn }
                    PxoLineChart(
                        values = netSeries,
                        valueFormatter = { v, _, _ -> formatBytes(v.toLong()) + "/s" },
                    )
                }
            }
        }
    }
}

@Composable
private fun ChartCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeframeRow(current: RrdTimeframe, onSelect: (RrdTimeframe) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RrdTimeframe.entries.take(4).forEach { tf ->
            FilterChip(
                selected = tf == current,
                onClick = { onSelect(tf) },
                label = {
                    Text(
                        text = when (tf) {
                            RrdTimeframe.HOUR -> stringResource(R.string.timeframe_1h)
                            RrdTimeframe.DAY -> stringResource(R.string.timeframe_24h)
                            RrdTimeframe.WEEK -> stringResource(R.string.timeframe_7d)
                            RrdTimeframe.MONTH -> stringResource(R.string.timeframe_30d)
                            else -> tf.name
                        },
                        fontWeight = FontWeight.Medium,
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        }
    }
}
