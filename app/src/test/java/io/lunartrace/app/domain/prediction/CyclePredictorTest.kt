package io.lunartrace.app.domain.prediction

import io.lunartrace.app.domain.model.Confidence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * 预测口径的边界测试。每一条都对应 `specs/capabilities/cycle-prediction.md` §3 的一行。
 */
class CyclePredictorTest {

    private fun date(text: String): LocalDate = LocalDate.parse(text)

    private fun analyze(today: String, vararg periodStarts: String) =
        CyclePredictor.analyze(periodStarts.map(::date), date(today))

    @Test
    fun `没有任何记录时不预测`() {
        val insight = CyclePredictor.analyze(emptyList(), date("2026-04-01"))

        assertEquals(Confidence.NONE, insight.prediction.confidence)
        assertNull(insight.prediction.nextPeriodStart)
        assertNull(insight.prediction.ovulationDay)
        assertNull(insight.prediction.fertileWindow)
        assertTrue(insight.prediction.predictedPeriodDays.isEmpty())
        assertNull(insight.stats.averageCycleLength)
        assertEquals(0, insight.stats.validSampleCount)
    }

    @Test
    fun `只有一段经期时不预测`() {
        val insight = analyze("2026-04-01", "2026-03-01")

        assertEquals(Confidence.NONE, insight.prediction.confidence)
        assertNull(insight.prediction.nextPeriodStart)
        // 单日记录 → 该段经期长度为 1 天（未夹取到下限，下限只作用于预测区间）
        assertEquals(1, insight.stats.averagePeriodLength)
    }

    @Test
    fun `只有一个有效周期长度时用该长度并降级为 LOW`() {
        val insight = analyze("2026-04-01", "2026-01-01", "2026-01-29")

        assertEquals(Confidence.LOW, insight.prediction.confidence)
        assertEquals(28, insight.stats.averageCycleLength)
        assertEquals(28, insight.stats.medianCycleLength)
        // 1 月 29 日 + 28 天 = 2 月 26 日（已过期）→ 顺推到 4 月 23 日
        assertEquals(date("2026-04-23"), insight.prediction.nextPeriodStart)
        // LOW 不展示易孕窗口
        assertNull(insight.prediction.fertileWindow)
        assertNull(insight.prediction.ovulationDay)
    }

    @Test
    fun `三个稳定周期给出 MEDIUM 与易孕窗口`() {
        val insight = analyze("2026-04-01", "2026-01-01", "2026-01-29", "2026-02-26", "2026-03-26")

        assertEquals(Confidence.MEDIUM, insight.prediction.confidence)
        assertEquals(3, insight.stats.validSampleCount)
        assertEquals(0, insight.stats.variabilityDays)
        assertEquals(date("2026-04-23"), insight.prediction.nextPeriodStart)
        // 平均经期长度为 1 天，但预测区间夹取到下限 3 天
        assertEquals(
            listOf(date("2026-04-23"), date("2026-04-24"), date("2026-04-25")),
            insight.prediction.predictedPeriodDays,
        )
        assertEquals(date("2026-04-09"), insight.prediction.ovulationDay)
        assertEquals(date("2026-04-04"), insight.prediction.fertileWindow?.start)
        assertEquals(date("2026-04-10"), insight.prediction.fertileWindow?.endInclusive)
    }

    @Test
    fun `四个以上稳定周期升级为 HIGH`() {
        val insight = analyze(
            "2026-07-01",
            "2026-01-01",
            "2026-01-29",
            "2026-02-26",
            "2026-03-26",
            "2026-04-23",
            "2026-05-21",
        )

        assertEquals(Confidence.HIGH, insight.prediction.confidence)
        assertEquals(5, insight.stats.validSampleCount)
    }

    @Test
    fun `周期波动超过七天降级为 LOW`() {
        // 周期长度：19、31、33 → 极差 14 天
        val insight = analyze("2026-06-01", "2026-01-01", "2026-01-20", "2026-02-20", "2026-03-25")

        assertEquals(14, insight.stats.variabilityDays)
        assertEquals(Confidence.LOW, insight.prediction.confidence)
        assertNull(insight.prediction.fertileWindow)
    }

    @Test
    fun `过期预测顺推到未来最近的一次`() {
        val insight = analyze("2026-06-20", "2026-01-01", "2026-01-29", "2026-02-26")

        val nextStart = insight.prediction.nextPeriodStart ?: error("应当给出预测")
        assertEquals(date("2026-07-16"), nextStart)
        assertTrue(nextStart.isAfter(date("2026-06-20")))
    }

    @Test
    fun `长期未记录导致顺推超过上限时不给预测但保留置信度`() {
        val insight = analyze("2030-01-01", "2026-01-01", "2026-01-29")

        // 仅一个有效周期长度 → LOW；但因顺推超过 24 次，日期字段全部为空
        assertEquals(Confidence.LOW, insight.prediction.confidence)
        assertNull(insight.prediction.nextPeriodStart)
        assertTrue(insight.prediction.predictedPeriodDays.isEmpty())
    }

    @Test
    fun `全部样本落在有效区间之外时不预测且如实计数`() {
        // 周期长度 10 天与 70 天，都超出 15–60 天
        val insight = analyze("2026-06-01", "2026-01-01", "2026-01-11", "2026-03-22")

        assertEquals(Confidence.NONE, insight.prediction.confidence)
        assertEquals(2, insight.stats.excludedCycleCount)
        assertEquals(0, insight.stats.validSampleCount)
        assertNull(insight.prediction.nextPeriodStart)
    }

    @Test
    fun `未来日期的标记不参与统计`() {
        val insight = analyze("2026-04-01", "2026-01-01", "2026-01-29", "2026-05-01")

        assertEquals(1, insight.stats.validSampleCount)
        assertEquals(date("2026-04-23"), insight.prediction.nextPeriodStart)
    }

    @Test
    fun `经期长度取最近若干段的平均并在预测区间上夹取上限`() {
        val insight = CyclePredictor.analyze(
            listOf(
                // 第 1 段：1 月 1 日 — 1 月 13 日（13 天）
                date("2026-01-01"), date("2026-01-02"), date("2026-01-03"), date("2026-01-04"),
                date("2026-01-05"), date("2026-01-06"), date("2026-01-07"), date("2026-01-08"),
                date("2026-01-09"), date("2026-01-10"), date("2026-01-11"), date("2026-01-12"),
                date("2026-01-13"),
                // 第 2 段：1 月 29 日 — 2 月 9 日（12 天）
                date("2026-01-29"), date("2026-01-30"), date("2026-01-31"), date("2026-02-01"),
                date("2026-02-02"), date("2026-02-03"), date("2026-02-04"), date("2026-02-05"),
                date("2026-02-06"), date("2026-02-07"), date("2026-02-08"), date("2026-02-09"),
                // 第 3 段：2 月 26 日 — 3 月 12 日（15 天）
                date("2026-02-26"), date("2026-02-27"), date("2026-02-28"), date("2026-03-01"),
                date("2026-03-02"), date("2026-03-03"), date("2026-03-04"), date("2026-03-05"),
                date("2026-03-06"), date("2026-03-07"), date("2026-03-08"), date("2026-03-09"),
                date("2026-03-10"), date("2026-03-11"), date("2026-03-12"),
            ),
            date("2026-05-01"),
        )

        // 周期长度 28、28 → 稳定；经期长度 13、12、15 → 平均 13 天
        assertEquals(Confidence.MEDIUM, insight.prediction.confidence)
        assertEquals(13, insight.stats.averagePeriodLength)
        // 预测区间被夹取到上限 10 天
        assertEquals(10, insight.prediction.predictedPeriodDays.size)
    }
}
