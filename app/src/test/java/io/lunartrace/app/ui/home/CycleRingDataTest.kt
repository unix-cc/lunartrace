package io.lunartrace.app.ui.home

import io.lunartrace.app.domain.model.Confidence
import io.lunartrace.app.domain.model.CyclePhase
import io.lunartrace.app.domain.model.CyclePosition
import io.lunartrace.app.domain.model.CyclePrediction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * 周期环的数据映射。红先绿后：本文件先于实现写出。
 *
 * 最容易错的是"格号换算"（起点反推与 1-based 下标），所以每个用例都把具体格号写死。
 */
class CycleRingDataTest {

    private fun date(text: String): LocalDate = LocalDate.parse(text)

    private fun prediction(
        nextStart: String?,
        basisDays: Int?,
        ovulationDay: String?,
    ) = CyclePrediction(
        nextPeriodStart = nextStart?.let(::date),
        predictedPeriodDays = emptyList(),
        ovulationDay = ovulationDay?.let(::date),
        fertileWindow = null,
        confidence = Confidence.MEDIUM,
        basisCycleLengthDays = basisDays,
    )

    @Test
    fun `没有预测时不画刻度`() {
        val data = CycleRingData.of(
            prediction = prediction(nextStart = null, basisDays = null, ovulationDay = null),
            position = CyclePosition(cycleDay = null, phase = CyclePhase.UNKNOWN),
            periodDates = emptySet(),
            today = date("2026-03-05"),
        )

        assertEquals(CycleRingData.none, data)
    }

    @Test
    fun `正常周期里经期日与排卵日都落在正确的格号上`() {
        // 今天第 5 天 → 起点 3 月 1 日；基准 28 天；排卵日 3 月 15 日 → 第 15 格
        val data = CycleRingData.of(
            prediction = prediction(nextStart = "2026-03-29", basisDays = 28, ovulationDay = "2026-03-15"),
            position = CyclePosition(cycleDay = 5, phase = CyclePhase.FOLLICULAR),
            periodDates = setOf(date("2026-03-01"), date("2026-03-02"), date("2026-03-03")),
            today = date("2026-03-05"),
        )

        assertEquals(28, data.totalDays)
        assertEquals(5, data.currentDayIndex)
        assertEquals(setOf(1, 2, 3), data.periodDayIndexes)
        assertEquals(15, data.ovulationDayIndex)
        assertFalse(data.isOverdue)
    }

    @Test
    fun `超出基准长度时标记仍落在前几格并标记为走完`() {
        // 今天第 1434 天 → 起点 2026-01-29（那次经期的开始日）
        val data = CycleRingData.of(
            prediction = prediction(nextStart = "2030-02-26", basisDays = 28, ovulationDay = null),
            position = CyclePosition(cycleDay = 1434, phase = CyclePhase.UNKNOWN),
            periodDates = setOf(date("2026-01-29")),
            today = date("2030-01-01"),
        )

        assertEquals(28, data.totalDays)
        assertEquals(1434, data.currentDayIndex)
        assertEquals(setOf(1), data.periodDayIndexes)
        assertTrue(data.isOverdue)
    }

    @Test
    fun `排卵日落在本周期之外时不画到环上`() {
        // 排卵日 3 月 15 日 → 起点 3 月 25 日 → 格号为负
        val data = CycleRingData.of(
            prediction = prediction(nextStart = "2026-04-22", basisDays = 28, ovulationDay = "2026-03-15"),
            position = CyclePosition(cycleDay = 3, phase = CyclePhase.MENSTRUATION),
            periodDates = setOf(date("2026-03-25")),
            today = date("2026-03-27"),
        )

        assertNull(data.ovulationDayIndex)
        assertEquals(setOf(1), data.periodDayIndexes)
    }

    @Test
    fun `没有排卵日时环上没有排卵标记`() {
        val data = CycleRingData.of(
            prediction = prediction(nextStart = "2026-03-29", basisDays = 28, ovulationDay = null),
            position = CyclePosition(cycleDay = 5, phase = CyclePhase.UNKNOWN),
            periodDates = emptySet(),
            today = date("2026-03-05"),
        )

        assertEquals(28, data.totalDays)
        assertNull(data.ovulationDayIndex)
    }

    @Test
    fun `零记录时没有周期天数也没有格号`() {
        val data = CycleRingData.of(
            prediction = prediction(nextStart = null, basisDays = null, ovulationDay = null),
            position = CyclePosition(cycleDay = 12, phase = CyclePhase.UNKNOWN),
            periodDates = emptySet(),
            today = date("2026-03-05"),
        )

        assertNull(data.totalDays)
        assertNull(data.currentDayIndex)
    }
}
