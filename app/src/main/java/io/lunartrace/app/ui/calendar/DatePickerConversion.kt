package io.lunartrace.app.ui.calendar

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Material `DatePicker` 与 `LocalDate` 之间的转换。
 *
 * `DatePicker` 返回的是 **UTC 午夜**的毫秒值，因此这里必须固定按 UTC 解释；
 * 一旦按系统时区解释，跨时区就会整体差一天（本项目"日期身份"红线，见 `local-data-model.md` §1）。
 */
object DatePickerConversion {

    fun toLocalDate(utcMillis: Long): LocalDate =
        Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate()

    fun toUtcMillis(date: LocalDate): Long =
        date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}
