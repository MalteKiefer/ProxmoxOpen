package de.kiefer_networks.proxmoxopen.core.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter

/**
 * Lightweight line chart drawn with Canvas. Renders [values] as a poly-line
 * with a vertical primary gradient fill underneath. Used by [ChartCard].
 */
@Composable
fun PxoLineChart(
    values: List<Double>,
    @Suppress("UNUSED_PARAMETER") valueFormatter: CartesianValueFormatter = percentFormatter,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(120.dp),
) {
    if (values.size < 2) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text(
                text = "—",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }
    val primary = MaterialTheme.colorScheme.primary
    val gradientStart = primary.copy(alpha = 0.45f)
    val gradientEnd = primary.copy(alpha = 0f)

    Canvas(modifier = modifier) {
        val min = values.min()
        val max = values.max().let { if (it == min) it + 1.0 else it }
        val stepX = size.width / (values.size - 1).toFloat()

        val linePath = Path()
        val areaPath = Path()
        values.forEachIndexed { index, v ->
            val x = index * stepX
            val y = size.height - ((v - min) / (max - min)).toFloat() * size.height
            if (index == 0) {
                linePath.moveTo(x, y)
                areaPath.moveTo(x, size.height)
                areaPath.lineTo(x, y)
            } else {
                linePath.lineTo(x, y)
                areaPath.lineTo(x, y)
            }
        }
        areaPath.lineTo((values.size - 1) * stepX, size.height)
        areaPath.close()

        drawPath(
            path = areaPath,
            brush = Brush.verticalGradient(
                colors = listOf(gradientStart, gradientEnd),
                startY = 0f,
                endY = size.height,
            ),
        )
        drawPath(
            path = linePath,
            color = primary,
            style = Stroke(width = 4f),
        )

        val lastX = (values.size - 1) * stepX
        val lastNormalized = ((values.last() - min) / (max - min)).toFloat()
        val lastY = size.height - lastNormalized * size.height
        drawCircle(color = primary, radius = 6f, center = Offset(lastX, lastY))
    }
}

val percentFormatter: CartesianValueFormatter = CartesianValueFormatter { value, _, _ ->
    val pct = (value * 100.0).toInt()
    "$pct%"
}
