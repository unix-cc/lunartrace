package io.lunartrace.app.domain.prediction

import io.lunartrace.app.domain.model.Confidence
import io.lunartrace.app.domain.model.CycleDefaults
import io.lunartrace.app.domain.model.CyclePhase
import io.lunartrace.app.domain.model.PredictionBasis
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/**
 * 用户填写的缺省周期长度（ADR-0005）的边界测试。
 *
 * 红先绿后：本文件先于实现写出。此时 `analyze` 虽然接受 `defaults`，但实现里**完全没有用它**，
 * 因此凡是以 `USER_DEFAULT` 为依据的用例都应当先失败。
 */
class CyclePredictorUserDefaultsTest {

    private fun date(text: String): LocalDate = LocalDate.parse(text)

    private fun analyze(
        today: String,
        defaults: CycleDefaults,
        vararg periodDays: String,
    ) = CyclePredictor.analyze(periodDays.map(::date), date(today), defaults)

    @Test
    fun `只有一段经期时用用户填写的周期长度兜底`() {
        val insight = analyze(
            today = "2026-04-01",
            defaults = CycleDefaults(cycleLengthDays = 35, periodLengthDays = null),
            "2026-03-01",
        )

        assertEquals(PredictionBasis.USER_DEFAULT, insight.prediction.basis)
        assertEquals(Confidence.LOW, insight.prediction.confidence)
        // 3 月 1 日 + 35 天 = 4 月 5 日
        assertEquals(date("2026-04-05"), insight.prediction.nextPeriodStart)
    }

    @Test
    fun `兜底状态下不显示排卵日与易孕窗口`() {
        val insight = analyze(
            today = "2026-04-01",
            defaults = CycleDefaults(cycleLengthDays = 35, periodLengthDays = 7),
            "2026-03-01",
        )

        assertNull(insight.prediction.ovulationDay)
        assertNull(insight.prediction.fertileWindow)
        assertEquals(CyclePhase.UNKNOWN, insight.position.phase)
        // 周期第几天是事实，仍然要给
        assertEquals(32, insight.position.cycleDay)
    }

    @Test
    fun `有实测样本时忽略用户填写的值`() {
        val insight = analyze(
            today = "2026-04-01",
            defaults = CycleDefaults(cycleLengthDays = 45, periodLengthDays = 7),
            "2026-01-01",
            "2026-01-29",
            "2026-02-26",
        )

        assertEquals(PredictionBasis.RECORDS, insight.prediction.basis)
        // 实测周期长度 28 天优先：2 月 26 日 + 28 天 = 3 月 26 日（已过期）→ 顺推到 4 月 23 日
        assertEquals(date("2026-04-23"), insight.prediction.nextPeriodStart)
    }

    @Test
    fun `没有任何记录时即使填了缺省值也不预测`() {
        val insight = analyze(
            today = "2026-04-01",
            defaults = CycleDefaults(cycleLengthDays = 35, periodLengthDays = null),
        )

        assertEquals(PredictionBasis.NONE, insight.prediction.basis)
        assertNull(insight.prediction.nextPeriodStart)
    }

    @Test
    fun `缺省周期长度越界时视为未填写`() {
        listOf(12, 90).forEach { outOfRange ->
            val insight = analyze(
                today = "2026-04-01",
                defaults = CycleDefaults(cycleLengthDays = outOfRange, periodLengthDays = null),
                "2026-03-01",
            )

            assertEquals("越界值 $outOfRange 不应被采用", PredictionBasis.NONE, insight.prediction.basis)
            assertNull(insight.prediction.nextPeriodStart)
        }
    }

    @Test
    fun `只填周期长度时预测区间用缺省经期长度`() {
        val insight = analyze(
            today = "2026-03-20",
            defaults = CycleDefaults(cycleLengthDays = 28, periodLengthDays = null),
            "2026-03-01",
        )

        assertEquals(date("2026-03-29"), insight.prediction.nextPeriodStart)
        assertEquals(5, insight.prediction.predictedPeriodDays.size)
    }

    @Test
    fun `填了经期长度时预测区间用它`() {
        val insight = analyze(
            today = "2026-03-20",
            defaults = CycleDefaults(cycleLengthDays = 28, periodLengthDays = 7),
            "2026-03-01",
        )

        assertEquals(7, insight.prediction.predictedPeriodDays.size)
    }

    @Test
    fun `缺省值不进入任何统计量`() {
        val insight = analyze(
            today = "2026-04-01",
            defaults = CycleDefaults(cycleLengthDays = 45, periodLengthDays = 7),
            "2026-03-01",
        )

        assertNull(insight.stats.averageCycleLength)
        assertNull(insight.stats.medianCycleLength)
        assertEquals(emptyList<Int>(), insight.stats.recentCycleLengths)
        assertEquals(0, insight.stats.validSampleCount)
    }
}
