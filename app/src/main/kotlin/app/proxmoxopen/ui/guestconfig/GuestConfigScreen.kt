package app.proxmoxopen.ui.guestconfig

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
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
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
import app.proxmoxopen.R
import app.proxmoxopen.core.ui.component.ErrorState
import app.proxmoxopen.core.ui.component.HeroHeader
import app.proxmoxopen.core.ui.component.LoadingState
import app.proxmoxopen.core.ui.component.SectionLabel
import app.proxmoxopen.domain.model.NetworkInterface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuestConfigScreen(
    onBack: () -> Unit,
    viewModel: GuestConfigViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved) { if (state.saved) onBack() }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        stringResource(R.string.config_tab_general),
        stringResource(R.string.config_tab_resources),
        stringResource(R.string.config_tab_dns),
        stringResource(R.string.config_tab_network),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.edit_config_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        when {
            state.isLoading -> LoadingState(Modifier.padding(padding))
            state.error != null && state.config == null ->
                ErrorState(state.error?.message ?: "", stringResource(R.string.retry), viewModel::load, Modifier.padding(padding))
            else -> Column(
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
                // Hero
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    HeroHeader(
                        title = state.hostname.ifBlank { "CT ${viewModel.vmid}" },
                        subtitle = "${viewModel.node} · LXC ${viewModel.vmid}",
                        icon = Icons.Outlined.Settings,
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        tabs.forEachIndexed { index, label ->
                            SegmentedButton(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                shape = SegmentedButtonDefaults.itemShape(index, tabs.size),
                            ) { Text(label) }
                        }
                    }
                }

                // Tab content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    when (selectedTab) {
                        0 -> GeneralTab(state, viewModel)
                        1 -> ResourcesTab(state, viewModel)
                        2 -> DnsTab(state, viewModel)
                        3 -> NetworkTab(state, viewModel)
                    }
                }

                // Save button
                Button(
                    onClick = viewModel::save,
                    enabled = !state.isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    if (state.isSaving) CircularProgressIndicator(
                        modifier = Modifier.padding(end = 8.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Text(stringResource(R.string.save))
                }

                state.error?.let {
                    Text(
                        it.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}

// ---- General Tab ----

@Composable
private fun GeneralTab(state: GuestConfigUiState, vm: GuestConfigViewModel) {
    SectionLabel(stringResource(R.string.config_section_general))
    Field(state.hostname, vm::onHostname, R.string.config_hostname)
    Field(state.description, vm::onDescription, R.string.config_description, singleLine = false)
    ToggleRow(stringResource(R.string.config_onboot), state.onboot, vm::onOnboot)
    ToggleRow(stringResource(R.string.config_protection), state.protection, vm::onProtection)
    Field(state.startup, vm::onStartup, R.string.config_startup)
}

// ---- Resources Tab ----

@Composable
private fun ResourcesTab(state: GuestConfigUiState, vm: GuestConfigViewModel) {
    SectionLabel(stringResource(R.string.config_section_resources))
    NumericField(state.cores, vm::onCores, R.string.config_cores)
    Field(state.cpulimit, vm::onCpulimit, R.string.config_cpulimit)
    NumericField(state.memory, vm::onMemory, R.string.config_memory_mb)
    NumericField(state.swap, vm::onSwap, R.string.config_swap_mb)
}

// ---- DNS Tab ----

@Composable
private fun DnsTab(state: GuestConfigUiState, vm: GuestConfigViewModel) {
    SectionLabel(stringResource(R.string.config_section_dns))
    Field(state.nameserver, vm::onNameserver, R.string.config_nameserver)
    Text(
        stringResource(R.string.config_nameserver_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Field(state.searchdomain, vm::onSearchdomain, R.string.config_searchdomain)
}

// ---- Network Tab ----

@Composable
private fun NetworkTab(state: GuestConfigUiState, vm: GuestConfigViewModel) {
    SectionLabel(stringResource(R.string.config_section_network))
    if (state.nets.isEmpty()) {
        Text(
            stringResource(R.string.config_no_interfaces),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    state.nets.forEachIndexed { i, net ->
        NetworkCard(i, net, vm)
    }
}

@Composable
private fun NetworkCard(index: Int, net: NetworkInterface, vm: GuestConfigViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${net.id} — ${net.name ?: "veth"}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                net.bridge?.let {
                    Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            net.hwaddr?.let {
                Text("MAC: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            OutlinedTextField(
                value = net.ip ?: "",
                onValueChange = { vm.onNetField(index, NetField.IP, it) },
                label = { Text(stringResource(R.string.config_ip)) },
                placeholder = { Text("dhcp / 10.0.0.1/24") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = net.gw ?: "",
                onValueChange = { vm.onNetField(index, NetField.GW, it) },
                label = { Text(stringResource(R.string.config_gateway)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = net.ip6 ?: "",
                onValueChange = { vm.onNetField(index, NetField.IP6, it) },
                label = { Text(stringResource(R.string.config_ip6)) },
                placeholder = { Text("dhcp / auto / fd00::1/64") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = net.gw6 ?: "",
                onValueChange = { vm.onNetField(index, NetField.GW6, it) },
                label = { Text(stringResource(R.string.config_gateway6)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = net.mtu?.toString() ?: "",
                    onValueChange = { vm.onNetField(index, NetField.MTU, it) },
                    label = { Text("MTU") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = net.tag?.toString() ?: "",
                    onValueChange = { vm.onNetField(index, NetField.TAG, it) },
                    label = { Text("VLAN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = net.rate?.toString() ?: "",
                    onValueChange = { vm.onNetField(index, NetField.RATE, it) },
                    label = { Text("Rate") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
            ToggleRow(stringResource(R.string.config_firewall), net.firewall) { vm.onNetFirewall(index, it) }
        }
    }
}

// ---- Helpers ----

@Composable
private fun Field(value: String, onChange: (String) -> Unit, labelRes: Int, singleLine: Boolean = true) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(stringResource(labelRes)) },
        singleLine = singleLine,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun NumericField(value: String, onChange: (String) -> Unit, labelRes: Int) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(stringResource(labelRes)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
