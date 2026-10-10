package io.lunartrace.app.data.local

import io.lunartrace.app.domain.model.FlowIntensity
import io.lunartrace.app.domain.model.Mood
import io.lunartrace.app.domain.model.Symptom

/**
 * 枚举 ↔ 存储字符串的**唯一**转换点。
 *
 * 解析未知值一律**降级为忽略**，绝不抛异常：历史数据里可能出现已删除的枚举名，
 * `valueOf` 会让应用在启动时崩溃（见 `specs/capabilities/local-data-model.md` §2）。
 */
internal object EnumCodec {

    /** 症状集合编码：先排序，保证同样的集合得到同样的字符串。 */
    fun encodeSymptoms(symptoms: Set<Symptom>): String =
        symptoms.map { it.name }.sorted().joinToString(separator = ",")

    /** 未知症状名被丢弃，不抛异常。 */
    fun decodeSymptoms(raw: String): Set<Symptom> =
        raw.split(',')
            .mapNotNull { name -> Symptom.entries.firstOrNull { it.name == name.trim() } }
            .toSet()

    fun encodeMood(mood: Mood?): String? = mood?.name

    /** 未知心情名降级为"未记录"。 */
    fun decodeMood(raw: String?): Mood? = raw?.let { value -> Mood.entries.firstOrNull { it.name == value } }

    /** 未知流量名返回 `null`，由调用方决定忽略整行。 */
    fun decodeFlow(raw: String): FlowIntensity? = FlowIntensity.entries.firstOrNull { it.name == raw }
}
