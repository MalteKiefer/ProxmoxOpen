package app.proxmoxopen.ui.addserver

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
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
import app.proxmoxopen.R
import app.proxmoxopen.domain.repository.ServerProbe
import java.text.DateFormat
import java.util.Date

@Composable
fun TrustCertificateDialog(
    probe: ServerProbe,
    onTrust: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        icon = {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = { Text(stringResource(R.string.trust_title)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = stringResource(R.string.trust_warning),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Field(stringResource(R.string.trust_subject), probe.subject)
                Field(stringResource(R.string.trust_issuer), probe.issuer)
                Field(
                    stringResource(R.string.trust_valid_from),
                    DateFormat.getDateInstance().format(Date(probe.validFrom)),
                )
                Field(
                    stringResource(R.string.trust_valid_to),
                    DateFormat.getDateInstance().format(Date(probe.validTo)),
                )
                FieldMono(
                    stringResource(R.string.trust_sha256),
                    probe.sha256Fingerprint.chunked(2).joinToString(":").uppercase(),
                )
                FieldMono(
                    stringResource(R.string.trust_sha1),
                    probe.sha1Fingerprint.chunked(2).joinToString(":").uppercase(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onTrust) { Text(stringResource(R.string.trust_button)) }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun Field(label: String, value: String) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun FieldMono(label: String, value: String) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
        )
    }
}
