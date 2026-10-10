package io.lunartrace.app.domain.model

/**
 * 周期阶段。用于告诉用户"现在处在周期的哪一段"，不给任何医学结论。
 *
 * 划分依据是"今天相对于本周期**预计**排卵日的位置"（见 `specs/capabilities/cycle-prediction.md` §3）：
 * 排卵日与阶段都依赖推算，只在 `Confidence ≥ MEDIUM` 时存在；[MENSTRUATION] 例外，它是实测事实。
 */
enum class CyclePhase {
    /** 今天落在最后一段经期段内（实测）。 */
    MENSTRUATION,

    /** 经期结束之后、排卵日之前。 */
    FOLLICULAR,

    /** 排卵日 ±1 天。**只用于阶段标签，不作为区间展示**（区间只有易孕窗口一个，见 ADR-0004）。 */
    OVULATION,

    /** 排卵日 +1 天之后、下次经期之前。 */
    LUTEAL,

    /** 数据不足，不推算（不是"没有阶段"）。UI 必须给出文字说明而不是留白。 */
    UNKNOWN,
}

/**
 * 今天在周期中的位置。
 *
 * [cycleDay] 与 [phase] 的**门槛刻意不同**：
 * - [cycleDay] 是事实（今天距最后一段经期起始日多少天），只要有经期记录就有值；
 * - [phase] 是推算，数据不足时为 [CyclePhase.UNKNOWN]。
 */
data class CyclePosition(
    val cycleDay: Int?,
    val phase: CyclePhase,
) {
    companion object {
        /** 没有任何经期记录时的空位。 */
        val unknown = CyclePosition(cycleDay = null, phase = CyclePhase.UNKNOWN)
    }
}
