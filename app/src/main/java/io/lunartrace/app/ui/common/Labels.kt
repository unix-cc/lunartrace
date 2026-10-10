package io.lunartrace.app.ui.common

import io.lunartrace.app.domain.model.Confidence
import io.lunartrace.app.domain.model.CyclePhase
import io.lunartrace.app.domain.model.FlowIntensity
import io.lunartrace.app.domain.model.Mood
import io.lunartrace.app.domain.model.Symptom
import java.time.LocalDate
import java.time.YearMonth

/**
 * 全部面向用户的中文文案的唯一来源（P0 不做多语言，因此不散落到各组件里；
 * 将来做 i18n 时只需把本文件换成字符串资源，见 `specs/changes/mvp-period-tracking/spec.md`）。
 * 文案口径来自 `PRODUCT.md` §5：预测必须带"预计"，禁止"准确率/概率/诊断"。
 */

fun FlowIntensity.label(): String = when (this) {
    FlowIntensity.SPOTTING -> "点滴"
    FlowIntensity.LIGHT -> "少"
    FlowIntensity.MEDIUM -> "中"
    FlowIntensity.HEAVY -> "多"
}

fun Symptom.label(): String = when (this) {
    Symptom.CRAMPS -> "腹痛"
    Symptom.HEADACHE -> "头痛"
    Symptom.BLOATING -> "腹胀"
    Symptom.BREAST_TENDERNESS -> "乳房胀痛"
    Symptom.FATIGUE -> "疲惫"
    Symptom.ACNE -> "冒痘"
    Symptom.NAUSEA -> "恶心"
    Symptom.BACK_PAIN -> "腰酸"
    Symptom.INSOMNIA -> "失眠"
    Symptom.APPETITE_CHANGE -> "食欲变化"
}

fun Mood.label(): String = when (this) {
    Mood.CALM -> "平静"
    Mood.HAPPY -> "开心"
    Mood.IRRITABLE -> "烦躁"
    Mood.ANXIOUS -> "焦虑"
    Mood.LOW -> "低落"
    Mood.ENERGETIC -> "有精神"
}

/** 置信度文案：描述数据充分程度，不含任何医学暗示。 */
fun Confidence.label(): String = when (this) {
    Confidence.NONE -> "暂无足够记录"
    Confidence.LOW -> "数据不足，仅供参考"
    Confidence.MEDIUM -> "数据较充足"
    Confidence.HIGH -> "数据充足"
}

/**
 * 阶段文案。`UNKNOWN` **没有文案**：数据不足时必须由 UI 显式说明，不允许留白或编一个阶段名。
 */
fun CyclePhase.labelOrNull(): String? = when (this) {
    CyclePhase.MENSTRUATION -> "经期"
    CyclePhase.FOLLICULAR -> "卵泡期"
    CyclePhase.OVULATION -> "排卵期"
    CyclePhase.LUTEAL -> "黄体期"
    CyclePhase.UNKNOWN -> null
}

/**
 * 兜底预测的来源说明（ADR-0005 Decision 6）：
 * 必须让用户看懂"这是你填的，不是算出来的"，否则等于把用户自述伪装成测量结果。
 */
fun userDefaultBasisText(cycleLengthDays: Int?, requiredEpisodes: Int): String {
    val lengthText = cycleLengthDays?.let { " $it 天" }.orEmpty()
    return "基于你填写的缺省周期长度$lengthText；记录满 $requiredEpisodes 次经期开始后会自动改用你的实际数据。"
}

/** 设置项里显示天数；未填写时明确显示"未填写"，不显示一个看起来像默认值的数字。 */
fun optionalDaysText(days: Int?): String = days?.let { "$it 天" } ?: "未填写"

/** 数据不足时的进度说明：把"还差几次"讲清楚，避免用户以为功能坏了（见 ADR-0004 代价 1）。 */
fun notEnoughRecordsText(episodeCount: Int, requiredEpisodes: Int): String {
    val remaining = (requiredEpisodes - episodeCount).coerceAtLeast(1)
    return "再记录 $remaining 次经期开始，就会显示下次经期预计与周期阶段。"
}

fun LocalDate.monthDayText(): String = "${monthValue}月${dayOfMonth}日"

/** 被排除样本的中性说明。日历与统计共用同一份文案，避免两处各写一遍。 */
fun excludedSamplesText(count: Int): String =
    "有 $count 个周期样本因超出 15–60 天范围未参与预计（这不是医学判断）。"

fun LocalDate.fullDateText(): String = "${year}年${monthValue}月${dayOfMonth}日"

fun YearMonth.titleText(): String = "${year}年${monthValue}月"
