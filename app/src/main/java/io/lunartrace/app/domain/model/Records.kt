package io.lunartrace.app.domain.model

import java.time.LocalDate

/** 流量强度。取值顺序即由轻到重，UI 直接按此顺序展示。 */
enum class FlowIntensity {
    SPOTTING,
    LIGHT,
    MEDIUM,
    HEAVY,
}

/** 症状标签。集合固定，新增需要同步 database 的解析降级逻辑与统计口径。 */
enum class Symptom {
    CRAMPS,
    HEADACHE,
    BLOATING,
    BREAST_TENDERNESS,
    FATIGUE,
    ACNE,
    NAUSEA,
    BACK_PAIN,
    INSOMNIA,
    APPETITE_CHANGE,
}

/** 心情。允许不记录（`null`），不设"未知"枚举值以避免哨兵值传播。 */
enum class Mood {
    CALM,
    HAPPY,
    IRRITABLE,
    ANXIOUS,
    LOW,
    ENERGETIC,
}

/**
 * 一天的经期记录。**日期即身份**：同一天最多一条。
 * 未来日期允许存在（用户提前标记），但不参与统计与预测。
 */
data class PeriodDay(
    val date: LocalDate,
    val flow: FlowIntensity,
)

/**
 * 一天的附加记录（症状 / 心情 / 备注），与经期记录相互独立。
 * [note] 已被截断到 [MAX_NOTE_LENGTH] 字符，非空字符串。
 */
data class SymptomEntry(
    val date: LocalDate,
    val symptoms: Set<Symptom>,
    val mood: Mood?,
    val note: String,
) {
    /** 该条目是否为空；空的条目在持久化层必须删除行而不是存空值。 */
    val isEmpty: Boolean
        get() = symptoms.isEmpty() && mood == null && note.isBlank()

    companion object {
        const val MAX_NOTE_LENGTH = 500
    }
}
