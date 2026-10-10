package io.lunartrace.app.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * 文案里的日期格式。红先绿后：本文件先于实现写出（实现里还只有日期、没有星期）。
 *
 * 星期是最容易 off-by-one 的地方，所以周末两端各押一例。
 */
class LabelsTest {

    private fun date(text: String): LocalDate = LocalDate.parse(text)

    @Test
    fun `周六日期带周六`() {
        assertEquals("2026年10月10日 周六", date("2026-10-10").fullDateWithWeekdayText())
    }

    @Test
    fun `周日日期带周日`() {
        assertEquals("2026年10月11日 周日", date("2026-10-11").fullDateWithWeekdayText())
    }

    @Test
    fun `周一日期带周一`() {
        assertEquals("2026年10月5日 周一", date("2026-10-05").fullDateWithWeekdayText())
    }

    @Test
    fun `跨年日期正确`() {
        // 2026-01-01 是周四
        assertEquals("2026年1月1日 周四", date("2026-01-01").fullDateWithWeekdayText())
    }
}
