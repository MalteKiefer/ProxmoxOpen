package de.kiefer_networks.proxmoxopen.ui.guestdetail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.kiefer_networks.proxmoxopen.R

@Composable
fun DeleteGuestDialog(
    guestTypeLabel: String,
    vmid: Int,
    expectedName: String,
    onDismiss: () -> Unit,
    onConfirm: (purge: Boolean, destroyDisks: Boolean) -> Unit,
) {
    var purge by remember { mutableStateOf(false) }
    var destroyDisks by remember { mutableStateOf(false) }
    var typed by remember { mutableStateOf("") }

    val confirmEnabled = typed == expectedName || typed == vmid.toString()

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
        },
        title = {
            Text(stringResource(R.string.delete_guest_title, guestTypeLabel, vmid))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { purge = !purge },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = purge, onCheckedChange = { purge = it })
                    Text(
                        stringResource(R.string.delete_purge),
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { destroyDisks = !destroyDisks },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = destroyDisks, onCheckedChange = { destroyDisks = it })
                    Text(
                        stringResource(R.string.delete_destroy_disks),
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
                OutlinedTextField(
                    value = typed,
                    onValueChange = { typed = it },
                    label = {
                        Text(stringResource(R.string.delete_type_to_confirm, expectedName))
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = stringResource(R.string.delete_guest_warning),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(purge, destroyDisks) },
                enabled = confirmEnabled,
            ) {
                Text(
                    stringResource(R.string.delete_confirm),
                    color = if (confirmEnabled) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
