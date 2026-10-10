package io.lunartrace.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.lunartrace.app.data.local.dao.PeriodDayDao
import io.lunartrace.app.data.local.dao.SymptomEntryDao
import io.lunartrace.app.data.local.entity.PeriodDayEntity
import io.lunartrace.app.data.local.entity.SymptomEntryEntity

/**
 * 本地数据库。
 *
 * 不变量（见 `specs/capabilities/local-data-model.md`）：
 * - `exportSchema = true`，schema JSON 入库，用于人工核对表结构漂移。
 * - 改表结构必须提升 `version` 并写 `Migration`；**禁止** `fallbackToDestructiveMigration()`（会静默清空用户记录）。
 */
@Database(
    entities = [PeriodDayEntity::class, SymptomEntryEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class LunartraceDatabase : RoomDatabase() {
    abstract fun periodDayDao(): PeriodDayDao
    abstract fun symptomEntryDao(): SymptomEntryDao

    companion object {
        const val NAME = "lunartrace.db"
    }
}
