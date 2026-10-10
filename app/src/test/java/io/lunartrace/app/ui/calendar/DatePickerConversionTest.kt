package io.lunartrace.app.ui.calendar

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * 日期选择器与 LocalDate 的转换。红先绿后：本文件先于实现写出。
 *
 * 期望值全部按 **UTC** 手算，因此本测试在任何时区的机器上都必须得到同样结果——
 * 这正是这个转换函数存在的意义（按系统时区解释会整体差一天）。
 */
class DatePickerConversionTest {

    private fun date(text: String): LocalDate = LocalDate.parse(text)

    @Test
    fun `纪元毫秒零对应 1970-01-01`() {
        assertEquals(date("1970-01-01"), DatePickerConversion.toLocalDate(0))
    }

    @Test
    fun `1970-01-01 对应纪元毫秒零`() {
        assertEquals(0L, DatePickerConversion.toUtcMillis(date("1970-01-01")))
    }

    @Test
    fun `若干日期往返一致`() {
        listOf("2024-02-29", "2025-12-31", "2026-03-01", "2026-10-10").forEach { text ->
            val original = date(text)
            val roundTripped = DatePickerConversion.toLocalDate(DatePickerConversion.toUtcMillis(original))

            assertEquals("往返后日期改变：$text", original, roundTripped)
        }
    }

    @Test
    fun `闰日正午的毫秒值仍归入同一天`() {
        // 1970-01-02 的 UTC 午夜 + 1 小时
        val noonish = 86_400_000L + 3_600_000L

        assertEquals(date("1970-01-02"), DatePickerConversion.toLocalDate(noonish))
    }

    @Test
    fun `跨年的毫秒值正确`() {
        val newYear = DatePickerConversion.toUtcMillis(date("2026-01-01"))

        assertEquals(date("2026-01-01"), DatePickerConversion.toLocalDate(newYear))
        assertEquals(date("2025-12-31"), DatePickerConversion.toLocalDate(newYear - 1))
    }
}
