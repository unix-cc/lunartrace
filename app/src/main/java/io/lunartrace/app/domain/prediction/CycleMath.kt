package io.lunartrace.app.domain.prediction

import io.lunartrace.app.domain.model.PeriodEpisode
import java.time.LocalDate

/**
 * 经期分段与长度计算。全部为纯函数：同样的输入必然得到同样的输出。
 */
object CycleMath {

    /**
     * 两个**已记录日**之间最多允许缺失 1 天，也就是相差不超过 2 天时视为同一段。
     * 例：记为 3 月 1 日与 3 月 3 日 → 同一段；记为 3 月 1 日与 3 月 4 日 → 两段。
     */
    const val MERGED_GAP_TOLERANCE_DAYS = 2L

    /** 把经期日合并成经期段，按时间升序返回。 */
    fun episodes(periodDays: Collection<LocalDate>): List<PeriodEpisode> {
        if (periodDays.isEmpty()) return emptyList()

        val sorted = periodDays.sorted()
        val episodes = mutableListOf<PeriodEpisode>()
        var start = sorted.first()
        var end = sorted.first()

        for (day in sorted.drop(1)) {
            if (day.toEpochDay() - end.toEpochDay() <= MERGED_GAP_TOLERANCE_DAYS) {
                end = day
            } else {
                episodes += PeriodEpisode(start, end)
                start = day
                end = day
            }
        }
        episodes += PeriodEpisode(start, end)
        return episodes
    }

    /** 相邻两段**起始日**之差，按时间升序返回。段数少于 2 时返回空列表。 */
    fun cycleLengths(episodes: List<PeriodEpisode>): List<Int> =
        episodes.zipWithNext { previous, next ->
            (next.start.toEpochDay() - previous.start.toEpochDay()).toInt()
        }

    /** 中位数；偶数个元素取中间两个的平均值并四舍五入。空列表返回 `null`。 */
    fun median(values: List<Int>): Int? {
        if (values.isEmpty()) return null

        val sorted = values.sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 1) {
            sorted[middle]
        } else {
            Math.round((sorted[middle - 1] + sorted[middle]) / 2.0).toInt()
        }
    }

    /** 算术平均并四舍五入；空列表返回 `null`。 */
    fun average(values: List<Int>): Int? =
        if (values.isEmpty()) null else Math.round(values.average()).toInt()
}
