package io.lunartrace.app.ui.calendar

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/**
 * 一格在"胶囊横条"里的哪一端需要倒圆角。
 * [start] 为真表示左侧是这条带的起点，[end] 为真表示右侧是终点。
 */
data class RibbonEdges(
    val start: Boolean,
    val end: Boolean,
)

/**
 * 日历网格的布局计算：纯函数，可在 JVM 上单元测试（不涉及任何绘制）。
 *
 * 从 `CalendarViewModel` 的私有函数里搬出来，理由见 `specs/changes/calendar-visual-refresh/spec.md`：
 * 藏在 ViewModel 私有方法里的日期布局逻辑既不可测、又最容易出 off-by-one。
 */
object MonthGridLayout {

    const val CELLS_PER_ROW = 7
    const val CELL_COUNT = 42

    /** 一周的起始日。与参考设计一致：周日为一周起始。 */
    val WEEK_START: DayOfWeek = DayOfWeek.SUNDAY

    /** 固定 6 行 × 7 列，从 [WEEK_START] 起，首格可能落在上个月。 */
    fun monthGrid(month: YearMonth): List<LocalDate> {
        val firstDay = month.atDay(1)
        val leading = (firstDay.dayOfWeek.value - WEEK_START.value + CELLS_PER_ROW) % CELLS_PER_ROW
        val gridStart = firstDay.minusDays(leading.toLong())
        return (0 until CELL_COUNT).map { gridStart.plusDays(it.toLong()) }
    }

    /**
     * 判断某一天在标记带里的两端位置：相邻日也在同一集合里就不倒角，于是连续多天会连成一条带。
     * 注意只看**相邻**（差 1 天）：差 2 天在网格上不连续，不应互相连接。
     */
    fun ribbonEdges(dates: Set<LocalDate>, date: LocalDate): RibbonEdges = RibbonEdges(
        start = date.minusDays(1) !in dates,
        end = date.plusDays(1) !in dates,
    )
}
