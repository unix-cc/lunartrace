package io.lunartrace.app.ui.calendar

import io.lunartrace.app.domain.model.CycleInsight
import io.lunartrace.app.domain.model.FlowIntensity
import io.lunartrace.app.domain.model.Mood
import io.lunartrace.app.domain.model.Symptom
import io.lunartrace.app.domain.model.SymptomEntry
import java.time.LocalDate
import java.time.YearMonth

/** 一天上可能出现的四类标记。它们可以叠加（例如实测经期同时落在易孕窗口内）。 */
data class DayMarks(
    val period: Boolean = false,
    val predicted: Boolean = false,
    val fertile: Boolean = false,
    val hasSymptoms: Boolean = false,
)

data class CalendarDay(
    val date: LocalDate,
    /** 是否属于当前展示的月份（网格会补齐首尾，用浅色显示相邻月份的日子）。 */
    val inCurrentMonth: Boolean,
    val marks: DayMarks,
)

/** 记录弹层的编辑中状态（尚未落库）。`flow == null` 表示"这一天不是经期"。 */
data class DayDraft(
    val date: LocalDate,
    val flow: FlowIntensity?,
    val symptoms: Set<Symptom>,
    val mood: Mood?,
    val note: String,
)

data class CalendarUiState(
    val today: LocalDate,
    val month: YearMonth,
    /** 固定 42 格（6 行 × 7 列），从周一开始。 */
    val days: List<CalendarDay>,
    val insight: CycleInsight,
    val periods: Map<LocalDate, FlowIntensity>,
    val entries: Map<LocalDate, SymptomEntry>,
    /** 非空表示记录弹层打开。 */
    val selected: DayDraft?,
)
