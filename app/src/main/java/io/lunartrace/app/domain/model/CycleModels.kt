package io.lunartrace.app.domain.model

import java.time.LocalDate

/**
 * 一段连续的经期。允许已记录日之间最多缺失 1 天（见 `CycleMath.MERGED_GAP_TOLERANCE_DAYS`）。
 */
data class PeriodEpisode(
    val start: LocalDate,
    val endInclusive: LocalDate,
) {
    /** 经期长度（天）：两端闭区间，因此 +1。 */
    val lengthDays: Int
        get() = (endInclusive.toEpochDay() - start.toEpochDay() + 1).toInt()
}

/**
 * 预测可靠性的分档。描述的是**数据充分程度**，不是怀孕概率，也不含任何医学判断。
 * 顺序有意义（[NONE] 最弱），可直接比较大小。
 */
enum class Confidence {
    /** 无有效样本，不展示任何预测。 */
    NONE,

    /** 样本仅 1 个，或周期极差过大。 */
    LOW,

    /** 样本 ≥2 且周期波动在可接受范围，但未达到 [HIGH]。 */
    MEDIUM,

    /** 样本 ≥4 且极差 ≤4 天。 */
    HIGH,
}

/**
 * 周期统计量。全部字段都可能为 `null`，`null` 的含义是"数据不足以计算"，不是 0。
 */
data class CycleStats(
    /** 平均周期长度（最近 [CyclePredictor.RECENT_SAMPLE_WINDOW] 个有效样本的算术平均，四舍五入）。 */
    val averageCycleLength: Int?,
    /** 中位周期长度（同一窗口）。 */
    val medianCycleLength: Int?,
    /** 同一窗口内的有效周期长度，按时间顺序，用于折线图。 */
    val recentCycleLengths: List<Int>,
    /** 经期段的平均长度（最近 6 段，未夹取、未填默认值）。 */
    val averagePeriodLength: Int?,
    /** 因超出 `[15, 60]` 天而未参与预计的周期个数（全部历史累计）。 */
    val excludedCycleCount: Int,
    /** 参与预计的有效样本数。 */
    val validSampleCount: Int,
    /** 有效样本的极差（最大 − 最小，天）；样本少于 2 个时为 `null`。 */
    val variabilityDays: Int?,
)

/**
 * 下一次经期的预测结果。
 *
 * 不变量（见 `specs/capabilities/cycle-prediction.md`）：
 * - [confidence] 为 [Confidence.NONE] 时，本对象所有日期字段必须为 `null`、[predictedPeriodDays] 必须为空。
 * - [ovulationDay] 与 [fertileWindow] 同生共死：两者都在 [confidence] ≥ [Confidence.MEDIUM] 时才非空。
 * - [nextPeriodStart] 若存在，永远晚于"今天"。
 */
data class CyclePrediction(
    val nextPeriodStart: LocalDate?,
    val predictedPeriodDays: List<LocalDate>,
    val ovulationDay: LocalDate?,
    val fertileWindow: ClosedRange<LocalDate>?,
    val confidence: Confidence,
) {
    companion object {
        /** 无预测。 */
        val none = CyclePrediction(
            nextPeriodStart = null,
            predictedPeriodDays = emptyList(),
            ovulationDay = null,
            fertileWindow = null,
            confidence = Confidence.NONE,
        )
    }
}

/** 统计 + 预测的组合，UI 一次拿到全部派生结果。 */
data class CycleInsight(
    val stats: CycleStats,
    val prediction: CyclePrediction,
)
