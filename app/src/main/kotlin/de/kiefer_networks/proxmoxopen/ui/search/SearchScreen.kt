package de.kiefer_networks.proxmoxopen.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.text.KeyboardOptions
import de.kiefer_networks.proxmoxopen.R
import de.kiefer_networks.proxmoxopen.core.ui.component.ErrorState
import de.kiefer_networks.proxmoxopen.core.ui.component.LoadingState
import de.kiefer_networks.proxmoxopen.core.ui.theme.StatusError
import de.kiefer_networks.proxmoxopen.core.ui.theme.StatusPaused
import de.kiefer_networks.proxmoxopen.core.ui.theme.StatusRunning
import de.kiefer_networks.proxmoxopen.core.ui.theme.StatusStopped
import de.kiefer_networks.proxmoxopen.domain.model.GuestStatus
import de.kiefer_networks.proxmoxopen.domain.model.GuestType
import de.kiefer_networks.proxmoxopen.domain.model.SearchResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onOpenGuest: (node: String, vmid: Int, type: String) -> Unit,
    onOpenNode: (String) -> Unit,
    onOpenStorage: (String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = state.query,
                        onValueChange = viewModel::onQueryChange,
                        placeholder = {
                            Text(
                                stringResource(R.string.search_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        textStyle = MaterialTheme.typography.bodyLarge,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                        ),
                        trailingIcon = {
                            if (state.query.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onQueryChange("") }) {
                                    Icon(
                                        Icons.Outlined.Close,
                                        contentDescription = "Clear",
                                    )
                                }
                            }
                        },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.search_title),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when {
                state.isLoading && state.results.isEmpty() -> LoadingState()
                state.error != null && state.results.isEmpty() ->
                    ErrorState(
                        message = state.error?.message ?: "",
                        retryLabel = stringResource(R.string.retry),
                        onRetry = { viewModel.onQueryChange(state.query) },
                    )
                state.query.isBlank() -> EmptyState(
                    icon = Icons.Outlined.Search,
                    message = stringResource(R.string.search_empty),
                )
                state.results.isEmpty() -> EmptyState(
                    icon = Icons.Outlined.Search,
                    message = stringResource(R.string.search_no_results),
                )
                else -> ResultsList(
                    results = state.results,
                    onOpenGuest = onOpenGuest,
                    onOpenNode = onOpenNode,
                    onOpenStorage = onOpenStorage,
                )
            }
        }
    }
}

@Composable
private fun ResultsList(
    results: List<SearchResult>,
    onOpenGuest: (node: String, vmid: Int, type: String) -> Unit,
    onOpenNode: (String) -> Unit,
    onOpenStorage: (String) -> Unit,
) {
    val vms = results.filterIsInstance<SearchResult.GuestResult>()
        .filter { it.type == GuestType.QEMU }
    val lxcs = results.filterIsInstance<SearchResult.GuestResult>()
        .filter { it.type == GuestType.LXC }
    val nodes = results.filterIsInstance<SearchResult.NodeResult>()
    val storages = results.filterIsInstance<SearchResult.StorageResult>()

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (vms.isNotEmpty()) {
            item { SectionHeader(stringResource(R.string.search_type_vm)) }
            items(vms, key = { it.id }) { g ->
                GuestResultCard(g) { onOpenGuest(g.node, g.vmid, g.type.apiPath) }
            }
        }
        if (lxcs.isNotEmpty()) {
            item { SectionHeader(stringResource(R.string.search_type_lxc)) }
            items(lxcs, key = { it.id }) { g ->
                GuestResultCard(g) { onOpenGuest(g.node, g.vmid, g.type.apiPath) }
            }
        }
        if (nodes.isNotEmpty()) {
            item { SectionHeader(stringResource(R.string.search_type_node)) }
            items(nodes, key = { it.id }) { n ->
                NodeResultCard(n) { onOpenNode(n.node) }
            }
        }
        if (storages.isNotEmpty()) {
            item { SectionHeader(stringResource(R.string.search_type_storage)) }
            items(storages, key = { it.id }) { s ->
                StorageResultCard(s) { onOpenStorage(s.node) }
            }
        }
    }
}

@Composable
private fun SectionHeader(label: String) {
    Text(
        text = label.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 12.dp, bottom = 2.dp),
    )
}

@Composable
private fun GuestResultCard(guest: SearchResult.GuestResult, onClick: () -> Unit) {
    val statusColor = when (guest.status) {
        GuestStatus.RUNNING -> StatusRunning
        GuestStatus.STOPPED -> StatusError
        GuestStatus.PAUSED, GuestStatus.SUSPENDED -> StatusPaused
        GuestStatus.UNKNOWN -> StatusStopped
    }
    val icon = if (guest.type == GuestType.QEMU) Icons.Outlined.Computer else Icons.Outlined.Inventory2
    ResultCard(
        icon = icon,
        iconTint = MaterialTheme.colorScheme.primary,
        onClick = onClick,
        primary = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${guest.vmid}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = guest.name,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        },
        secondary = {
            val statusLabel = guest.status.name.lowercase().replaceFirstChar { it.uppercase() }
            val tagPart = if (guest.tags.isNotEmpty()) "  •  ${guest.tags.joinToString(", ")}" else ""
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(statusColor, CircleShape),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "${guest.node}  •  $statusLabel$tagPart",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}

@Composable
private fun NodeResultCard(node: SearchResult.NodeResult, onClick: () -> Unit) {
    ResultCard(
        icon = Icons.Outlined.Storage,
        iconTint = MaterialTheme.colorScheme.primary,
        onClick = onClick,
        primary = {
            Text(
                text = node.node,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
        },
        secondary = {
            Text(
                text = node.status ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
    )
}

@Composable
private fun StorageResultCard(storage: SearchResult.StorageResult, onClick: () -> Unit) {
    ResultCard(
        icon = Icons.Outlined.Storage,
        iconTint = MaterialTheme.colorScheme.primary,
        onClick = onClick,
        primary = {
            Text(
                text = storage.storage,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
        },
        secondary = {
            val parts = listOfNotNull(
                storage.node,
                storage.status,
                storage.content,
            )
            Text(
                text = parts.joinToString("  •  "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
    )
}

@Composable
private fun ResultCard(
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit,
    primary: @Composable () -> Unit,
    secondary: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                primary()
                Spacer(Modifier.height(2.dp))
                secondary()
            }
        }
    }
}

@Composable
private fun EmptyState(icon: ImageVector, message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
