package io.lunartrace.app.domain.prediction

import io.lunartrace.app.domain.model.Confidence
import io.lunartrace.app.domain.model.CycleDefaults
import io.lunartrace.app.domain.model.CycleInsight
import io.lunartrace.app.domain.model.CyclePhase
import io.lunartrace.app.domain.model.CyclePosition
import io.lunartrace.app.domain.model.CyclePrediction
import io.lunartrace.app.domain.model.CycleStats
import io.lunartrace.app.domain.model.PredictionBasis
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

    /** 排卵阶段的展示半径（天）：排卵日 ±1 视为 `OVULATION`。只用于阶段标签，不作为区间展示。 */
    const val OVULATION_PHASE_RADIUS_DAYS = 1L

    /**
     * 会出现阶段与排卵日所需的最少经期段数：MEDIUM 需要 2 个有效周期长度，而 N 个周期长度需要 N+1 段经期。
     * 仅用于向用户说明"还差几次记录"，不参与任何推算。
     */
    const val MIN_EPISODES_FOR_PHASE = 3

    /** 置信度判定的极差阈值与样本数阈值。 */
    const val STABLE_VARIABILITY_DAYS = 4
    const val UNSTABLE_VARIABILITY_DAYS = 7
    const val HIGH_CONFIDENCE_MIN_SAMPLES = 4

    /**
     * 分析一组经期日。
     *
     * @param periodDays 全部经期日，允许包含未来日期（会被忽略）。
     * @param today 本次计算依据的"今天"，由 `core/time/Today` 提供。
     * @param defaults 用户在设置里填写的缺省值；只在没有有效样本时兜底（ADR-0005）。
     */
    fun analyze(
        periodDays: Collection<LocalDate>,
        today: LocalDate,
        defaults: CycleDefaults = CycleDefaults.none,
    ): CycleInsight {
        val episodes = CycleMath.episodes(periodDays.filterNot { it.isAfter(today) })
        val allCycleLengths = CycleMath.cycleLengths(episodes)
        val validCycleLengths = allCycleLengths.filter { it in MIN_VALID_CYCLE_LENGTH..MAX_VALID_CYCLE_LENGTH }
        val window = validCycleLengths.takeLast(RECENT_SAMPLE_WINDOW)

        val stats = CycleStats(
            averageCycleLength = CycleMath.average(window),
            medianCycleLength = CycleMath.median(window),
            recentCycleLengths = window,
            averagePeriodLength = CycleMath.average(episodes.map { it.lengthDays }.takeLast(RECENT_SAMPLE_WINDOW)),
            episodeCount = episodes.size,
            excludedCycleCount = allCycleLengths.size - validCycleLengths.size,
            validSampleCount = window.size,
            variabilityDays = variability(window),
        )
        val confidence = confidenceOf(stats)
        val prediction = predict(episodes, stats, confidence, defaults, today)
        return CycleInsight(
            stats = stats,
            prediction = prediction,
            position = positionOf(episodes, prediction, today),
        )
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

    /**
     * 生成预测。依据的优先级（ADR-0005）：
     * 实测有效样本 → 用户填写的缺省周期长度（需通过范围校验）→ 不预测。
     *
     * 注意：`averageCycleLength` 为 `null` 时就意味着有效样本为 0（即 [Confidence.NONE]），
     * 所以不需要额外的置信度判断；兜底路径把置信度显式降为 [Confidence.LOW]。
     */
    private fun predict(
        episodes: List<PeriodEpisode>,
        stats: CycleStats,
        confidence: Confidence,
        defaults: CycleDefaults,
        today: LocalDate,
    ): CyclePrediction {
        val lastEpisode = episodes.lastOrNull() ?: return CyclePrediction.none
        val measuredCycleLength = stats.averageCycleLength
        val basisDays = measuredCycleLength ?: validatedDefaultCycleLength(defaults) ?: return CyclePrediction.none

        val basis = if (measuredCycleLength != null) PredictionBasis.RECORDS else PredictionBasis.USER_DEFAULT
        val effectiveConfidence = if (measuredCycleLength != null) confidence else Confidence.LOW

        val nextStart = nextPeriodStart(lastEpisode.start, basisDays.toLong(), today)
            ?: return CyclePrediction.none.copy(confidence = effectiveConfidence, basis = basis)

        // 经期长度同样遵循"实测优先"，但在兜底状态下**不用实测**：
        // 只有 1 段经期时"实测长度"往往只是"用户还没记完"，用它会把预测区间压到 3 天。
        val periodLengthBasis = if (basis == PredictionBasis.RECORDS) {
            stats.averagePeriodLength
        } else {
            validatedDefaultPeriodLength(defaults)
        }
        val periodLength = (periodLengthBasis ?: DEFAULT_PERIOD_LENGTH_DAYS)
            .coerceIn(MIN_PERIOD_LENGTH_DAYS, MAX_PERIOD_LENGTH_DAYS)

        // 排卵日与易孕窗口同生共死：置信度不足时两者都为 null，
        // 否则"只藏窗口、漏出排卵日"等于换个说法继续误导（见 cycle-prediction.md §2）。
        val ovulationDay = if (effectiveConfidence >= Confidence.MEDIUM) {
            nextStart.minusDays(LUTEAL_PHASE_DAYS.toLong())
        } else {
            null
        }

        return CyclePrediction(
            nextPeriodStart = nextStart,
            predictedPeriodDays = (0 until periodLength).map { nextStart.plusDays(it.toLong()) },
            ovulationDay = ovulationDay,
            fertileWindow = ovulationDay?.let(::fertileWindow),
            confidence = effectiveConfidence,
            basis = basis,
        )
    }

    /** 用户填写的周期长度：必须落在与有效样本**完全相同**的区间内，越界视为未填写（不夹取）。 */
    private fun validatedDefaultCycleLength(defaults: CycleDefaults): Int? =
        defaults.cycleLengthDays?.takeIf { it in MIN_VALID_CYCLE_LENGTH..MAX_VALID_CYCLE_LENGTH }

    /** 用户填写的经期长度：与预测区间的夹取范围一致，越界视为未填写。 */
    private fun validatedDefaultPeriodLength(defaults: CycleDefaults): Int? =
        defaults.periodLengthDays?.takeIf { it in MIN_PERIOD_LENGTH_DAYS..MAX_PERIOD_LENGTH_DAYS }

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

    /**
     * 今天在周期中的位置。
     *
     * [CyclePosition.cycleDay] 是事实（只要有经期记录就有值），
     * [CyclePosition.phase] 是推算（依赖 [CyclePrediction.ovulationDay]，即 `confidence ≥ MEDIUM`）。
     * 两者的门槛刻意不同，详见 ADR-0004。
     */
    private fun positionOf(
        episodes: List<PeriodEpisode>,
        prediction: CyclePrediction,
        today: LocalDate,
    ): CyclePosition {
        val lastEpisode = episodes.lastOrNull() ?: return CyclePosition.unknown
        return CyclePosition(
            cycleDay = (today.toEpochDay() - lastEpisode.start.toEpochDay() + 1).toInt(),
            phase = phaseOf(lastEpisode, prediction.ovulationDay, today),
        )
    }

    /** 阶段判定：先看实测（今天是否还在经期段内），再看推算（相对预计排卵日的位置）。 */
    private fun phaseOf(
        lastEpisode: PeriodEpisode,
        ovulationDay: LocalDate?,
        today: LocalDate,
    ): CyclePhase = when {
        !today.isAfter(lastEpisode.endInclusive) -> CyclePhase.MENSTRUATION
        ovulationDay == null -> CyclePhase.UNKNOWN
        today.isBefore(ovulationDay.minusDays(OVULATION_PHASE_RADIUS_DAYS)) -> CyclePhase.FOLLICULAR
        today.isAfter(ovulationDay.plusDays(OVULATION_PHASE_RADIUS_DAYS)) -> CyclePhase.LUTEAL
        else -> CyclePhase.OVULATION
    }

    private fun variability(window: List<Int>): Int? =
        if (window.size < 2) null else window.max() - window.min()
}
