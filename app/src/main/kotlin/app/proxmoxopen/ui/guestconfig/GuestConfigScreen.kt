package app.proxmoxopen.ui.guestconfig

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
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.proxmoxopen.R
import app.proxmoxopen.core.ui.component.ErrorState
import app.proxmoxopen.core.ui.component.HeroHeader
import app.proxmoxopen.core.ui.component.LoadingState
import app.proxmoxopen.core.ui.component.SectionLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuestConfigScreen(
    onBack: () -> Unit,
    viewModel: GuestConfigViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved) { if (state.saved) onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.edit_config_title)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        when {
            state.isLoading -> LoadingState(Modifier.padding(padding))
            state.error != null && state.config == null ->
                ErrorState(state.error?.message ?: "", stringResource(R.string.retry), viewModel::load, Modifier.padding(padding))
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                HeroHeader(
                    title = state.name.ifBlank { "VM ${viewModel.vmid}" },
                    subtitle = "${viewModel.node} · ${viewModel.type.apiPath} ${viewModel.vmid}",
                    icon = Icons.Outlined.Settings,
                )

                SectionLabel(stringResource(R.string.config_section_general))
                OutlinedTextField(value = state.name, onValueChange = viewModel::onName, label = { Text(stringResource(R.string.config_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (viewModel.type == app.proxmoxopen.domain.model.GuestType.LXC) {
                    OutlinedTextField(value = state.hostname, onValueChange = viewModel::onHostname, label = { Text(stringResource(R.string.config_hostname)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.config_onboot), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Switch(checked = state.onboot, onCheckedChange = viewModel::onOnboot)
                }

                SectionLabel(stringResource(R.string.config_section_dns))
                OutlinedTextField(value = state.nameserver, onValueChange = viewModel::onNameserver, label = { Text(stringResource(R.string.config_nameserver)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = state.searchdomain, onValueChange = viewModel::onSearchdomain, label = { Text(stringResource(R.string.config_searchdomain)) }, singleLine = true, modifier = Modifier.fillMaxWidth())

                if (state.nets.isNotEmpty()) {
                    SectionLabel(stringResource(R.string.config_section_network))
                    state.nets.forEachIndexed { i, net ->
                        Text("${net.id} (${net.bridge ?: "—"})", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        OutlinedTextField(value = net.ip ?: "", onValueChange = { viewModel.onNetIp(i, it) }, label = { Text(stringResource(R.string.config_ip)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = net.gw ?: "", onValueChange = { viewModel.onNetGw(i, it) }, label = { Text(stringResource(R.string.config_gateway)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        if (net.hwaddr != null) {
                            Text("MAC: ${net.hwaddr}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Button(
                    onClick = viewModel::save,
                    enabled = !state.isSaving,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) {
                    if (state.isSaving) CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    Text(stringResource(R.string.save))
                }

                state.error?.let { Text(it.message, color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}
