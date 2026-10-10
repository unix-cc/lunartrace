package io.lunartrace.app.data.local

import androidx.room.TypeConverter
import java.time.LocalDate

/**
 * 日期 ↔ 存储值的**唯一**转换点。
 *
 * 存 `epochDay` 而不是毫秒时间戳：毫秒会随时区与夏令时漂移，让"同一天"变成两个值
 * （见 `specs/capabilities/local-data-model.md` §1）。
 */
class Converters {

    @TypeConverter
    fun toEpochDay(date: LocalDate?): Long? = date?.toEpochDay()

    @TypeConverter
    fun fromEpochDay(epochDay: Long?): LocalDate? = epochDay?.let(LocalDate::ofEpochDay)
}
