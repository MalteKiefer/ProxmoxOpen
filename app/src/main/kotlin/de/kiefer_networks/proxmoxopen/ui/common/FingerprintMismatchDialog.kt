package de.kiefer_networks.proxmoxopen.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import de.kiefer_networks.proxmoxopen.R

@Composable
fun FingerprintMismatchDialog(
    expected: String,
    actual: String,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
        },
        title = { Text(stringResource(R.string.fp_mismatch_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(R.string.fp_mismatch_body),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        stringResource(R.string.fp_mismatch_old),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        expected,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        stringResource(R.string.fp_mismatch_new),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        actual,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.fp_mismatch_close))
            }
        },
    )
}
