package io.lunartrace.app.domain.repository

import io.lunartrace.app.domain.model.FlowIntensity
import io.lunartrace.app.domain.model.PeriodDay
import io.lunartrace.app.domain.model.SymptomEntry
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * 领域层唯一的数据出口。实现放在 `data/`，领域层与 UI 都不接触 DAO。
 *
 * 约定：查询返回 [Flow]（数据变化后 UI 自动刷新），写操作为 `suspend` 且幂等语义明确。
 */
interface CycleRepository {

    /** 全部经期记录，按日期升序。 */
    fun observePeriodDays(): Flow<List<PeriodDay>>

    /** 全部症状条目，按日期升序。 */
    fun observeSymptomEntries(): Flow<List<SymptomEntry>>

    /** 写入或覆盖某天的经期记录（"一天一条"语义）。 */
    suspend fun setPeriodDay(date: LocalDate, flow: FlowIntensity)

    /** 取消某天的经期标记。日期不存在时不做任何事。 */
    suspend fun clearPeriodDay(date: LocalDate)

    /** 写入某天的症状条目；[SymptomEntry.isEmpty] 为真时删除该行。 */
    suspend fun upsertSymptomEntry(entry: SymptomEntry)

    /** 删除某天的症状条目。 */
    suspend fun clearSymptomEntry(date: LocalDate)

    /** 真删除全部记录与设置。幂等。 */
    suspend fun wipeAll()
}
