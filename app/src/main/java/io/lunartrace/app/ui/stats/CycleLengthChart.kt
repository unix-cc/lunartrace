package io.lunartrace.app.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private const val MIN_POINTS = 2
private val CHART_HEIGHT = 120.dp
private const val STROKE_WIDTH = 3f
private const val POINT_RADIUS = 5f

/**
 * 周期长度趋势（自绘 Canvas，不引入图表库）。
 * 只画折线与数据点，数值标签放在下方一行，避免在 Canvas 里做文字测量。
 */
@Composable
fun CycleLengthChart(values: List<Int>, modifier: Modifier = Modifier) {
    if (values.size < MIN_POINTS) {
        Text(
            text = "至少记录两次经期开始（产生一个周期长度）后展示趋势。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier,
        )
        return
    }

    val lineColor = MaterialTheme.colorScheme.primary
    val pointColor = MaterialTheme.colorScheme.tertiary
    val baselineColor = MaterialTheme.colorScheme.surfaceVariant
    val minValue = values.minOrNull() ?: 0
    val maxValue = values.maxOrNull() ?: 0
    val span = (maxValue - minValue).coerceAtLeast(1)

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(CHART_HEIGHT)
                .padding(vertical = 8.dp),
        ) {
            val stepX = size.width / (values.size - 1)
            val points = values.mapIndexed { index, value ->
                val ratio = (value - minValue).toFloat() / span
                Offset(x = stepX * index, y = size.height * (1f - ratio))
            }

            drawLine(
                color = baselineColor,
                start = Offset(0f, size.height),
                end = Offset(size.width, size.height),
                strokeWidth = STROKE_WIDTH,
            )
            points.zipWithNext { previous, next ->
                drawLine(
                    color = lineColor,
                    start = previous,
                    end = next,
                    strokeWidth = STROKE_WIDTH,
                    cap = StrokeCap.Round,
                )
            }
            points.forEach { point ->
                drawCircle(color = Color.White, radius = POINT_RADIUS + 1.5f, center = point)
                drawCircle(color = pointColor, radius = POINT_RADIUS, center = point)
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            values.forEach { value ->
                Text(
                    text = value.toString(),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = "最近 ${values.size} 个有效周期长度（天），左为较早",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
