package io.lunartrace.app.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import io.lunartrace.app.domain.model.CyclePhase
import io.lunartrace.app.ui.common.fullDateWithWeekdayText
import io.lunartrace.app.ui.common.labelOrNull
import java.time.LocalDate
import kotlin.math.cos
import kotlin.math.sin

/**
 * 角度约定：**0 度在正上方、顺时针**；第 i 格的中心角 = `-90 + (i - 0.5) / 格数 × 360`。
 * 这条约定差一格就整圈偏，所以格号换算由 `CycleRingDataTest` 固定，改动前先看它。
 */
private const val START_ANGLE_DEGREES = -90f
private const val DEGREES_IN_CIRCLE = 360f

/** 第 i 格的"格中心"角要落在格子的正中，而不是格子的边界上。 */
private const val SLOT_CENTER_OFFSET = 0.5f
private val RING_SIZE = 200.dp
private val RING_STROKE = 12.dp
private val PERIOD_MARKER_RADIUS = 4.dp
private val OVULATION_MARKER_RADIUS = 5.dp
private val TODAY_MARKER_RADIUS = 6.dp
private const val HOLDOVER_ALPHA = 0.4f

/**
 * 第 [dayIndex] 格（1-based）的**格中心**角度（度）。
 *
 * 抽成纯函数是为了让它可测：环上最容易错的就是这条角度约定（0 度在正上方、顺时针、且落在格中心），
 * 一旦差一格，整圈的标记都会偏，而画出来的东西又没法靠单元测试发现。
 */
internal fun ringAngleForDay(dayIndex: Int, totalDays: Int): Float =
    START_ANGLE_DEGREES + DEGREES_IN_CIRCLE * (dayIndex - SLOT_CENTER_OFFSET) / totalDays

/**
 * 周期环区块：环本体 + 今天的日期。
 *
 * 环覆盖**本周期**（起点 = 本次经期开始日）：
 * 主色弧 = 已走过的天数、主色圆点 = 实测经期日、橙点 = 预计排卵日、中性圆点 = 今天。
 * 没有预测时不画刻度与弧，圆心直说"还没有记录"——不画假进度。
 */
@Composable
fun CycleRingSection(
    data: CycleRingData,
    today: LocalDate,
    phase: CyclePhase,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.size(RING_SIZE), contentAlignment = Alignment.Center) {
            RingCanvas(data = data, modifier = Modifier.fillMaxSize())
            RingCenter(data = data, phase = phase)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = today.fullDateWithWeekdayText(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RingCanvas(data: CycleRingData, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme

    Canvas(modifier = modifier) {
        val stroke = RING_STROKE.toPx()
        val margin = stroke / 2f + TODAY_MARKER_RADIUS.toPx()
        val diameter = size.minDimension - margin * 2f
        if (diameter <= 0f) return@Canvas

        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
        val arcSize = Size(diameter, diameter)
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = diameter / 2f

        // 底圈：没有预测时，这一圈就是全部内容。
        drawArc(
            color = colors.surfaceVariant,
            startAngle = 0f,
            sweepAngle = DEGREES_IN_CIRCLE,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke),
        )

        val totalDays = data.totalDays ?: return@Canvas
        val currentDay = data.currentDayIndex ?: 0
        val progress = currentDay.coerceIn(0, totalDays).toFloat() / totalDays

        fun pointOnRing(degrees: Float): Offset {
            val radians = Math.toRadians(degrees.toDouble())
            return Offset(
                x = center.x + (radius * cos(radians)).toFloat(),
                y = center.y + (radius * sin(radians)).toFloat(),
            )
        }

        fun pointAtDay(index: Int): Offset = pointOnRing(ringAngleForDay(index, totalDays))

        if (progress > 0f) {
            drawArc(
                color = colors.primary,
                startAngle = START_ANGLE_DEGREES,
                sweepAngle = DEGREES_IN_CIRCLE * progress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }

        // 超过一个基准长度：多出来的部分用同色浅一些再画一遍，表达"又走了这些天"，
        // 而不是把弧截断成"还在半路"。
        if (data.isOverdue) {
            val holdover = (currentDay - totalDays).coerceAtMost(totalDays).toFloat() / totalDays
            drawArc(
                color = colors.primary.copy(alpha = HOLDOVER_ALPHA),
                startAngle = START_ANGLE_DEGREES,
                sweepAngle = DEGREES_IN_CIRCLE * holdover,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }

        data.periodDayIndexes.forEach { index ->
            drawCircle(
                color = colors.primary,
                radius = PERIOD_MARKER_RADIUS.toPx(),
                center = pointAtDay(index),
            )
        }
        data.ovulationDayIndex?.let { index ->
            drawCircle(
                color = colors.tertiary,
                radius = OVULATION_MARKER_RADIUS.toPx(),
                center = pointAtDay(index),
            )
        }

        // 今天：在弧的末端打一个中性圆点（先挖一个底色孔，避免与弧糊在一起）。
        if (progress > 0f) {
            val tip = pointOnRing(START_ANGLE_DEGREES + DEGREES_IN_CIRCLE * progress)
            drawCircle(color = colors.surface, radius = TODAY_MARKER_RADIUS.toPx(), center = tip)
            drawCircle(
                color = colors.onSurface,
                radius = TODAY_MARKER_RADIUS.toPx() - 2.dp.toPx(),
                center = tip,
            )
        }
    }
}

@Composable
private fun RingCenter(data: CycleRingData, phase: CyclePhase) {
    val colors = MaterialTheme.colorScheme

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = data.currentDayIndex?.let { "第 $it 天" } ?: "还没有记录",
            style = MaterialTheme.typography.headlineSmall,
            color = colors.onSurface,
        )
        phase.labelOrNull()?.let { phaseLabel ->
            Spacer(Modifier.height(2.dp))
            Text(text = phaseLabel, style = MaterialTheme.typography.bodyMedium, color = colors.primary)
        }
    }
}
