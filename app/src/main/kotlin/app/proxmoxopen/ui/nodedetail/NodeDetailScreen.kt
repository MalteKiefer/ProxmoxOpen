package app.proxmoxopen.ui.nodedetail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.proxmoxopen.R
import app.proxmoxopen.core.ui.component.ErrorState
import app.proxmoxopen.core.ui.component.LoadingState
import app.proxmoxopen.core.ui.component.MetricBar
import app.proxmoxopen.core.ui.component.PxoLineChart
import app.proxmoxopen.domain.model.RrdTimeframe

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
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
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
                    .padding(16.dp),
            ) {
                state.node?.let { node ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(node.name, style = MaterialTheme.typography.titleLarge)
                            MetricBar(
                                label = stringResource(R.string.metric_cpu),
                                value = node.cpuUsage.toFloat(),
                                caption = "${(node.cpuUsage * 100).toInt()}%",
                            )
                            val memFrac = if (node.memTotal > 0) {
                                node.memUsed.toFloat() / node.memTotal.toFloat()
                            } else {
                                0f
                            }
                            MetricBar(
                                label = stringResource(R.string.metric_memory),
                                value = memFrac,
                                caption = "${node.memUsed / (1 shl 30)} / ${node.memTotal / (1 shl 30)} GiB",
                            )
                            val diskFrac = if (node.diskTotal > 0) {
                                node.diskUsed.toFloat() / node.diskTotal.toFloat()
                            } else {
                                0f
                            }
                            MetricBar(
                                label = stringResource(R.string.metric_disk),
                                value = diskFrac,
                                caption = "${node.diskUsed / (1 shl 30)} / ${node.diskTotal / (1 shl 30)} GiB",
                            )
                        }
                    }
                }

                TimeframeRow(state.timeframe, viewModel::setTimeframe)

                val cpuSeries = state.rrd.mapNotNull { it.cpu }
                if (cpuSeries.isNotEmpty()) {
                    Text(
                        stringResource(R.string.metric_cpu),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                    PxoLineChart(values = cpuSeries)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeframeRow(current: RrdTimeframe, onSelect: (RrdTimeframe) -> Unit) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
    ) {
        RrdTimeframe.entries.take(4).forEach { tf ->
            FilterChip(
                selected = tf == current,
                onClick = { onSelect(tf) },
                label = {
                    Text(
                        when (tf) {
                            RrdTimeframe.HOUR -> stringResource(R.string.timeframe_1h)
                            RrdTimeframe.DAY -> stringResource(R.string.timeframe_24h)
                            RrdTimeframe.WEEK -> stringResource(R.string.timeframe_7d)
                            RrdTimeframe.MONTH -> stringResource(R.string.timeframe_30d)
                            else -> tf.name
                        },
                    )
                },
            )
        }
    }
}
