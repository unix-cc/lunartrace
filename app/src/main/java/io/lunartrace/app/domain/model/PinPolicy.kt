package io.lunartrace.app.domain.model

/**
 * PIN 的输入规则（纯策略，不含加密实现，因此放在领域层让 UI 可直接校验）。
 * 派生与校验见 `data/security/PinHasher.kt`。
 */
object PinPolicy {

    const val MIN_LENGTH = 4
    const val MAX_LENGTH = 6

    /** 4–6 位纯数字。 */
    fun isValid(pin: String): Boolean = pin.length in MIN_LENGTH..MAX_LENGTH && pin.all(Char::isDigit)
}
