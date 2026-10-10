package io.lunartrace.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.lunartrace.app.data.local.entity.PeriodDayEntity
import io.lunartrace.app.data.local.entity.SymptomEntryEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * DAO 保持"薄"：只有查询与写入，没有业务逻辑。
 * 因此本仓库不为 DAO 写自动化测试（接缝上移到 `domain/prediction` 与 `data/security`，
 * 理由记录在 `specs/changes/mvp-period-tracking/spec.md` 的 Testing Decisions）。
 */
@Dao
interface PeriodDayDao {

    @Query("SELECT * FROM period_days ORDER BY date ASC")
    fun observeAll(): Flow<List<PeriodDayEntity>>

    /** "一天一条"语义：冲突时覆盖流量。 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PeriodDayEntity)

    @Query("DELETE FROM period_days WHERE date = :date")
    suspend fun deleteByDate(date: LocalDate)

    @Query("DELETE FROM period_days")
    suspend fun deleteAll()
}

@Dao
interface SymptomEntryDao {

    @Query("SELECT * FROM symptom_entries ORDER BY date ASC")
    fun observeAll(): Flow<List<SymptomEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SymptomEntryEntity)

    @Query("DELETE FROM symptom_entries WHERE date = :date")
    suspend fun deleteByDate(date: LocalDate)

    @Query("DELETE FROM symptom_entries")
    suspend fun deleteAll()
}
