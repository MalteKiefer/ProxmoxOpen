package de.kiefer_networks.proxmoxopen.ui.ha

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.kiefer_networks.proxmoxopen.R
import de.kiefer_networks.proxmoxopen.core.ui.component.BadgeTone
import de.kiefer_networks.proxmoxopen.core.ui.component.ErrorState
import de.kiefer_networks.proxmoxopen.core.ui.component.LoadingState
import de.kiefer_networks.proxmoxopen.core.ui.component.StatusBadge
import de.kiefer_networks.proxmoxopen.domain.model.HaClusterStatus
import de.kiefer_networks.proxmoxopen.domain.model.HaGroup
import de.kiefer_networks.proxmoxopen.domain.model.HaMember
import de.kiefer_networks.proxmoxopen.domain.model.HaResource

private enum class HaTab(val titleRes: Int) {
    Status(R.string.ha_tab_status),
    Resources(R.string.ha_tab_resources),
    Groups(R.string.ha_tab_groups),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HaScreen(
    onBack: () -> Unit,
    viewModel: HaViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableStateOf(HaTab.Status) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ha_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (state.isRefreshing) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            TabRow(selectedTabIndex = selectedTab.ordinal) {
                HaTab.entries.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = { Text(stringResource(tab.titleRes)) },
                    )
                }
            }

            val isEmpty = state.status == null &&
                state.resources.isEmpty() &&
                state.groups.isEmpty()
            when {
                state.isLoading && isEmpty -> LoadingState()
                state.error != null && isEmpty -> ErrorState(
                    message = state.error?.message ?: "",
                    retryLabel = stringResource(R.string.retry),
                    onRetry = { viewModel.refresh() },
                )
                else -> when (selectedTab) {
                    HaTab.Status -> StatusTab(state.status)
                    HaTab.Resources -> ResourcesTab(state.resources)
                    HaTab.Groups -> GroupsTab(state.groups)
                }
            }
        }
    }
}

// --- Status tab --------------------------------------------------------------

@Composable
private fun StatusTab(status: HaClusterStatus?) {
    if (status == null || status.members.isEmpty()) {
        EmptyMessage()
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { QuorumMasterCard(status) }
        items(status.members, key = { it.id.ifEmpty { it.type + it.node } }) { member ->
            MemberRow(member)
        }
    }
}

@Composable
private fun QuorumMasterCard(status: HaClusterStatus) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.ha_quorum),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                val quorum = status.quorate
                val tone = when (quorum) {
                    true -> BadgeTone.Running
                    false -> BadgeTone.Stopped
                    null -> BadgeTone.Neutral
                }
                val label = when (quorum) {
                    true -> "quorate"
                    false -> "not quorate"
                    null -> "unknown"
                }
                StatusBadge(label = label, tone = tone)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.ha_master),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    status.master ?: "—",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

@Composable
private fun MemberRow(member: HaMember) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        member.id.ifBlank { member.type.ifBlank { "—" } },
                        style = MaterialTheme.typography.titleSmall,
                    )
                    val sub = buildString {
                        append(member.type.ifBlank { "?" })
                        member.node?.let { if (it.isNotBlank()) append(" · ").append(it) }
                    }
                    Text(
                        sub,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                val stateLabel = member.state ?: member.status
                if (!stateLabel.isNullOrBlank()) {
                    StatusBadge(label = stateLabel, tone = toneForState(stateLabel))
                }
            }
        }
    }
}

// --- Resources tab -----------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResourcesTab(resources: List<HaResource>) {
    if (resources.isEmpty()) {
        EmptyMessage()
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(resources, key = { it.sid }) { resource ->
            ResourceCard(resource)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResourceCard(resource: HaResource) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(resource.sid, style = MaterialTheme.typography.titleMedium)
                    Text(
                        resource.type,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                StatusBadge(
                    label = resource.state,
                    tone = toneForState(resource.state),
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                resource.group?.let { Chip(text = it) }
                resource.comment?.let { Chip(text = it) }
            }
        }
    }
}

// --- Groups tab --------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GroupsTab(groups: List<HaGroup>) {
    if (groups.isEmpty()) {
        EmptyMessage()
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(groups, key = { it.group }) { group ->
            GroupCard(group)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GroupCard(group: HaGroup) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(group.group, style = MaterialTheme.typography.titleMedium)
                    group.comment?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                val failbackTone = if (group.nofailback) BadgeTone.Paused else BadgeTone.Running
                val failbackLabel = if (group.nofailback) "no failback" else "failback"
                StatusBadge(label = failbackLabel, tone = failbackTone)
            }
            if (group.nodes.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    group.nodes.forEach { node -> Chip(text = node) }
                }
            }
            if (group.restricted) {
                Text(
                    "restricted",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// --- helpers -----------------------------------------------------------------

@Composable
private fun Chip(text: String) {
    SuggestionChip(
        onClick = {},
        label = {
            Text(text, style = MaterialTheme.typography.labelSmall)
        },
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    )
}

@Composable
private fun EmptyMessage() {
    Text(
        stringResource(R.string.ha_empty),
        modifier = Modifier.padding(16.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun toneForState(state: String): BadgeTone = when (state.lowercase()) {
    "started", "running", "active", "online", "enabled" -> BadgeTone.Running
    "stopped", "disabled", "offline" -> BadgeTone.Stopped
    "error", "fence", "fenced" -> BadgeTone.Error
    "migrate", "relocate", "freeze", "request_start", "request_stop" -> BadgeTone.Paused
    else -> BadgeTone.Neutral
}
