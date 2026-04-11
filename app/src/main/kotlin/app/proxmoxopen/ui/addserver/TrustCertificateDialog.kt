package app.proxmoxopen.ui.addserver

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
        title = { Text(stringResource(R.string.trust_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.trust_warning),
                    style = MaterialTheme.typography.bodySmall,
                )
                LabeledValue(stringResource(R.string.trust_subject), probe.subject)
                LabeledValue(stringResource(R.string.trust_issuer), probe.issuer)
                LabeledValue(
                    stringResource(R.string.trust_valid_from),
                    DateFormat.getDateInstance().format(Date(probe.validFrom)),
                )
                LabeledValue(
                    stringResource(R.string.trust_valid_to),
                    DateFormat.getDateInstance().format(Date(probe.validTo)),
                )
                LabeledValue(
                    stringResource(R.string.trust_sha256),
                    probe.sha256Fingerprint.chunked(2).joinToString(":").uppercase(),
                )
                LabeledValue(
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
private fun LabeledValue(label: String, value: String) {
    Column(Modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}
