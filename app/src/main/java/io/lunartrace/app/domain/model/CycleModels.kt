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
    /** 经期段总数。用于向用户说明"还差几次记录就能看到预计"（事实计数，不含推算）。 */
    val episodeCount: Int,
    /** 因超出 `[15, 60]` 天而未参与预计的周期个数（全部历史累计）。 */
    val excludedCycleCount: Int,
    /** 参与预计的有效样本数。 */
    val validSampleCount: Int,
    /** 有效样本的极差（最大 − 最小，天）；样本少于 2 个时为 `null`。 */
    val variabilityDays: Int?,
)

/**
 * 预测的依据来源。UI 必须显式处理三种情况：**不能假定"有日期就一定是算出来的"**。
 */
enum class PredictionBasis {
    /** 没有依据，不预测。 */
    NONE,

    /** 用户自己在设置里填写的缺省周期长度（不是从记录算出来的，见 ADR-0005）。 */
    USER_DEFAULT,

    /** 从实际记录的有效样本算出来的。 */
    RECORDS,
}

/**
 * 用户在设置里填写的缺省值；`null` 表示**未填写**。
 *
 * 刻意不预置 28/5：预置会制造"这个数字是应用测出来的"的错觉（见 ADR-0005）。
 */
data class CycleDefaults(
    val cycleLengthDays: Int?,
    val periodLengthDays: Int?,
) {
    companion object {
        val none = CycleDefaults(cycleLengthDays = null, periodLengthDays = null)
    }
}

/**
 * 下一次经期的预测结果。
 *
 * 不变量（见 `specs/capabilities/cycle-prediction.md`）：
 * - [confidence] 为 [Confidence.NONE] 时，本对象所有日期字段必须为 `null`、[predictedPeriodDays] 必须为空。
 * - [basis] 为 [PredictionBasis.NONE] 时同上。
 * - [ovulationDay] 与 [fertileWindow] 同生共死：两者都在 [confidence] ≥ [Confidence.MEDIUM] 时才非空。
 * - [nextPeriodStart] 若存在，永远晚于"今天"。
 */
data class CyclePrediction(
    val nextPeriodStart: LocalDate?,
    val predictedPeriodDays: List<LocalDate>,
    val ovulationDay: LocalDate?,
    val fertileWindow: ClosedRange<LocalDate>?,
    val confidence: Confidence,
    val basis: PredictionBasis = PredictionBasis.NONE,
    /**
     * 本次预测实际使用的基准周期长度（实测均值，或用户填写的缺省值）。
     * **不改变任何预测行为**，只是把"用了多少天当基准"说出来，供周期环与文案使用。
     */
    val basisCycleLengthDays: Int? = null,
) {
    companion object {
        /** 无预测。 */
        val none = CyclePrediction(
            nextPeriodStart = null,
            predictedPeriodDays = emptyList(),
            ovulationDay = null,
            fertileWindow = null,
            confidence = Confidence.NONE,
            basis = PredictionBasis.NONE,
            basisCycleLengthDays = null,
        )
    }
}

/** 统计 + 预测 + 今日位置的组合，UI 一次拿到全部派生结果。 */
data class CycleInsight(
    val stats: CycleStats,
    val prediction: CyclePrediction,
    val position: CyclePosition,
)
