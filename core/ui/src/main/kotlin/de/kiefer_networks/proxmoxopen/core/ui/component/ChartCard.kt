package de.kiefer_networks.proxmoxopen.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter

/**
 * Card containing a labelled chart with the current value displayed
 * prominently. The chart fills the bottom of the card.
 */
@Composable
fun ChartCard(
    icon: ImageVector,
    title: String,
    currentValue: String,
    secondaryValue: String? = null,
    values: List<Double>,
    valueFormatter: CartesianValueFormatter = percentFormatter,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                    modifier = Modifier.size(36.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Column(modifier = Modifier.padding(start = 12.dp).fillMaxWidth()) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = currentValue,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (secondaryValue != null) {
                            Text(
                                text = "  $secondaryValue",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                            )
                        }
                    }
                }
            }
            PxoLineChart(values = values, valueFormatter = valueFormatter)
        }
    }
}
