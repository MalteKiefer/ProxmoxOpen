package app.proxmoxopen.ui.guestdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import app.proxmoxopen.core.ui.component.ErrorState
import app.proxmoxopen.core.ui.component.LoadingState
import app.proxmoxopen.core.ui.component.MetricBar
import app.proxmoxopen.core.ui.component.PxoLineChart
import app.proxmoxopen.domain.model.Guest
import app.proxmoxopen.ui.power.PowerActionSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuestDetailScreen(
    onBack: () -> Unit,
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
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text(stringResource(R.string.power_start)) },
                icon = { Icon(Icons.Default.PowerSettingsNew, contentDescription = null) },
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
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                state.guest?.let { guest -> GuestCard(guest) }
                val cpuSeries = state.rrd.mapNotNull { it.cpu }
                if (cpuSeries.isNotEmpty()) {
                    Text(stringResource(R.string.metric_cpu), style = MaterialTheme.typography.titleSmall)
                    PxoLineChart(values = cpuSeries)
                }
                state.actionMessage?.let { Text(it) }
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
private fun GuestCard(guest: Guest) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(guest.name, style = MaterialTheme.typography.titleLarge)
            Text(
                "${guest.node} · ${stringResource(R.string.vm_id, guest.vmid)} · ${guest.status.name}",
                style = MaterialTheme.typography.labelMedium,
            )
            MetricBar(
                label = stringResource(R.string.metric_cpu),
                value = guest.cpuUsage.toFloat(),
                caption = "${(guest.cpuUsage * 100).toInt()}%",
            )
            val memFrac = if (guest.memTotal > 0) guest.memUsed.toFloat() / guest.memTotal else 0f
            MetricBar(
                label = stringResource(R.string.metric_memory),
                value = memFrac,
                caption = "${guest.memUsed / (1 shl 20)} / ${guest.memTotal / (1 shl 20)} MiB",
            )
        }
    }
}
