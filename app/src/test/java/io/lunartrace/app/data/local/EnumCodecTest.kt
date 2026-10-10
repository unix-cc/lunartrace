package io.lunartrace.app.data.local

import io.lunartrace.app.domain.model.FlowIntensity
import io.lunartrace.app.domain.model.Mood
import io.lunartrace.app.domain.model.Symptom
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 枚举编解码的降级行为。历史数据里出现已删除的枚举名时，应用**不能崩**。
 */
class EnumCodecTest {

    @Test
    fun `症状集合编码与迭代顺序无关`() {
        val first = EnumCodec.encodeSymptoms(setOf(Symptom.ACNE, Symptom.CRAMPS))
        val second = EnumCodec.encodeSymptoms(setOf(Symptom.CRAMPS, Symptom.ACNE))

        assertEquals(first, second)
        assertEquals("ACNE,CRAMPS", first)
    }

    @Test
    fun `未知症状名被忽略而不是抛异常`() {
        val decoded = EnumCodec.decodeSymptoms("CRAMPS,NO_SUCH_SYMPTOM,ACNE")

        assertEquals(setOf(Symptom.CRAMPS, Symptom.ACNE), decoded)
    }

    @Test
    fun `空字符串解码为空集合`() {
        assertEquals(emptySet<Symptom>(), EnumCodec.decodeSymptoms(""))
    }

    @Test
    fun `未知心情名降级为未记录`() {
        assertNull(EnumCodec.decodeMood("NO_SUCH_MOOD"))
        assertNull(EnumCodec.decodeMood(null))
        assertEquals(Mood.CALM, EnumCodec.decodeMood(Mood.CALM.name))
    }

    @Test
    fun `未知流量名返回 null 由调用方忽略该行`() {
        assertNull(EnumCodec.decodeFlow("NO_SUCH_FLOW"))
        assertEquals(FlowIntensity.HEAVY, EnumCodec.decodeFlow(FlowIntensity.HEAVY.name))
    }
}
