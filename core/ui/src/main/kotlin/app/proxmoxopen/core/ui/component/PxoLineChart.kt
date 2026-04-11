package app.proxmoxopen.core.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Minimal dependency-free line chart. Renders [values] normalized to the canvas.
 * Vico-based richer charts are scheduled for phase 1 polish.
 */
@Composable
fun PxoLineChart(
    values: List<Double>,
    modifier: Modifier = Modifier.fillMaxWidth().height(160.dp),
) {
    val color = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier) {
        if (values.size < 2) return@Canvas
        val min = values.min()
        val max = values.max().let { if (it == min) it + 1.0 else it }
        val stepX = size.width / (values.size - 1).toFloat()
        val path = Path()
        values.forEachIndexed { index, v ->
            val x = index * stepX
            val y = size.height - ((v - min) / (max - min)).toFloat() * size.height
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 4f),
        )
        // End-point marker
        val lastX = (values.size - 1) * stepX
        val lastNormalized = ((values.last() - min) / (max - min)).toFloat()
        val lastY = size.height - lastNormalized * size.height
        drawCircle(color = color, radius = 6f, center = Offset(lastX, lastY))
    }
}
