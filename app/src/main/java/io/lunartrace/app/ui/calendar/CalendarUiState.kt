package io.lunartrace.app.ui.calendar

import io.lunartrace.app.domain.model.CycleDefaults
import io.lunartrace.app.domain.model.CycleInsight
import io.lunartrace.app.domain.model.FlowIntensity
import io.lunartrace.app.domain.model.Mood
import io.lunartrace.app.domain.model.Symptom
import io.lunartrace.app.domain.model.SymptomEntry
import java.time.LocalDate
import java.time.YearMonth

/** 一天上可能出现的标记。它们可以叠加（例如实测经期同时落在易孕窗口内）。 */
data class DayMarks(
    val period: Boolean = false,
    val predicted: Boolean = false,
    val fertile: Boolean = false,
    /** 预计排卵日（单日）。仅 `confidence ≥ MEDIUM` 时可能为真。 */
    val ovulation: Boolean = false,
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
    /** 预计经期日集合：网格需要它来判断"胶囊横条"该在哪一天倒角（见 `MonthGridLayout.ribbonEdges`）。 */
    val predictedDates: Set<LocalDate>,
    /** 用户填写的缺省值：预测卡需要它来说明"依据来自你填写，而不是算出来的"（ADR-0005）。 */
    val cycleDefaults: CycleDefaults,
    /** 记录面板正在编辑哪一天。面板是内联的，所以永远有值（默认"今天"）。 */
    val selectedDate: LocalDate,
    /**
     * 选中日的显示值：本地覆盖层（尚在编辑、可能还没落库的部分）合并已存数据后的结果。
     * **面板必须读它，而不是直接读 [periods] / [entries]**，否则快速连点时会因为写库异步而丢写。
     */
    val record: DayDraft,
)
