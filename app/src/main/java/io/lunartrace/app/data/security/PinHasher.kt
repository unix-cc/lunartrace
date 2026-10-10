package io.lunartrace.app.data.security

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** PIN 的存储形态：只有 salt、迭代次数与派生结果，**没有任何可还原 PIN 的信息**。 */
class PinHash(
    val salt: ByteArray,
    val iterations: Int,
    val hash: ByteArray,
)

/**
 * PIN 派生与校验。纯 JVM 实现（不依赖 Android），因此可以在单元测试里直接验证。
 *
 * 安全要求（见 `specs/capabilities/data-privacy.md` §4）：
 * - PBKDF2-HMAC-SHA256，迭代次数 ≥ [DEFAULT_ITERATIONS]
 * - 每次设置 PIN 重新生成随机 salt
 * - 校验使用常量时间比较
 */
object PinHasher {

    const val DEFAULT_ITERATIONS = 120_000

    private const val KEY_LENGTH_BITS = 256
    private const val SALT_BYTES = 16
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"

    /** 用新生成的随机 salt 派生，返回可安全持久化的结果。 */
    fun create(
        pin: String,
        iterations: Int = DEFAULT_ITERATIONS,
        random: SecureRandom = SecureRandom(),
    ): PinHash {
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        return PinHash(salt = salt, iterations = iterations, hash = derive(pin, salt, iterations))
    }

    /** 常量时间比较，避免通过响应时间推断 PIN 的匹配长度。 */
    fun verify(pin: String, stored: PinHash): Boolean =
        MessageDigest.isEqual(derive(pin, stored.salt, stored.iterations), stored.hash)

    private fun derive(pin: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, KEY_LENGTH_BITS)
        return try {
            SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }
}
