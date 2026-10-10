package io.lunartrace.app.data.security

import io.lunartrace.app.domain.model.PinPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * PIN 派生与校验的安全行为测试。
 * 迭代次数设为 1_000 以便测试快速运行——生产常量为 [PinHasher.DEFAULT_ITERATIONS]。
 */
class PinHasherTest {

    private val fastIterations = 1_000

    @Test
    fun `正确 PIN 通过校验`() {
        val stored = PinHasher.create("1234", iterations = fastIterations)

        assertTrue(PinHasher.verify("1234", stored))
    }

    @Test
    fun `错误 PIN 不通过校验`() {
        val stored = PinHasher.create("1234", iterations = fastIterations)

        assertFalse(PinHasher.verify("1235", stored))
        assertFalse(PinHasher.verify("", stored))
        assertFalse(PinHasher.verify("12345", stored))
    }

    @Test
    fun `相同 PIN 在不同 salt 下得到不同哈希`() {
        val first = PinHasher.create("1234", iterations = fastIterations)
        val second = PinHasher.create("1234", iterations = fastIterations)

        assertNotEquals(first.salt.toList(), second.salt.toList())
        assertNotEquals(first.hash.toList(), second.hash.toList())
    }

    @Test
    fun `相同 salt 与迭代次数下派生结果可复现`() {
        val stored = PinHasher.create("1234", iterations = fastIterations)
        val reconstructed = PinHash(salt = stored.salt, iterations = stored.iterations, hash = stored.hash)

        // 同样的输入重复校验必须得到同样的结论（派生是确定性的）
        assertTrue(PinHasher.verify("1234", reconstructed))
        assertTrue(PinHasher.verify("1234", stored))
        assertFalse(PinHasher.verify("9999", reconstructed))
    }

    @Test
    fun `生产默认迭代次数不低于安全下限`() {
        assertTrue(PinHasher.DEFAULT_ITERATIONS >= 120_000)
    }

    @Test
    fun `PIN 形态校验只接受四位到六位数字`() {
        assertTrue(PinPolicy.isValid("1234"))
        assertTrue(PinPolicy.isValid("123456"))
        assertFalse(PinPolicy.isValid("123"))
        assertFalse(PinPolicy.isValid("1234567"))
        assertFalse(PinPolicy.isValid("12a4"))
        assertFalse(PinPolicy.isValid(""))
    }
}
