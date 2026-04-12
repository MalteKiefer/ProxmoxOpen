package app.proxmoxopen.core.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottomAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStartAxis
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries

/**
 * Vico-backed line chart with Material-3 theming.
 *
 * Renders [values] as a single line series with start and bottom axes.
 * Shows a dash when there are fewer than two samples.
 */
@Composable
fun PxoLineChart(
    values: List<Double>,
    valueFormatter: CartesianValueFormatter = percentFormatter,
    modifier: Modifier = Modifier.fillMaxWidth().height(160.dp),
) {
    if (values.size < 2) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text("—", style = MaterialTheme.typography.titleLarge)
        }
        return
    }
    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(values) {
        modelProducer.runTransaction {
            lineSeries { series(values) }
        }
    }
    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(),
            startAxis = rememberStartAxis(valueFormatter = valueFormatter),
            bottomAxis = rememberBottomAxis(),
        ),
        modelProducer = modelProducer,
        modifier = modifier,
    )
}

val percentFormatter: CartesianValueFormatter = CartesianValueFormatter { value, _, _ ->
    val pct = (value * 100.0).toInt()
    "$pct%"
}
