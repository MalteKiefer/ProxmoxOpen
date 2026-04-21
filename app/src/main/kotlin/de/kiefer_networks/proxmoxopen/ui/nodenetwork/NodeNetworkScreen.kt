package de.kiefer_networks.proxmoxopen.ui.nodenetwork

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.kiefer_networks.proxmoxopen.R
import de.kiefer_networks.proxmoxopen.core.ui.component.BadgeTone
import de.kiefer_networks.proxmoxopen.core.ui.component.ErrorState
import de.kiefer_networks.proxmoxopen.core.ui.component.LoadingState
import de.kiefer_networks.proxmoxopen.core.ui.component.StatusBadge
import de.kiefer_networks.proxmoxopen.domain.model.NodeNetworkIface
import de.kiefer_networks.proxmoxopen.domain.model.NodeNetworkIfaceType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NodeNetworkScreen(
    onBack: () -> Unit,
    viewModel: NodeNetworkViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.network_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            Icons.Outlined.Refresh,
                            contentDescription = stringResource(R.string.retry),
                        )
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
            when {
                state.isLoading && state.interfaces.isEmpty() -> LoadingState()
                state.error != null && state.interfaces.isEmpty() -> ErrorState(
                    message = state.error?.message ?: "",
                    retryLabel = stringResource(R.string.retry),
                    onRetry = viewModel::refresh,
                )
                state.interfaces.isEmpty() -> EmptyNetworkState()
                else -> InterfaceList(state.interfaces)
            }
        }
    }
}

@Composable
private fun EmptyNetworkState() {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            stringResource(R.string.network_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        ReadOnlyFooter()
    }
}

@Composable
private fun InterfaceList(interfaces: List<NodeNetworkIface>) {
    val grouped = interfaces
        .groupBy { it.type }
        .toSortedMap(compareBy { groupOrder(it) })

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        grouped.forEach { (type, group) ->
            if (type == NodeNetworkIfaceType.LOOPBACK) return@forEach
            item(key = "header_${type.name}") { SectionHeader(sectionTitleFor(type)) }
            group.sortedBy { it.iface }.forEach { iface ->
                item(key = "iface_${iface.iface}") { InterfaceCard(iface) }
            }
        }
        item(key = "readonly_footer") {
            Spacer(Modifier.height(8.dp))
            ReadOnlyFooter()
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 2.dp),
    )
}

@Composable
private fun InterfaceCard(iface: NodeNetworkIface) {
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
            HeaderRow(iface)
            AddressSection(iface)
            TypeSpecificSection(iface)
            FooterSection(iface)
        }
    }
}

@Composable
private fun HeaderRow(iface: NodeNetworkIface) {
    val isActive = iface.active == true
    val badgeLabel = if (isActive) {
        stringResource(R.string.network_active)
    } else {
        stringResource(R.string.network_inactive)
    }
    val badgeTone = if (isActive) BadgeTone.Running else BadgeTone.Stopped

    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                iface.iface,
                style = MaterialTheme.typography.titleMedium,
                fontFamily = FontFamily.Monospace,
            )
            val subtitle = iface.rawType ?: typeLabel(iface.type)
            if (!subtitle.isNullOrBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        StatusBadge(label = badgeLabel, tone = badgeTone)
    }
}

@Composable
private fun typeLabel(type: NodeNetworkIfaceType): String? = when (type) {
    NodeNetworkIfaceType.BRIDGE -> stringResource(R.string.network_type_bridge)
    NodeNetworkIfaceType.BOND -> stringResource(R.string.network_type_bond)
    NodeNetworkIfaceType.ETH -> stringResource(R.string.network_type_eth)
    NodeNetworkIfaceType.VLAN -> stringResource(R.string.network_type_vlan)
    NodeNetworkIfaceType.LOOPBACK -> null
    NodeNetworkIfaceType.OTHER -> null
}

@Composable
private fun AddressSection(iface: NodeNetworkIface) {
    val v4Primary = iface.cidr?.takeIf { it.isNotBlank() }
        ?: iface.address?.takeIf { it.isNotBlank() }?.let { addr ->
            iface.netmask?.let { "$addr / $it" } ?: addr
        }
    val v6Primary = iface.cidr6?.takeIf { it.isNotBlank() }
        ?: iface.address6?.takeIf { it.isNotBlank() }

    if (v4Primary != null) {
        KeyValueRow(stringResource(R.string.network_address), v4Primary, mono = true)
    }
    val gw = iface.gateway
    if (!gw.isNullOrBlank()) {
        KeyValueRow(stringResource(R.string.network_gateway), gw, mono = true)
    }
    if (v6Primary != null) {
        KeyValueRow(
            stringResource(R.string.network_address) + " (v6)",
            v6Primary,
            mono = true,
        )
    }
    val gw6 = iface.gateway6
    if (!gw6.isNullOrBlank()) {
        KeyValueRow(
            stringResource(R.string.network_gateway) + " (v6)",
            gw6,
            mono = true,
        )
    }
}

@Composable
private fun TypeSpecificSection(iface: NodeNetworkIface) {
    when (iface.type) {
        NodeNetworkIfaceType.BRIDGE -> {
            if (iface.bridgePorts.isNotEmpty()) {
                KeyValueRow(
                    stringResource(R.string.network_members),
                    iface.bridgePorts.joinToString(", "),
                    mono = true,
                )
            }
        }
        NodeNetworkIfaceType.BOND -> {
            if (iface.slaves.isNotEmpty()) {
                KeyValueRow(
                    stringResource(R.string.network_members),
                    iface.slaves.joinToString(", "),
                    mono = true,
                )
            }
            val mode = iface.bondMode
            if (!mode.isNullOrBlank()) {
                KeyValueRow("Mode", mode)
            }
        }
        NodeNetworkIfaceType.VLAN -> {
            val parent = iface.vlanRawDevice
            if (!parent.isNullOrBlank()) {
                KeyValueRow("Parent", parent, mono = true)
            }
            iface.vlanId?.let { KeyValueRow("Tag", it.toString()) }
        }
        else -> Unit
    }
}

@Composable
private fun FooterSection(iface: NodeNetworkIface) {
    val mtu = iface.mtu
    if (!mtu.isNullOrBlank()) {
        KeyValueRow("MTU", mtu)
    }
    val comment = iface.comments
    if (!comment.isNullOrBlank()) {
        Text(
            comment.trim(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun KeyValueRow(label: String, value: String, mono: Boolean = false) {
    Row(verticalAlignment = Alignment.Top) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(96.dp),
        )
        Text(
            value,
            style = if (mono) {
                MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
            } else {
                MaterialTheme.typography.bodyMedium
            },
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ReadOnlyFooter() {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                "Read-only — edit from Proxmox web UI",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun sectionTitleFor(type: NodeNetworkIfaceType): String = when (type) {
    NodeNetworkIfaceType.BRIDGE -> "Bridges"
    NodeNetworkIfaceType.BOND -> "Bonds"
    NodeNetworkIfaceType.ETH -> "Physical NICs"
    NodeNetworkIfaceType.VLAN -> "VLANs"
    NodeNetworkIfaceType.LOOPBACK -> "Loopback"
    NodeNetworkIfaceType.OTHER -> "Other"
}

private fun groupOrder(type: NodeNetworkIfaceType): Int = when (type) {
    NodeNetworkIfaceType.BRIDGE -> 0
    NodeNetworkIfaceType.BOND -> 1
    NodeNetworkIfaceType.ETH -> 2
    NodeNetworkIfaceType.VLAN -> 3
    NodeNetworkIfaceType.OTHER -> 4
    NodeNetworkIfaceType.LOOPBACK -> 5
}
