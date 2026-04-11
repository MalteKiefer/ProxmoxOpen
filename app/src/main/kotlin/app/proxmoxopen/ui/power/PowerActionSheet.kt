package app.proxmoxopen.ui.power

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.proxmoxopen.R
import app.proxmoxopen.domain.model.PowerAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PowerActionSheet(
    guestName: String,
    onDismiss: () -> Unit,
    onSelect: (PowerAction) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    var pendingConfirm by remember { mutableStateOf<PowerAction?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(guestName, style = MaterialTheme.typography.titleMedium)
            PowerAction.entries.forEach { action ->
                OutlinedButton(
                    onClick = {
                        if (action.destructive) pendingConfirm = action else onSelect(action)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(label(action))
                }
            }
        }
    }

    pendingConfirm?.let { action ->
        AlertDialog(
            onDismissRequest = { pendingConfirm = null },
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
