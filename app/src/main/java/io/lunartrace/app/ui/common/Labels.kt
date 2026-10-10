package io.lunartrace.app.ui.common

import io.lunartrace.app.domain.model.Confidence
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

fun LocalDate.monthDayText(): String = "${monthValue}月${dayOfMonth}日"

/** 被排除样本的中性说明。日历与统计共用同一份文案，避免两处各写一遍。 */
fun excludedSamplesText(count: Int): String =
    "有 $count 个周期样本因超出 15–60 天范围未参与预计（这不是医学判断）。"

fun LocalDate.fullDateText(): String = "${year}年${monthValue}月${dayOfMonth}日"

fun YearMonth.titleText(): String = "${year}年${monthValue}月"
