package io.lunartrace.app.domain.prediction

import io.lunartrace.app.domain.model.Confidence
import io.lunartrace.app.domain.model.CycleInsight
import io.lunartrace.app.domain.model.CyclePrediction
import io.lunartrace.app.domain.model.CycleStats
import io.lunartrace.app.domain.model.PeriodEpisode
import java.time.LocalDate

/**
 * 周期统计与预测。**纯函数**：不读系统时钟、不读数据库、不做 IO。
 *
 * 口径的唯一来源是 `specs/capabilities/cycle-prediction.md`；本类的常量与那里的表格一一对应，
 * 改动任何一个常量都属于"改产品语义"，会触发 Grill 工作流（`docs/agents/workflow.md`）。
 */
object CyclePredictor {

    /** 参与预计的历史窗口：最近 6 个有效周期样本。 */
    const val RECENT_SAMPLE_WINDOW = 6

    /** 有效周期长度区间；区间外的样本不参与预计，仅计数。 */
    const val MIN_VALID_CYCLE_LENGTH = 15
    const val MAX_VALID_CYCLE_LENGTH = 60

    /** 平均经期长度的缺省与夹取区间。 */
    const val DEFAULT_PERIOD_LENGTH_DAYS = 5
    const val MIN_PERIOD_LENGTH_DAYS = 3
    const val MAX_PERIOD_LENGTH_DAYS = 10

    /** 黄体期固定 14 天的群体假设（不构成医学结论）。 */
    const val LUTEAL_PHASE_DAYS = 14
    const val FERTILE_WINDOW_DAYS_BEFORE_OVULATION = 5
    const val FERTILE_WINDOW_DAYS_AFTER_OVULATION = 1

    /** 顺推上限：超过此次数仍追不上"今天"就放弃预测，而不是给出荒谬的远期日期。 */
    const val MAX_FORWARD_STEPS = 24

    /** 置信度判定的极差阈值与样本数阈值。 */
    const val STABLE_VARIABILITY_DAYS = 4
    const val UNSTABLE_VARIABILITY_DAYS = 7
    const val HIGH_CONFIDENCE_MIN_SAMPLES = 4

    /**
     * 分析一组经期日。
     *
     * @param periodDays 全部经期日，允许包含未来日期（会被忽略）。
     * @param today 本次计算依据的"今天"，由 `core/time/Today` 提供。
     */
    fun analyze(periodDays: Collection<LocalDate>, today: LocalDate): CycleInsight {
        val episodes = CycleMath.episodes(periodDays.filterNot { it.isAfter(today) })
        val allCycleLengths = CycleMath.cycleLengths(episodes)
        val validCycleLengths = allCycleLengths.filter { it in MIN_VALID_CYCLE_LENGTH..MAX_VALID_CYCLE_LENGTH }
        val window = validCycleLengths.takeLast(RECENT_SAMPLE_WINDOW)

        val stats = CycleStats(
            averageCycleLength = CycleMath.average(window),
            medianCycleLength = CycleMath.median(window),
            recentCycleLengths = window,
            averagePeriodLength = CycleMath.average(episodes.map { it.lengthDays }.takeLast(RECENT_SAMPLE_WINDOW)),
            excludedCycleCount = allCycleLengths.size - validCycleLengths.size,
            validSampleCount = window.size,
            variabilityDays = variability(window),
        )
        val confidence = confidenceOf(stats)
        return CycleInsight(stats = stats, prediction = predict(episodes, stats, confidence, today))
    }

    /** 置信度分档。样本不足或波动过大时主动降级，宁可不展示也不误导。 */
    fun confidenceOf(stats: CycleStats): Confidence = when {
        stats.validSampleCount == 0 -> Confidence.NONE
        stats.validSampleCount == 1 -> Confidence.LOW
        (stats.variabilityDays ?: 0) > UNSTABLE_VARIABILITY_DAYS -> Confidence.LOW
        stats.validSampleCount >= HIGH_CONFIDENCE_MIN_SAMPLES &&
            (stats.variabilityDays ?: Int.MAX_VALUE) <= STABLE_VARIABILITY_DAYS -> Confidence.HIGH
        else -> Confidence.MEDIUM
    }

    private fun predict(
        episodes: List<PeriodEpisode>,
        stats: CycleStats,
        confidence: Confidence,
        today: LocalDate,
    ): CyclePrediction {
        if (confidence == Confidence.NONE) return CyclePrediction.none

        val basisDays = stats.averageCycleLength ?: return CyclePrediction.none
        val lastStart = episodes.lastOrNull()?.start ?: return CyclePrediction.none
        val nextStart = nextPeriodStart(lastStart, basisDays.toLong(), today)
            ?: return CyclePrediction.none.copy(confidence = confidence)

        val periodLength = (stats.averagePeriodLength ?: DEFAULT_PERIOD_LENGTH_DAYS)
            .coerceIn(MIN_PERIOD_LENGTH_DAYS, MAX_PERIOD_LENGTH_DAYS)
        // 排卵日与易孕窗口同生共死：置信度不足时两者都为 null，
        // 否则"只藏窗口、漏出排卵日"等于换个说法继续误导（见 cycle-prediction.md §2）。
        val ovulationDay = if (confidence >= Confidence.MEDIUM) nextStart.minusDays(LUTEAL_PHASE_DAYS.toLong()) else null

        return CyclePrediction(
            nextPeriodStart = nextStart,
            predictedPeriodDays = (0 until periodLength).map { nextStart.plusDays(it.toLong()) },
            ovulationDay = ovulationDay,
            fertileWindow = ovulationDay?.let(::fertileWindow),
            confidence = confidence,
        )
    }

    private fun fertileWindow(ovulationDay: LocalDate): ClosedRange<LocalDate> {
        val windowStart = ovulationDay.minusDays(FERTILE_WINDOW_DAYS_BEFORE_OVULATION.toLong())
        val windowEnd = ovulationDay.plusDays(FERTILE_WINDOW_DAYS_AFTER_OVULATION.toLong())
        return windowStart..windowEnd
    }

    /** 从最后一段经期起始日顺推到**未来最近**的起点；超过上限返回 `null`。 */
    private fun nextPeriodStart(lastStart: LocalDate, basisDays: Long, today: LocalDate): LocalDate? {
        var candidate = lastStart.plusDays(basisDays)
        var steps = 0
        while (!candidate.isAfter(today)) {
            if (steps >= MAX_FORWARD_STEPS) return null
            candidate = candidate.plusDays(basisDays)
            steps++
        }
        return candidate
    }

    private fun variability(window: List<Int>): Int? =
        if (window.size < 2) null else window.max() - window.min()
}
