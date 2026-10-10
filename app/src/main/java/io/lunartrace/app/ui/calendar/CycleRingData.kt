package io.lunartrace.app.ui.calendar

import io.lunartrace.app.domain.model.CyclePosition
import io.lunartrace.app.domain.model.CyclePrediction
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * 周期环要画的内容。纯数据，不含任何绘制逻辑。
 *
 * 环覆盖**本周期**：起点 = 本次周期的开始日，格数 = 预测实际使用的基准长度。
 * 格号是 1-based，且只保留落在 `1..totalDays` 内的标记。
 *
 * 红先绿后：本文件的实现待补（先返回空数据），由 `CycleRingDataTest` 固定行为。
 */
data class CycleRingData(
    /** 环等分成多少格；`null` 表示没有预测，不画刻度。 */
    val totalDays: Int?,
    /** 今天在第几格（1-based，可能大于 [totalDays]，表示这一圈已经走完）。 */
    val currentDayIndex: Int?,
    /** 实测经期日落在哪些格。 */
    val periodDayIndexes: Set<Int>,
    /** 预计排卵日落在哪一格；不在本周期内或没有排卵日时为 `null`。 */
    val ovulationDayIndex: Int?,
) {
    /** 今天已经超过预计的基准长度：环要画满，而不是被截断成"还在半路"。 */
    val isOverdue: Boolean
        get() = totalDays != null && currentDayIndex != null && currentDayIndex > totalDays

    companion object {
        /** 没有预测时的空环。 */
        val none = CycleRingData(
            totalDays = null,
            currentDayIndex = null,
            periodDayIndexes = emptySet(),
            ovulationDayIndex = null,
        )

        /**
         * 起点由 `position.cycleDay` 反推：`cycleStart = 今天 − (cycleDay − 1)`。
         * 这与预测卡显示的天数是同一个数，避免出现"环说第 5 天、卡说第 1434 天"。
         */
        fun of(
            prediction: CyclePrediction,
            position: CyclePosition,
            periodDates: Set<LocalDate>,
            today: LocalDate,
        ): CycleRingData {
            val totalDays = prediction.basisCycleLengthDays
            val cycleDay = position.cycleDay
            if (totalDays == null || cycleDay == null || totalDays <= 0) return none

            val cycleStart = today.minusDays((cycleDay - 1).toLong())
            return CycleRingData(
                totalDays = totalDays,
                currentDayIndex = cycleDay,
                periodDayIndexes = periodDates
                    .mapNotNull { indexWithin(it, cycleStart, totalDays) }
                    .toSet(),
                ovulationDayIndex = prediction.ovulationDay?.let { indexWithin(it, cycleStart, totalDays) },
            )
        }

        /** 把日期换算成环上的格号（1-based）；落在本周期之外时为 `null`（不画到环上）。 */
        private fun indexWithin(date: LocalDate, cycleStart: LocalDate, totalDays: Int): Int? {
            val index = ChronoUnit.DAYS.between(cycleStart, date) + 1
            return if (index in 1L..totalDays.toLong()) index.toInt() else null
        }
    }
}
