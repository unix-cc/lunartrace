package io.lunartrace.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.lunartrace.app.data.security.PinHash
import io.lunartrace.app.data.security.PinHasher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Base64

/** 应用级 DataStore。顶层委托是 DataStore 的推荐用法：同一进程只创建一个实例。 */
val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "lunartrace_settings")

/**
 * 锁与引导状态的持久化。
 *
 * 红线：这里**只**存派生结果与开关，不存 PIN 明文（见 `specs/capabilities/data-privacy.md` §4）。
 */
class SettingsStore(private val dataStore: DataStore<Preferences>) {

    val appLockEnabled: Flow<Boolean> = dataStore.data.map { it[KEY_APP_LOCK_ENABLED] ?: false }

    suspend fun enableAppLock(pin: String) {
        val pinHash = PinHasher.create(pin)
        dataStore.edit { preferences ->
            preferences[KEY_APP_LOCK_ENABLED] = true
            preferences[KEY_PIN_SALT] = encode(pinHash.salt)
            preferences[KEY_PIN_HASH] = encode(pinHash.hash)
            preferences[KEY_PIN_ITERATIONS] = pinHash.iterations
        }
    }

    /** 关闭应用锁并清除全部 PIN 派生数据。调用方必须先校验 PIN。 */
    suspend fun disableAppLock() {
        dataStore.edit { preferences ->
            preferences.remove(KEY_APP_LOCK_ENABLED)
            preferences.remove(KEY_PIN_SALT)
            preferences.remove(KEY_PIN_HASH)
            preferences.remove(KEY_PIN_ITERATIONS)
        }
    }

    /** 校验 PIN。未设置过 PIN 或数据不完整时一律返回 `false`。 */
    suspend fun verifyPin(pin: String): Boolean {
        val stored = readPinHash() ?: return false
        return PinHasher.verify(pin, stored)
    }

    /** 清除全部设置（"清除数据"的一部分，真删除）。幂等。 */
    suspend fun wipe() {
        dataStore.edit { it.clear() }
    }

    private suspend fun readPinHash(): PinHash? {
        val preferences = dataStore.data.first()
        val salt = preferences[KEY_PIN_SALT]?.let(::decode) ?: return null
        val hash = preferences[KEY_PIN_HASH]?.let(::decode) ?: return null
        val iterations = preferences[KEY_PIN_ITERATIONS] ?: PinHasher.DEFAULT_ITERATIONS
        return PinHash(salt = salt, iterations = iterations, hash = hash)
    }

    private fun encode(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)

    private fun decode(text: String): ByteArray = Base64.getDecoder().decode(text)

    private companion object {
        val KEY_APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val KEY_PIN_SALT = stringPreferencesKey("pin_salt")
        val KEY_PIN_HASH = stringPreferencesKey("pin_hash")
        val KEY_PIN_ITERATIONS = intPreferencesKey("pin_iterations")
    }
}
