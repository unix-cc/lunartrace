package io.lunartrace.app.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalDate

private val WEEKDAY_LABELS = listOf("日", "一", "二", "三", "四", "五", "六")
private const val OUT_OF_MONTH_ALPHA = 0.35f
private const val PREDICTED_BORDER_ALPHA = 0.5f

private val CELL_PADDING = 1.dp
private val CELL_CORNER = 6.dp
private val PILL_HEIGHT = 8.dp
private val PILL_HORIZONTAL_PADDING = 4.dp
private val PILL_RADIUS = 4.dp
private val DOT_SIZE = 4.dp
private val SYMPTOM_DOT_PADDING = 4.dp
private val OVULATION_DOT_PADDING = 2.dp
private val NUMBER_RING_SIZE = 26.dp
private val SELECTED_RING_WIDTH = 1.dp

/**
 * 月视图网格：固定 6 行 × 7 列，周日起始。
 *
 * 标记语义（与图例一致）：
 * - 经期日 → 实心胶囊；预测经期日 → 空心胶囊；连续多天连成一条带（见 [MonthGridLayout.ribbonEdges]）
 * - 易孕窗口 → 格子浅黄底（`tertiaryContainer`）
 * - 排卵日 → 底部居中橙点（`tertiary`）；有症状记录 → 右上角灰点（`secondary`），两者位置分开以免遮挡
 */
@Composable
fun MonthGrid(
    days: List<CalendarDay>,
    today: LocalDate,
    selectedDate: LocalDate,
    periodDates: Set<LocalDate>,
    predictedDates: Set<LocalDate>,
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
        days.chunked(MonthGridLayout.CELLS_PER_ROW).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    DayCell(
                        day = day,
                        isToday = day.date == today,
                        isSelected = day.date == selectedDate,
                        periodEdges = if (day.marks.period) MonthGridLayout.ribbonEdges(periodDates, day.date) else null,
                        predictedEdges = if (day.marks.predicted) {
                            MonthGridLayout.ribbonEdges(predictedDates, day.date)
                        } else {
                            null
                        },
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
    isSelected: Boolean,
    periodEdges: RibbonEdges?,
    predictedEdges: RibbonEdges?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val marks = day.marks

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(CELL_PADDING)
            .clip(RoundedCornerShape(CELL_CORNER))
            .background(if (marks.fertile) colors.tertiaryContainer else Color.Transparent)
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // 选中的那一天在数字外加一圈中性描边：与"今天"（加粗 + 主色）可以同时成立，
            // 也不与格子下方的胶囊条、易孕底色、排卵点冲突。
            Box(
                modifier = Modifier
                    .size(NUMBER_RING_SIZE)
                    .then(
                        if (isSelected) {
                            Modifier.border(SELECTED_RING_WIDTH, colors.onSurfaceVariant, CircleShape)
                        } else {
                            Modifier
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = day.date.dayOfMonth.toString(),
                    color = when {
                        isToday -> colors.primary
                        day.inCurrentMonth -> colors.onSurface
                        else -> colors.onSurfaceVariant.copy(alpha = OUT_OF_MONTH_ALPHA)
                    },
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(Modifier.height(2.dp))
            MarkBar(periodEdges = periodEdges, predictedEdges = predictedEdges)
        }
        if (marks.ovulation) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = OVULATION_DOT_PADDING)
                    .size(DOT_SIZE)
                    .clip(CircleShape)
                    .background(colors.tertiary),
            )
        }
        if (marks.hasSymptoms) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(SYMPTOM_DOT_PADDING)
                    .size(DOT_SIZE)
                    .clip(CircleShape)
                    .background(colors.secondary),
            )
        }
    }
}

/** 格子下方的胶囊条：经期实心、预测经期空心；两者都没有时留一条透明占位以保持各行基线一致。 */
@Composable
private fun MarkBar(periodEdges: RibbonEdges?, predictedEdges: RibbonEdges?) {
    val colors = MaterialTheme.colorScheme
    val edges = periodEdges ?: predictedEdges
    val shape = ribbonShape(edges)
    val base = Modifier
        .fillMaxWidth()
        .padding(horizontal = PILL_HORIZONTAL_PADDING)
        .height(PILL_HEIGHT)

    when {
        periodEdges != null -> Box(base.background(colors.primary, shape))
        predictedEdges != null -> Box(base.border(1.dp, colors.primary.copy(alpha = PREDICTED_BORDER_ALPHA), shape))
        else -> Spacer(base)
    }
}

/** 只在外侧端倒圆角：左端 [RibbonEdges.start]、右端 [RibbonEdges.end]。 */
private fun ribbonShape(edges: RibbonEdges?): Shape {
    val startRadius = if (edges?.start != false) PILL_RADIUS else 0.dp
    val endRadius = if (edges?.end != false) PILL_RADIUS else 0.dp
    return RoundedCornerShape(
        topStart = startRadius,
        bottomStart = startRadius,
        topEnd = endRadius,
        bottomEnd = endRadius,
    )
}
