package de.kiefer_networks.proxmoxopen.ui.guestconfig

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.kiefer_networks.proxmoxopen.R
import de.kiefer_networks.proxmoxopen.core.ui.component.ErrorState
import de.kiefer_networks.proxmoxopen.core.ui.component.LoadingState
import de.kiefer_networks.proxmoxopen.domain.model.NetworkInterface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuestConfigScreen(
    onBack: () -> Unit,
    viewModel: GuestConfigViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved) { if (state.saved) onBack() }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("General", "Resources", "DNS", "Network", stringResource(R.string.config_options), stringResource(R.string.config_disks))

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.edit_config_title), style = MaterialTheme.typography.titleMedium)
                        Text("CT ${viewModel.vmid} · ${viewModel.node}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        when {
            state.isLoading -> LoadingState(Modifier.padding(padding))
            state.error != null && state.config == null -> ErrorState(state.error?.message ?: "", stringResource(R.string.retry), viewModel::load, Modifier.padding(padding))
            else -> Column(Modifier.fillMaxSize().padding(padding)) {
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.background,
                    edgePadding = 16.dp,
                ) {
                    tabs.forEachIndexed { i, label ->
                        Tab(selected = selectedTab == i, onClick = { selectedTab = i }, text = { Text(label, style = MaterialTheme.typography.labelLarge) })
                    }
                }

                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    when (selectedTab) {
                        0 -> GeneralTab(state, viewModel)
                        1 -> ResourcesTab(state, viewModel)
                        2 -> DnsTab(state, viewModel)
                        3 -> NetworkTab(state, viewModel)
                        4 -> OptionsTab(state)
                        5 -> DisksTab(state)
                    }
                }

                Button(
                    onClick = viewModel::save,
                    enabled = !state.isSaving,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    if (state.isSaving) CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    Text(stringResource(R.string.save))
                }
                state.error?.let { Text(it.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) }
            }
        }
    }
}

// ---- General ----

@Composable
private fun GeneralTab(state: GuestConfigUiState, vm: GuestConfigViewModel) {
    Compact(state.hostname, vm::onHostname, "Hostname")
    Compact(state.description, vm::onDescription, stringResource(R.string.config_description), singleLine = false)
    Compact(state.tags, vm::onTags, stringResource(R.string.tags_hint))
    Toggle(stringResource(R.string.config_onboot), state.onboot, vm::onOnboot)
    Toggle(stringResource(R.string.config_protection), state.protection, vm::onProtection)
    Compact(state.startup, vm::onStartup, stringResource(R.string.config_startup))
    state.config?.arch?.let { ReadOnlyRow("Architecture", it) }
    state.config?.ostype?.let { ReadOnlyRow("OS Type", it) }
    state.config?.unprivileged?.let { ReadOnlyRow("Unprivileged", if (it) "Yes" else "No") }
}

// ---- Resources ----

@Composable
private fun ResourcesTab(state: GuestConfigUiState, vm: GuestConfigViewModel) {
    StopHint()
    Numeric(state.cores, vm::onCores, stringResource(R.string.config_cores))
    Compact(state.cpulimit, vm::onCpulimit, stringResource(R.string.config_cpulimit))
    Numeric(state.memory, vm::onMemory, stringResource(R.string.config_memory_mb))
    Numeric(state.swap, vm::onSwap, stringResource(R.string.config_swap_mb))
}

// ---- DNS ----

@Composable
private fun DnsTab(state: GuestConfigUiState, vm: GuestConfigViewModel) {
    Compact(state.nameserver, vm::onNameserver, stringResource(R.string.config_nameserver))
    Text(stringResource(R.string.config_nameserver_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Compact(state.searchdomain, vm::onSearchdomain, stringResource(R.string.config_searchdomain))
}

// ---- Network ----

@Composable
private fun NetworkTab(state: GuestConfigUiState, vm: GuestConfigViewModel) {
    if (state.nets.isEmpty()) {
        Text(stringResource(R.string.config_no_interfaces), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    state.nets.forEachIndexed { i, net ->
        NetCard(i, net, vm, canDelete = state.nets.size > 1)
    }
    OutlinedButton(onClick = { vm.addNetInterface() }, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Outlined.Add, contentDescription = "Add network interface", modifier = Modifier.padding(end = 8.dp))
        Text(stringResource(R.string.config_add_interface))
    }
}

@Composable
private fun NetCard(index: Int, net: NetworkInterface, vm: GuestConfigViewModel, canDelete: Boolean) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${net.id}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                net.bridge?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                if (canDelete) IconButton(onClick = { vm.deleteNetInterface(index) }) { Icon(Icons.Outlined.Delete, contentDescription = "Delete network interface", tint = MaterialTheme.colorScheme.error, modifier = Modifier.padding(0.dp)) }
            }
            net.hwaddr?.let { Text("MAC: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Compact(net.ip ?: "", { vm.onNetField(index, NetField.IP, it) }, "IPv4 (CIDR/dhcp)")
            Compact(net.gw ?: "", { vm.onNetField(index, NetField.GW, it) }, "Gateway")
            Compact(net.ip6 ?: "", { vm.onNetField(index, NetField.IP6, it) }, "IPv6")
            Compact(net.gw6 ?: "", { vm.onNetField(index, NetField.GW6, it) }, "Gateway6")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Numeric(net.mtu?.toString() ?: "", { vm.onNetField(index, NetField.MTU, it) }, "MTU", Modifier.weight(1f))
                Numeric(net.tag?.toString() ?: "", { vm.onNetField(index, NetField.TAG, it) }, "VLAN", Modifier.weight(1f))
            }
            Toggle(stringResource(R.string.config_firewall), net.firewall) { vm.onNetFirewall(index, it) }
        }
    }
}

// ---- Options (read-only display for now) ----

@Composable
private fun OptionsTab(state: GuestConfigUiState) {
    Text(stringResource(R.string.config_options), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    state.config?.let { c ->
        ReadOnlyRow("Start at boot", if (c.onboot) "Yes" else "No")
        ReadOnlyRow("Start/Shutdown order", c.startup ?: "order=any")
        ReadOnlyRow("OS Type", c.ostype ?: "—")
        ReadOnlyRow("Architecture", c.arch ?: "amd64")
        ReadOnlyRow("Protection", if (c.protection) "Yes" else "No")
        ReadOnlyRow("Unprivileged", if (c.unprivileged) "Yes" else "No")
        ReadOnlyRow("Features", c.features ?: "none")
    }
    TodoHint(stringResource(R.string.config_options_todo))
}

// ---- Disks (placeholder) ----

@Composable
private fun DisksTab(state: GuestConfigUiState) {
    Text(stringResource(R.string.config_disks), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    TodoHint(stringResource(R.string.config_disks_todo))
}

// ---- Helpers ----

@Composable
private fun Compact(value: String, onChange: (String) -> Unit, label: String, singleLine: Boolean = true, modifier: Modifier = Modifier) {
    OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label, style = MaterialTheme.typography.bodySmall) }, singleLine = singleLine, modifier = modifier.fillMaxWidth(), textStyle = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun Numeric(value: String, onChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    OutlinedTextField(value = value, onValueChange = { onChange(it.filter { c -> c.isDigit() || c == '.' }) }, label = { Text(label, style = MaterialTheme.typography.bodySmall) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = modifier.fillMaxWidth(), textStyle = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun Toggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ReadOnlyRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun StopHint() {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 8.dp))
        Text(stringResource(R.string.config_stop_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TodoHint(text: String) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)) {
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(12.dp))
    }
}
