package io.lunartrace.app.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalDate

private val WEEKDAY_LABELS = listOf("一", "二", "三", "四", "五", "六", "日")
private const val CELLS_PER_ROW = 7
private const val OUT_OF_MONTH_ALPHA = 0.35f
private val CELL_PADDING = 2.dp
private val PREDICTED_BORDER = 1.5.dp
private val TODAY_BORDER = 2.dp
private val SYMPTOM_DOT_SIZE = 4.dp
private val SYMPTOM_DOT_PADDING = 6.dp

/**
 * 月视图网格：固定 6 行 × 7 列，周一起始（中文习惯）。
 * 三类标记必须视觉可区分：实测经期（实心）/ 预计经期（描边）/ 易孕窗口（浅色底）。
 */
@Composable
fun MonthGrid(
    days: List<CalendarDay>,
    today: LocalDate,
    onDateClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            WEEKDAY_LABELS.forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        days.chunked(CELLS_PER_ROW).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    DayCell(
                        day = day,
                        isToday = day.date == today,
                        onClick = { onDateClick(day.date) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: CalendarDay,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val marks = day.marks

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(CELL_PADDING)
            .clip(CircleShape)
            .background(
                when {
                    marks.period -> colors.primary
                    marks.fertile -> colors.primaryContainer
                    else -> Color.Transparent
                },
            )
            .then(
                if (marks.predicted) Modifier.border(PREDICTED_BORDER, colors.tertiary, CircleShape) else Modifier,
            )
            .then(
                if (isToday) Modifier.border(TODAY_BORDER, colors.secondary, CircleShape) else Modifier,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = day.date.dayOfMonth.toString(),
            color = when {
                marks.period -> colors.onPrimary
                day.inCurrentMonth -> colors.onSurface
                else -> colors.onSurfaceVariant.copy(alpha = OUT_OF_MONTH_ALPHA)
            },
            style = MaterialTheme.typography.bodyMedium,
        )
        if (marks.hasSymptoms) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = SYMPTOM_DOT_PADDING)
                    .size(SYMPTOM_DOT_SIZE)
                    .clip(CircleShape)
                    .background(colors.secondary),
            )
        }
    }
}
