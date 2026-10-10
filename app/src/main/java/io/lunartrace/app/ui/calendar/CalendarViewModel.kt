package io.lunartrace.app.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.lunartrace.app.core.time.Today
import io.lunartrace.app.data.prefs.SettingsStore
import io.lunartrace.app.domain.model.CycleDefaults
import io.lunartrace.app.domain.model.FlowIntensity
import io.lunartrace.app.domain.model.Mood
import io.lunartrace.app.domain.model.PeriodDay
import io.lunartrace.app.domain.model.Symptom
import io.lunartrace.app.domain.model.SymptomEntry
import io.lunartrace.app.domain.prediction.CyclePredictor
import io.lunartrace.app.domain.repository.CycleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

/**
 * 日历页状态机。所有派生（预测、标记）都在这里算完，Composable 只负责画。
 */
class CalendarViewModel(
    private val repository: CycleRepository,
    private val today: Today,
    private val settingsStore: SettingsStore,
) : ViewModel() {

    private val month = MutableStateFlow(YearMonth.from(today.date()))
    private val selection = MutableStateFlow<DayDraft?>(null)

    private val records = combine(
        repository.observePeriodDays(),
        repository.observeSymptomEntries(),
        settingsStore.cycleDefaults,
    ) { periods, entries, defaults -> Triple(periods, entries, defaults) }

    val uiState: StateFlow<CalendarUiState> = combine(records, month, selection) { (periods, entries, defaults), currentMonth, draft ->
        buildState(currentMonth, periods, entries, defaults, draft)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = buildState(
            month = YearMonth.from(today.date()),
            periods = emptyList(),
            entries = emptyList(),
            defaults = CycleDefaults.none,
            draft = null,
        ),
    )

    /** 弹层的动作集合，供 Composable 直接使用。 */
    fun dayRecordActions(): DayRecordActions = DayRecordActions(
        onFlowSelected = ::onFlowSelected,
        onPeriodCleared = ::onPeriodCleared,
        onSymptomToggled = ::onSymptomToggled,
        onMoodToggled = ::onMoodToggled,
        onNoteChanged = ::onNoteChanged,
        onSave = ::onSave,
        onClearDay = ::onClearDay,
        onDismiss = ::onSheetDismissed,
    )

    fun onPreviousMonth() = month.update { it.minusMonths(1) }

    fun onNextMonth() = month.update { it.plusMonths(1) }

    fun onJumpToToday() {
        month.value = YearMonth.from(today.date())
    }

    fun onDateSelected(date: LocalDate) {
        val state = uiState.value
        val entry = state.entries[date]
        selection.value = DayDraft(
            date = date,
            flow = state.periods[date],
            symptoms = entry?.symptoms.orEmpty(),
            mood = entry?.mood,
            note = entry?.note.orEmpty(),
        )
    }

    fun onTodaySelected() = onDateSelected(today.date())

    /**
     * 零记录用户的入口：把选中的日期写成一条**正常的经期记录**（不另建设置项，避免出现两个事实源），
     * 并立刻打开该日的记录弹层，让用户看到并可修改预填的流量。
     *
     * 这里直接构造草稿而不是回读 `uiState`：写库是异步的，回读可能读到还没刷新的状态。
     */
    fun onLastPeriodStartPicked(date: LocalDate) {
        viewModelScope.launch {
            repository.setPeriodDay(date, FlowIntensity.MEDIUM)
            month.value = YearMonth.from(date)
            selection.value = DayDraft(
                date = date,
                flow = FlowIntensity.MEDIUM,
                symptoms = emptySet(),
                mood = null,
                note = "",
            )
        }
    }

    fun onSheetDismissed() {
        selection.value = null
    }

    fun onFlowSelected(flow: FlowIntensity) = selection.update { it?.copy(flow = flow) }

    fun onPeriodCleared() = selection.update { it?.copy(flow = null) }

    fun onSymptomToggled(symptom: Symptom) = selection.update { draft ->
        draft?.copy(symptoms = if (symptom in draft.symptoms) draft.symptoms - symptom else draft.symptoms + symptom)
    }

    fun onMoodToggled(mood: Mood) = selection.update { draft ->
        draft?.copy(mood = if (draft.mood == mood) null else mood)
    }

    fun onNoteChanged(note: String) = selection.update {
        it?.copy(note = note.take(SymptomEntry.MAX_NOTE_LENGTH))
    }

    fun onSave() {
        val draft = selection.value ?: return
        viewModelScope.launch {
            val flow = draft.flow
            if (flow == null) {
                repository.clearPeriodDay(draft.date)
            } else {
                repository.setPeriodDay(draft.date, flow)
            }
            repository.upsertSymptomEntry(
                SymptomEntry(
                    date = draft.date,
                    symptoms = draft.symptoms,
                    mood = draft.mood,
                    note = draft.note.trim(),
                ),
            )
            selection.value = null
        }
    }

    fun onClearDay() {
        val draft = selection.value ?: return
        viewModelScope.launch {
            repository.clearPeriodDay(draft.date)
            repository.clearSymptomEntry(draft.date)
            selection.value = null
        }
    }

    private fun buildState(
        month: YearMonth,
        periods: List<PeriodDay>,
        entries: List<SymptomEntry>,
        defaults: CycleDefaults,
        draft: DayDraft?,
    ): CalendarUiState {
        val todayDate = today.date()
        val insight = CyclePredictor.analyze(periods.map { it.date }, todayDate, defaults)
        val periodDates = periods.associate { it.date to it.flow }
        val predictedDates = insight.prediction.predictedPeriodDays.toSet()
        val ovulationDates = setOfNotNull(insight.prediction.ovulationDay)
        val fertileWindow = insight.prediction.fertileWindow
        val symptomDates = entries.filterNot { it.isEmpty }.map { it.date }.toSet()

        return CalendarUiState(
            today = todayDate,
            month = month,
            days = MonthGridLayout.monthGrid(month).map { date ->
                CalendarDay(
                    date = date,
                    inCurrentMonth = YearMonth.from(date) == month,
                    marks = DayMarks(
                        period = date in periodDates,
                        predicted = date in predictedDates,
                        fertile = fertileWindow?.contains(date) == true,
                        ovulation = date in ovulationDates,
                        hasSymptoms = date in symptomDates,
                    ),
                )
            },
            insight = insight,
            periods = periodDates,
            entries = entries.associateBy { it.date },
            predictedDates = predictedDates,
            cycleDefaults = defaults,
            selected = draft,
        )
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
