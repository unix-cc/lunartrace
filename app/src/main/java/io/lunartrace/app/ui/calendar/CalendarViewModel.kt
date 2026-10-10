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
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
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
 * 日历与记录面板的状态机。
 *
 * 记录是**点选即时保存**的（没有保存按钮），这带来两个必须处理的现实问题，见
 * `specs/changes/inline-record-panel/spec.md`：
 * 1. 写库是异步的 → 显示值不能直接读数据库，否则快速连点会丢写。因此用 [localEdits] 作为
 *    当前选中日的本地覆盖层，显示值 = 覆盖层 ?: 由已存数据构造。
 * 2. 快速连点会并发写 → 所有写操作进入 [writeQueue] 这一个队列，按入队顺序依次执行；
 *    入队的是**完整行快照**，因此队列顺序就等价于最终状态。
 */
class CalendarViewModel(
    private val repository: CycleRepository,
    private val today: Today,
    private val settingsStore: SettingsStore,
) : ViewModel() {

    private val month = MutableStateFlow(YearMonth.from(today.date()))
    private val selectedDate = MutableStateFlow(today.date())

    /** 当前选中日的本地覆盖层；`null` 表示这一天还没被编辑过，显示值直接由已存数据构造。 */
    private val localEdits = MutableStateFlow<DayDraft?>(null)

    private val writeQueue = Channel<PendingWrite>(Channel.UNLIMITED)
    private var noteWriteJob: Job? = null

    init {
        viewModelScope.launch {
            for (write in writeQueue) {
                when (write) {
                    is PendingWrite.PeriodDay -> if (write.flow == null) {
                        repository.clearPeriodDay(write.date)
                    } else {
                        repository.setPeriodDay(write.date, write.flow)
                    }
                    is PendingWrite.Entry -> repository.upsertSymptomEntry(write.entry)
                }
            }
        }
    }

    private val records = combine(
        repository.observePeriodDays(),
        repository.observeSymptomEntries(),
        settingsStore.cycleDefaults,
    ) { periods, entries, defaults -> Triple(periods, entries, defaults) }

    val uiState: StateFlow<CalendarUiState> = combine(
        records,
        month,
        selectedDate,
        localEdits,
    ) { (periods, entries, defaults), currentMonth, date, edits ->
        buildState(currentMonth, periods, entries, defaults, date, edits)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = buildState(
            month = YearMonth.from(today.date()),
            periods = emptyList(),
            entries = emptyList(),
            defaults = CycleDefaults.none,
            selectedDate = today.date(),
            edits = null,
        ),
    )

    // ---- 月份导航 ----

    fun onPreviousMonth() = month.update { it.minusMonths(1) }

    fun onNextMonth() = month.update { it.plusMonths(1) }

    fun onJumpToToday() {
        month.value = YearMonth.from(today.date())
    }

    // ---- 选中日 ----

    /** 切换编辑对象。**切换前必须把防抖中的备注落库**，否则它会写到错误的那一天。 */
    fun onDateSelected(date: LocalDate) {
        if (date == selectedDate.value) return
        flushPendingNote()
        localEdits.value = null
        selectedDate.value = date
    }

    fun onTodaySelected() = onDateSelected(today.date())

    /**
     * 零记录用户的入口：把选中的日期写成一条正常的经期记录，并把它切到面板里编辑。
     *
     * 这里**显式构造**显示值，而不是走 `applyEdit`：刚切换选中日时组合流还没重算，
     * `uiState.value.record` 仍指向旧的选中日。
     */
    fun onLastPeriodStartPicked(date: LocalDate) {
        val storedEntry = uiState.value.entries[date]
        flushPendingNote()
        month.value = YearMonth.from(date)
        selectedDate.value = date
        localEdits.value = DayRecordEditing.fromStored(date, FlowIntensity.MEDIUM, storedEntry)
        writeQueue.trySend(PendingWrite.PeriodDay(date, FlowIntensity.MEDIUM))
    }

    // ---- 记录编辑（即时保存） ----

    fun onFlowSelected(flow: FlowIntensity) = applyEdit { DayRecordEditing.setFlow(it, flow) }

    fun onPeriodCleared() = applyEdit { DayRecordEditing.setFlow(it, null) }

    fun onSymptomToggled(symptom: Symptom) = applyEdit { DayRecordEditing.toggleSymptom(it, symptom) }

    fun onMoodToggled(mood: Mood) = applyEdit { DayRecordEditing.toggleMood(it, mood) }

    /** 备注逐字写入太吵，因此防抖；切换日期或清空时会强制落库（见 [flushPendingNote]）。 */
    fun onNoteChanged(note: String) {
        val updated = applyEdit { DayRecordEditing.withNote(it, note) } ?: return
        noteWriteJob?.cancel()
        noteWriteJob = viewModelScope.launch {
            delay(NOTE_DEBOUNCE_MILLIS)
            enqueueEntry(updated)
        }
    }

    fun onClearDay() {
        noteWriteJob?.cancel()
        noteWriteJob = null
        val date = selectedDate.value
        val cleared = DayRecordEditing.fromStored(date, flow = null, entry = null)
        localEdits.value = cleared
        writeQueue.trySend(PendingWrite.PeriodDay(date, flow = null))
        enqueueEntry(cleared)
    }

    fun dayRecordActions(): DayRecordActions = DayRecordActions(
        onFlowSelected = ::onFlowSelected,
        onPeriodCleared = ::onPeriodCleared,
        onSymptomToggled = ::onSymptomToggled,
        onMoodToggled = ::onMoodToggled,
        onNoteChanged = ::onNoteChanged,
        onClearDay = ::onClearDay,
    )

    /** 同步更新显示值并入队写库；返回更新后的显示值（`null` 只在没有选中日时，实际不会发生）。 */
    private fun applyEdit(transform: (DayDraft) -> DayDraft): DayDraft? {
        val date = selectedDate.value
        val base = localEdits.value?.takeIf { it.date == date }
            ?: uiState.value.record.takeIf { it.date == date }
            ?: DayRecordEditing.fromStored(date, uiState.value.periods[date], uiState.value.entries[date])
        val updated = transform(base)

        localEdits.value = updated
        writeQueue.trySend(PendingWrite.PeriodDay(updated.date, updated.flow))
        enqueueEntry(updated)
        return updated
    }

    /** 防抖窗口内的备注必须先落库，否则切换日期后它会写到错误的日期。 */
    private fun flushPendingNote() {
        val job = noteWriteJob ?: return
        job.cancel()
        noteWriteJob = null
        localEdits.value?.let(::enqueueEntry)
    }

    private fun enqueueEntry(draft: DayDraft) {
        writeQueue.trySend(
            PendingWrite.Entry(
                SymptomEntry(
                    date = draft.date,
                    symptoms = draft.symptoms,
                    mood = draft.mood,
                    note = draft.note.trim(),
                ),
            ),
        )
    }

    private fun buildState(
        month: YearMonth,
        periods: List<PeriodDay>,
        entries: List<SymptomEntry>,
        defaults: CycleDefaults,
        selectedDate: LocalDate,
        edits: DayDraft?,
    ): CalendarUiState {
        val todayDate = today.date()
        val insight = CyclePredictor.analyze(periods.map { it.date }, todayDate, defaults)
        val periodDates = periods.associate { it.date to it.flow }
        val entriesByDate = entries.associateBy { it.date }
        val predictedDates = insight.prediction.predictedPeriodDays.toSet()
        val ovulationDates = setOfNotNull(insight.prediction.ovulationDay)
        val fertileWindow = insight.prediction.fertileWindow
        val symptomDates = entries.filterNot { it.isEmpty }.map { it.date }.toSet()

        val record = edits ?: DayRecordEditing.fromStored(
            date = selectedDate,
            flow = periodDates[selectedDate],
            entry = entriesByDate[selectedDate],
        )

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
            entries = entriesByDate,
            predictedDates = predictedDates,
            cycleDefaults = defaults,
            ring = CycleRingData.of(
                prediction = insight.prediction,
                position = insight.position,
                periodDates = periodDates.keys,
                today = todayDate,
            ),
            selectedDate = selectedDate,
            record = record,
        )
    }

    /** 入队的写操作。只允许两种：改某天的经期状态、覆盖某天的整行症状条目。 */
    private sealed interface PendingWrite {
        data class PeriodDay(val date: LocalDate, val flow: FlowIntensity?) : PendingWrite

        data class Entry(val entry: SymptomEntry) : PendingWrite
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val NOTE_DEBOUNCE_MILLIS = 600L
    }
}
