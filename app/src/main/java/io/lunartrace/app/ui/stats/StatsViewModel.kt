package io.lunartrace.app.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.lunartrace.app.core.time.Today
import io.lunartrace.app.data.prefs.SettingsStore
import io.lunartrace.app.domain.model.CycleDefaults
import io.lunartrace.app.domain.model.CycleInsight
import io.lunartrace.app.domain.model.Symptom
import io.lunartrace.app.domain.model.SymptomEntry
import io.lunartrace.app.domain.prediction.CyclePredictor
import io.lunartrace.app.domain.repository.CycleRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

/** 症状频次。口径：全部症状条目中出现的次数，按次数降序，次数相同按枚举声明顺序。 */
data class SymptomFrequency(
    val symptom: Symptom,
    val count: Int,
)

data class StatsUiState(
    val insight: CycleInsight,
    val symptomFrequencies: List<SymptomFrequency>,
    val periodDayCount: Int,
    val symptomEntryCount: Int,
    val noteCount: Int,
)

class StatsViewModel(
    private val repository: CycleRepository,
    private val today: Today,
    private val settingsStore: SettingsStore,
) : ViewModel() {

    val uiState: StateFlow<StatsUiState> = combine(
        repository.observePeriodDays(),
        repository.observeSymptomEntries(),
        settingsStore.cycleDefaults,
    ) { periods, entries, defaults ->
        buildState(entries, periods.map { it.date }, defaults)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = buildState(emptyList(), emptyList(), CycleDefaults.none),
    )

    /** 注意：缺省值只影响 `insight`（预测与阶段），**不影响任何统计量**——统计只由实测样本产生。 */
    private fun buildState(
        entries: List<SymptomEntry>,
        periodDates: List<LocalDate>,
        defaults: CycleDefaults,
    ): StatsUiState {
        val filledEntries = entries.filterNot { it.isEmpty }
        return StatsUiState(
            insight = CyclePredictor.analyze(periodDates, today.date(), defaults),
            symptomFrequencies = symptomFrequencies(filledEntries),
            periodDayCount = periodDates.size,
            symptomEntryCount = filledEntries.size,
            noteCount = filledEntries.count { it.note.isNotBlank() },
        )
    }

    private fun symptomFrequencies(entries: List<SymptomEntry>): List<SymptomFrequency> {
        val counts = mutableMapOf<Symptom, Int>()
        entries.forEach { entry ->
            entry.symptoms.forEach { symptom -> counts[symptom] = (counts[symptom] ?: 0) + 1 }
        }
        return counts
            .map { (symptom, count) -> SymptomFrequency(symptom, count) }
            .sortedWith(compareByDescending<SymptomFrequency> { it.count }.thenBy { it.symptom.ordinal })
            .take(TOP_SYMPTOM_COUNT)
    }

    private companion object {
        const val TOP_SYMPTOM_COUNT = 5
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
