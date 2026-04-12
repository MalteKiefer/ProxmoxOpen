package app.proxmoxopen.core.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.proxmoxopen.core.ui.theme.StatusError
import app.proxmoxopen.core.ui.theme.StatusPaused
import app.proxmoxopen.core.ui.theme.StatusRunning
import app.proxmoxopen.core.ui.theme.StatusStopped

enum class BadgeTone { Running, Stopped, Paused, Error, Neutral }

@Composable
fun StatusBadge(label: String, tone: BadgeTone, modifier: Modifier = Modifier) {
    val color = toneColor(tone)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.18f),
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color,
            )
        }
    }
}

@Composable
fun StatusDot(tone: BadgeTone, modifier: Modifier = Modifier.size(10.dp)) {
    val color = toneColor(tone)
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = CircleShape,
            color = color,
            modifier = Modifier.size(10.dp),
        ) {}
    }
}

@Composable
private fun toneColor(tone: BadgeTone): Color = when (tone) {
    BadgeTone.Running -> StatusRunning
    BadgeTone.Stopped -> StatusStopped
    BadgeTone.Paused -> StatusPaused
    BadgeTone.Error -> StatusError
    BadgeTone.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
}
