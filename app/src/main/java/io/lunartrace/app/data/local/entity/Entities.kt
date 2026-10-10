package io.lunartrace.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * 经期日。**日期是主键**：一天最多一条（见 `specs/capabilities/local-data-model.md` §2）。
 */
@Entity(tableName = "period_days")
data class PeriodDayEntity(
    @PrimaryKey val date: LocalDate,
    val flow: String,
)

/**
 * 症状条目。`symptoms` 是排序后的逗号分隔枚举名；空集合应当删行，不存空串行。
 */
@Entity(tableName = "symptom_entries")
data class SymptomEntryEntity(
    @PrimaryKey val date: LocalDate,
    val symptoms: String,
    val mood: String?,
    val note: String?,
)
