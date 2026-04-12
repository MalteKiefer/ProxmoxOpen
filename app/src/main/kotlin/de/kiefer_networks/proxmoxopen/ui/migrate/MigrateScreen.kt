package de.kiefer_networks.proxmoxopen.ui.migrate

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import de.kiefer_networks.proxmoxopen.R
import de.kiefer_networks.proxmoxopen.core.ui.component.ErrorState
import de.kiefer_networks.proxmoxopen.core.ui.component.LoadingState
import de.kiefer_networks.proxmoxopen.domain.model.GuestType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MigrateScreen(
    onBack: () -> Unit,
    viewModel: MigrateViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val typeLabel = if (viewModel.type == GuestType.QEMU) "VM" else "CT"

    LaunchedEffect(state.success) {
        if (state.success) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.migrate_title, typeLabel, viewModel.vmid),
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
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
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when {
                state.isLoading -> LoadingState()
                state.error != null && state.nodes.isEmpty() -> ErrorState(
                    state.error?.message ?: "",
                    stringResource(R.string.retry),
                    {},
                )
                state.nodes.isEmpty() -> {
                    Text(
                        "No other nodes available for migration.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                else -> {
                    // Target node dropdown
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded },
                    ) {
                        OutlinedTextField(
                            value = state.selectedNode ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.migrate_target_node)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                        ) {
                            state.nodes.forEach { nodeName ->
                                DropdownMenuItem(
                                    text = { Text(nodeName) },
                                    onClick = {
                                        viewModel.selectNode(nodeName)
                                        expanded = false
                                    },
                                )
                            }
                        }
                    }

                    // Online migration checkbox
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setOnline(!state.online) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = state.online,
                            onCheckedChange = { viewModel.setOnline(it) },
                        )
                        Text(
                            stringResource(R.string.migrate_online),
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Migrate button
                    Button(
                        onClick = viewModel::migrate,
                        enabled = state.selectedNode != null && !state.isMigrating,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (state.isMigrating) {
                            CircularProgressIndicator(
                                modifier = Modifier.height(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(stringResource(R.string.migrate_button))
                        }
                    }

                    // Message
                    state.message?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (state.error != null) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
