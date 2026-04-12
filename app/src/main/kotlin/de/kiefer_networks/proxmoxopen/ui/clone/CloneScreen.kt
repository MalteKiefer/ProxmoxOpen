package de.kiefer_networks.proxmoxopen.ui.clone

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.kiefer_networks.proxmoxopen.R
import de.kiefer_networks.proxmoxopen.core.ui.component.LoadingState
import de.kiefer_networks.proxmoxopen.domain.model.GuestType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloneScreen(
    onBack: () -> Unit,
    viewModel: CloneViewModel = hiltViewModel(),
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
                        stringResource(R.string.clone_title, typeLabel, viewModel.vmid),
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (state.isLoading) {
                LoadingState()
                return@Column
            }

            // New VMID
            OutlinedTextField(
                value = state.newId,
                onValueChange = viewModel::setNewId,
                label = { Text(stringResource(R.string.clone_new_id)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            // Name
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::setName,
                label = { Text(stringResource(R.string.clone_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            // Full clone checkbox
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.setFullClone(!state.fullClone) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = state.fullClone,
                    onCheckedChange = { viewModel.setFullClone(it) },
                )
                Text(
                    stringResource(R.string.clone_full),
                    modifier = Modifier.padding(start = 4.dp),
                )
            }

            // Target node dropdown (optional)
            if (state.nodes.isNotEmpty()) {
                var nodeExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = nodeExpanded,
                    onExpandedChange = { nodeExpanded = !nodeExpanded },
                ) {
                    OutlinedTextField(
                        value = state.targetNode ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.clone_target_node)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(nodeExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    )
                    ExposedDropdownMenu(
                        expanded = nodeExpanded,
                        onDismissRequest = { nodeExpanded = false },
                    ) {
                        // Empty option to clear selection
                        DropdownMenuItem(
                            text = { Text("(same node)") },
                            onClick = {
                                viewModel.setTargetNode(null)
                                nodeExpanded = false
                            },
                        )
                        state.nodes.forEach { nodeName ->
                            DropdownMenuItem(
                                text = { Text(nodeName) },
                                onClick = {
                                    viewModel.setTargetNode(nodeName)
                                    nodeExpanded = false
                                },
                            )
                        }
                    }
                }
            }

            // Target storage dropdown (optional)
            if (state.storages.isNotEmpty()) {
                var storageExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = storageExpanded,
                    onExpandedChange = { storageExpanded = !storageExpanded },
                ) {
                    OutlinedTextField(
                        value = state.targetStorage ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.clone_storage)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(storageExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    )
                    ExposedDropdownMenu(
                        expanded = storageExpanded,
                        onDismissRequest = { storageExpanded = false },
                    ) {
                        // Empty option to clear selection
                        DropdownMenuItem(
                            text = { Text("(same storage)") },
                            onClick = {
                                viewModel.setTargetStorage(null)
                                storageExpanded = false
                            },
                        )
                        state.storages.forEach { storageName ->
                            DropdownMenuItem(
                                text = { Text(storageName) },
                                onClick = {
                                    viewModel.setTargetStorage(storageName)
                                    storageExpanded = false
                                },
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Clone button
            Button(
                onClick = viewModel::clone,
                enabled = state.newId.toIntOrNull() != null && !state.isCloning,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.isCloning) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(stringResource(R.string.clone_button))
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
