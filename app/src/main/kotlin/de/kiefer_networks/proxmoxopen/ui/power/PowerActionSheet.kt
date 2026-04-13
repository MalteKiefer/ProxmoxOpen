package de.kiefer_networks.proxmoxopen.ui.power

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.kiefer_networks.proxmoxopen.R
import de.kiefer_networks.proxmoxopen.domain.model.PowerAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PowerActionSheet(
    guestName: String,
    guestType: String,
    onDismiss: () -> Unit,
    onSelect: (PowerAction) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    var pendingConfirm by remember { mutableStateOf<PowerAction?>(null) }
    val actions = if (guestType == "lxc") {
        PowerAction.entries.filter { it != PowerAction.RESET }
    } else {
        PowerAction.entries
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.power_actions),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            Text(
                text = guestName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            actions.forEach { action ->
                ActionRow(
                    action = action,
                    onClick = {
                        if (action.destructive) pendingConfirm = action else onSelect(action)
                    },
                )
            }
        }
    }

    pendingConfirm?.let { action ->
        AlertDialog(
            onDismissRequest = { pendingConfirm = null },
            icon = {
                Icon(
                    imageVector = iconFor(action),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
            },
            title = { Text(stringResource(R.string.power_confirm_title)) },
            text = { Text(stringResource(R.string.power_confirm_body, label(action), guestName)) },
            confirmButton = {
                TextButton(onClick = {
                    pendingConfirm = null
                    onSelect(action)
                }) { Text(label(action)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingConfirm = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun ActionRow(action: PowerAction, onClick: () -> Unit) {
    val color = if (action.destructive) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val onColor = if (action.destructive) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = color,
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = onColor.copy(alpha = 0.12f),
                modifier = Modifier.size(36.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = iconFor(action),
                        contentDescription = label(action),
                        tint = onColor,
                    )
                }
            }
            Text(
                text = label(action),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                color = onColor,
                modifier = Modifier.padding(start = 16.dp),
            )
        }
    }
}

@Composable
private fun label(action: PowerAction): String = stringResource(
    when (action) {
        PowerAction.START -> R.string.power_start
        PowerAction.STOP -> R.string.power_stop
        PowerAction.SHUTDOWN -> R.string.power_shutdown
        PowerAction.REBOOT -> R.string.power_reboot
        PowerAction.SUSPEND -> R.string.power_suspend
        PowerAction.RESUME -> R.string.power_resume
        PowerAction.RESET -> R.string.power_reset
    },
)

private fun iconFor(action: PowerAction): ImageVector = when (action) {
    PowerAction.START -> Icons.Outlined.PlayArrow
    PowerAction.STOP -> Icons.Outlined.Stop
    PowerAction.SHUTDOWN -> Icons.Outlined.PowerSettingsNew
    PowerAction.REBOOT -> Icons.Outlined.Refresh
    PowerAction.SUSPEND -> Icons.Outlined.Bedtime
    PowerAction.RESUME -> Icons.Outlined.WbSunny
    PowerAction.RESET -> Icons.Outlined.RestartAlt
}
