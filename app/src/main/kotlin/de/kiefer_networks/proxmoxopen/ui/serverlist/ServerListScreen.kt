package de.kiefer_networks.proxmoxopen.ui.serverlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.kiefer_networks.proxmoxopen.R
import de.kiefer_networks.proxmoxopen.domain.model.Realm
import de.kiefer_networks.proxmoxopen.domain.model.Server
import java.text.DateFormat
import java.util.Date

@Composable
fun ServerListScreen(
    onAddServer: () -> Unit,
    onOpenServer: (Server) -> Unit,
    onEditServer: (Long) -> Unit = {},
    viewModel: ServerListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    StatelessServerList(
        servers = state.servers,
        onAdd = onAddServer,
        onOpen = onOpenServer,
        onEdit = onEditServer,
        onDelete = viewModel::delete,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatelessServerList(
    servers: List<Server>,
    onAdd: () -> Unit,
    onOpen: (Server) -> Unit,
    onEdit: (Long) -> Unit = {},
    onDelete: (Server) -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.server_list_title),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text(stringResource(R.string.add_server)) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                onClick = onAdd,
            )
        },
    ) { padding ->
        if (servers.isEmpty()) {
            EmptyServers(padding)
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = padding.calculateTopPadding() + 4.dp,
                    bottom = padding.calculateBottomPadding() + 96.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(servers, key = { it.id }) { server ->
                    ServerCard(
                        server = server,
                        onOpen = { onOpen(server) },
                        onEdit = { onEdit(server.id) },
                        onDelete = { onDelete(server) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ServerCard(
    server: Server,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Dns,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            Column(
                modifier = Modifier
                    .padding(start = 16.dp)
                    .weight(1f),
            ) {
                Text(
                    text = server.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "${server.host}:${server.port}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = realmLabel(server.realm) + " · " + lastConnectedLabel(server.lastConnectedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = stringResource(R.string.edit_server_title),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.delete_server),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun realmLabel(realm: Realm): String = stringResource(
    when (realm) {
        Realm.PAM -> R.string.realm_pam
        Realm.PVE -> R.string.realm_pve
        Realm.PVE_TOKEN -> R.string.realm_pve_token
    },
)

@Composable
private fun lastConnectedLabel(timestamp: Long?): String {
    val format = remember0 { DateFormat.getDateInstance(DateFormat.SHORT) }
    return if (timestamp == null) {
        stringResource(R.string.never_connected)
    } else {
        stringResource(R.string.last_connected, format.format(Date(timestamp)))
    }
}

@Composable
private inline fun <T> remember0(crossinline calc: () -> T): T =
    androidx.compose.runtime.remember { calc() }

@Composable
private fun EmptyServers(padding: PaddingValues) {
    Box(
        Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Dns,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            Text(
                text = stringResource(R.string.server_list_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
