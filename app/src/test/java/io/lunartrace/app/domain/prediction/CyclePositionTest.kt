package io.lunartrace.app.domain.prediction

import io.lunartrace.app.domain.model.CyclePhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/**
 * 今日位置（周期第几天 / 阶段）的边界测试。
 * 每一条对应 `specs/capabilities/cycle-prediction.md` §3 的一行。
 *
 * 本文件是"红先绿后"的载体：实现补齐之前，整组用例必须是红的。
 */
class CyclePositionTest {

    private fun date(text: String): LocalDate = LocalDate.parse(text)

    private fun position(today: String, vararg periodDays: String) =
        CyclePredictor.analyze(periodDays.map(::date), date(today)).position

    @Test
    fun `没有任何记录时没有位置`() {
        val position = position("2026-04-01")

        assertNull(position.cycleDay)
        assertEquals(CyclePhase.UNKNOWN, position.phase)
    }

    @Test
    fun `只有一段经期时给出周期天数但不推算阶段`() {
        // 事实可以给：今天距最后一段起始日 32 天；推算不给：0 个有效周期长度 → 无排卵日
        val position = position("2026-04-01", "2026-03-01")

        assertEquals(32, position.cycleDay)
        assertEquals(CyclePhase.UNKNOWN, position.phase)
    }

    @Test
    fun `今天仍在经期段内属于经期期`() {
        val position = position(
            "2026-03-03",
            "2026-03-01",
            "2026-03-02",
            "2026-03-03",
            "2026-03-04",
            "2026-03-05",
        )

        assertEquals(3, position.cycleDay)
        assertEquals(CyclePhase.MENSTRUATION, position.phase)
    }

    @Test
    fun `经期结束之后排卵日之前属于卵泡期`() {
        // 周期长度 28、28、28 → MEDIUM；最后一段起于 3 月 26 日 → 下次经期 4 月 23 日 → 排卵日 4 月 9 日
        val position = position("2026-03-28", "2026-01-01", "2026-01-29", "2026-02-26", "2026-03-26")

        assertEquals(3, position.cycleDay)
        assertEquals(CyclePhase.FOLLICULAR, position.phase)
    }

    @Test
    fun `排卵日当天及其前后一天属于排卵期`() {
        val around = listOf("2026-04-08", "2026-04-09", "2026-04-10")

        around.forEach { today ->
            val position = position(today, "2026-01-01", "2026-01-29", "2026-02-26", "2026-03-26")
            assertEquals("排卵期判定错误：$today", CyclePhase.OVULATION, position.phase)
        }

        assertEquals(15, position("2026-04-09", "2026-01-01", "2026-01-29", "2026-02-26", "2026-03-26").cycleDay)
    }

    @Test
    fun `排卵日两天之后属于黄体期`() {
        val position = position("2026-04-12", "2026-01-01", "2026-01-29", "2026-02-26", "2026-03-26")

        assertEquals(18, position.cycleDay)
        assertEquals(CyclePhase.LUTEAL, position.phase)
    }

    @Test
    fun `周期波动过大时不给阶段但周期天数照给`() {
        // 周期长度 19、31、33 → 极差 14 天 → LOW → 没有排卵日
        val position = position("2026-04-01", "2026-01-01", "2026-01-20", "2026-02-20", "2026-03-25")

        assertEquals(8, position.cycleDay)
        assertEquals(CyclePhase.UNKNOWN, position.phase)
    }

    @Test
    fun `长期未记录时周期天数照实很大且不给阶段`() {
        // 顺推超过 24 次 → 没有下次经期 → 没有排卵日
        // 手算：2026-01-01 → 2030-01-01 为 1461 天（2028 闰）；最后一段起于 2026-01-29（第 28 天）
        //      → 2030-01-01 − 2026-01-29 = 1433 天 → 周期第 1434 天
        val position = position("2030-01-01", "2026-01-01", "2026-01-29")

        assertEquals(1434, position.cycleDay)
        assertEquals(CyclePhase.UNKNOWN, position.phase)
    }

    @Test
    fun `跨年与跨月的周期定位正确`() {
        // 经期段跨年：2025-12-30 .. 2026-01-02
        val inPeriod = position("2026-01-01", "2025-12-30", "2025-12-31", "2026-01-01", "2026-01-02")
        assertEquals(3, inPeriod.cycleDay)
        assertEquals(CyclePhase.MENSTRUATION, inPeriod.phase)

        // 周期长度 28、28 → MEDIUM；最后一段起于 2 月 24 日 → 下次经期 3 月 24 日 → 排卵日 3 月 10 日
        val afterPeriod = position("2026-02-26", "2025-12-30", "2026-01-27", "2026-02-24")
        assertEquals(3, afterPeriod.cycleDay)
        assertEquals(CyclePhase.FOLLICULAR, afterPeriod.phase)
    }
}
