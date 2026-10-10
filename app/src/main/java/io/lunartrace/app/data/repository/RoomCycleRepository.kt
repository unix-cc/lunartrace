package io.lunartrace.app.data.repository

import io.lunartrace.app.data.local.EnumCodec
import io.lunartrace.app.data.local.dao.PeriodDayDao
import io.lunartrace.app.data.local.dao.SymptomEntryDao
import io.lunartrace.app.data.local.entity.PeriodDayEntity
import io.lunartrace.app.data.local.entity.SymptomEntryEntity
import io.lunartrace.app.domain.model.FlowIntensity
import io.lunartrace.app.domain.model.PeriodDay
import io.lunartrace.app.domain.model.SymptomEntry
import io.lunartrace.app.domain.repository.CycleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * [CycleRepository] 的 Room 实现。只做"映射 + 一行写"，不含业务规则。
 * 注意：`domain` 不认识本类，UI 也只认识接口。
 */
class RoomCycleRepository(
    private val periodDayDao: PeriodDayDao,
    private val symptomEntryDao: SymptomEntryDao,
) : CycleRepository {

    override fun observePeriodDays(): Flow<List<PeriodDay>> =
        periodDayDao.observeAll().map { entities -> entities.mapNotNull { it.toDomain() } }

    override fun observeSymptomEntries(): Flow<List<SymptomEntry>> =
        symptomEntryDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun setPeriodDay(date: LocalDate, flow: FlowIntensity) = withContext(Dispatchers.IO) {
        periodDayDao.upsert(PeriodDayEntity(date = date, flow = flow.name))
    }

    override suspend fun clearPeriodDay(date: LocalDate) = withContext(Dispatchers.IO) {
        periodDayDao.deleteByDate(date)
    }

    override suspend fun upsertSymptomEntry(entry: SymptomEntry) = withContext(Dispatchers.IO) {
        if (entry.isEmpty) {
            symptomEntryDao.deleteByDate(entry.date)
        } else {
            symptomEntryDao.upsert(entry.toEntity())
        }
    }

    override suspend fun clearSymptomEntry(date: LocalDate) = withContext(Dispatchers.IO) {
        symptomEntryDao.deleteByDate(date)
    }

    override suspend fun wipeAll() = withContext(Dispatchers.IO) {
        periodDayDao.deleteAll()
        symptomEntryDao.deleteAll()
    }

    private fun SymptomEntry.toEntity(): SymptomEntryEntity = SymptomEntryEntity(
        date = date,
        symptoms = EnumCodec.encodeSymptoms(symptoms),
        mood = EnumCodec.encodeMood(mood),
        note = note.trim().take(SymptomEntry.MAX_NOTE_LENGTH).ifBlank { null },
    )

    private fun PeriodDayEntity.toDomain(): PeriodDay? {
        // 未知流量名视为无效行并忽略：既不抛异常，也不伪造一个流量值。
        val parsedFlow = EnumCodec.decodeFlow(flow) ?: return null
        return PeriodDay(date = date, flow = parsedFlow)
    }

    private fun SymptomEntryEntity.toDomain(): SymptomEntry = SymptomEntry(
        date = date,
        symptoms = EnumCodec.decodeSymptoms(symptoms),
        mood = EnumCodec.decodeMood(mood),
        note = note.orEmpty(),
    )
}
