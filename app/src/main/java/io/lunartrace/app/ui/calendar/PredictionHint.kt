package io.lunartrace.app.ui.calendar

/**
 * 预测卡在没有预测时该说什么。**纯函数决定，不在 Composable 里拼字符串**：
 * 上一轮的"再记录 3 次"文案就是因为把两档门槛（记录 1 次出预计 / 3 次出阶段）混在一起，
 * 而对用户说了假话（见 spec 的 Problem Statement）。
 */
enum class PredictionHint {
    /** 有预测，卡片不需要额外提示。 */
    NONE,

    /** 零经期记录，但已填缺省周期长度：只差一次记录。 */
    RECORD_FIRST_PERIOD,

    /** 零经期记录且未填缺省值：记录一次、或先去设置里填周期长度，两条路都通。 */
    RECORD_FIRST_PERIOD_OR_SET_DEFAULTS,

    /** 有记录但推不出预计（例如长期未打开导致顺推超限、或有效样本全被排除）。 */
    CHECK_RECORDS,
}

object PredictionHints {

    /**
     * @param hasPeriodRecords 是否已有任何经期记录（决定"缺锚点"这一档）
     * @param hasDefaultCycleLength 用户是否填了缺省周期长度（决定只差 1 次还是还差 3 次）
     * @param hasPrediction 当前是否已经推得出下次经期
     */
    fun of(
        hasPeriodRecords: Boolean,
        hasDefaultCycleLength: Boolean,
        hasPrediction: Boolean,
    ): PredictionHint = when {
        hasPrediction -> PredictionHint.NONE
        !hasPeriodRecords && hasDefaultCycleLength -> PredictionHint.RECORD_FIRST_PERIOD
        !hasPeriodRecords -> PredictionHint.RECORD_FIRST_PERIOD_OR_SET_DEFAULTS
        else -> PredictionHint.CHECK_RECORDS
    }
}
