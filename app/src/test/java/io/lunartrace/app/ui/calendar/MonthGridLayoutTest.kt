package io.lunartrace.app.ui.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/**
 * 日历网格布局的边界测试（纯函数，无需设备）。
 *
 * 红先绿后：本文件先于实现写出。写它时 `MonthGridLayout` 仍是"周一起始、无连段判定"，
 * 因此应当先看到失败，再补实现。
 */
class MonthGridLayoutTest {

    private fun date(text: String): LocalDate = LocalDate.parse(text)

    @Test
    fun `一周从周日开始`() {
        // 2024-01-01 是周一 → 首格必须是 2023-12-31（周日）
        val grid = MonthGridLayout.monthGrid(YearMonth.of(2024, 1))

        assertEquals(date("2023-12-31"), grid.first())
    }

    @Test
    fun `当月一日恰好是周日时首格就是一日`() {
        // 2024-09-01 是周日
        val grid = MonthGridLayout.monthGrid(YearMonth.of(2024, 9))

        assertEquals(date("2024-09-01"), grid.first())
    }

    @Test
    fun `网格恒为 42 格并覆盖到下月中旬`() {
        val grid = MonthGridLayout.monthGrid(YearMonth.of(2024, 2))

        assertEquals(42, grid.size)
        assertEquals(date("2024-01-28"), grid.first())
        assertEquals(date("2024-03-09"), grid.last())
    }

    @Test
    fun `跨年月份正确`() {
        val grid = MonthGridLayout.monthGrid(YearMonth.of(2025, 12))

        // 2025-12-01 是周一 → 首格为 2025-11-30（周日）
        assertEquals(date("2025-11-30"), grid.first())
        assertEquals(date("2026-01-10"), grid.last())
    }

    @Test
    fun `单日标记两端都要倒角`() {
        val edges = MonthGridLayout.ribbonEdges(setOf(date("2026-03-01")), date("2026-03-01"))

        assertTrue(edges.start)
        assertTrue(edges.end)
    }

    @Test
    fun `连续三天的中间一天不倒角`() {
        val dates = setOf(date("2026-03-01"), date("2026-03-02"), date("2026-03-03"))

        val first = MonthGridLayout.ribbonEdges(dates, date("2026-03-01"))
        val middle = MonthGridLayout.ribbonEdges(dates, date("2026-03-02"))
        val last = MonthGridLayout.ribbonEdges(dates, date("2026-03-03"))

        assertTrue(first.start)
        assertFalse(first.end)
        assertFalse(middle.start)
        assertFalse(middle.end)
        assertFalse(last.start)
        assertTrue(last.end)
    }

    @Test
    fun `相隔一天的两段不会连成一条带`() {
        // 3 月 1 日与 3 月 3 日之间隔了 2 日：网格上不连续，不应互相连接
        val dates = setOf(date("2026-03-01"), date("2026-03-03"))

        assertTrue(MonthGridLayout.ribbonEdges(dates, date("2026-03-01")).end)
        assertTrue(MonthGridLayout.ribbonEdges(dates, date("2026-03-03")).start)
    }
}
