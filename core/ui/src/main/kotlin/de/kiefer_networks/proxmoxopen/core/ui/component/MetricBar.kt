package de.kiefer_networks.proxmoxopen.core.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Shows a label on the left, a percentage/ratio on the right, and a progress bar underneath.
 * [value] should be in 0f..1f; callers format the right-hand text themselves.
 */
@Composable
fun MetricBar(
    label: String,
    value: Float,
    caption: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            Text(caption, style = MaterialTheme.typography.labelMedium)
        }
        LinearProgressIndicator(
            progress = { value.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        )
    }
}
