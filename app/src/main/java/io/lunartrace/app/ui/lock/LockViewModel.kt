package io.lunartrace.app.ui.lock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.lunartrace.app.data.prefs.SettingsStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 应用锁状态。
 *
 * - `lockEnabled == null` 表示设置尚未读出，UI 应显示空白而不是内容（避免解锁前闪出记录）。
 * - 失败 5 次后进入 30 秒冷却；冷却计数不用系统时钟，改为逐秒递减，避免依赖时间源。
 * - 进程重建后 `locked` 回到初始值（重新要求解锁），因此没有"记住解锁状态"的持久化。
 */
data class LockUiState(
    val lockEnabled: Boolean? = null,
    val locked: Boolean = false,
    val pinInput: String = "",
    val pinRejected: Boolean = false,
    val lockoutSecondsRemaining: Int = 0,
)

class LockViewModel(private val settingsStore: SettingsStore) : ViewModel() {

    private val _uiState = MutableStateFlow(LockUiState())
    val uiState: StateFlow<LockUiState> = _uiState.asStateFlow()

    private var failedAttempts = 0
    private var countdownJob: Job? = null

    init {
        viewModelScope.launch {
            val enabled = settingsStore.appLockEnabled.first()
            _uiState.update { it.copy(lockEnabled = enabled, locked = enabled) }
        }
    }

    fun onPinChanged(pin: String) {
        if (pin.length > MAX_PIN_LENGTH) return
        _uiState.update { it.copy(pinInput = pin.filter(Char::isDigit), pinRejected = false) }
    }

    fun onUnlockRequested() {
        val state = _uiState.value
        if (state.lockoutSecondsRemaining > 0) return
        val pin = state.pinInput
        if (pin.isBlank()) return

        viewModelScope.launch {
            if (settingsStore.verifyPin(pin)) {
                failedAttempts = 0
                _uiState.update { it.copy(locked = false, pinInput = "", pinRejected = false) }
            } else {
                registerFailure()
                _uiState.update { it.copy(pinInput = "", pinRejected = true) }
            }
        }
    }

    private fun registerFailure() {
        failedAttempts++
        if (failedAttempts < MAX_FAILED_ATTEMPTS) return

        failedAttempts = 0
        countdownJob?.cancel()
        _uiState.update { it.copy(lockoutSecondsRemaining = LOCKOUT_SECONDS) }
        countdownJob = viewModelScope.launch {
            while (_uiState.value.lockoutSecondsRemaining > 0) {
                delay(ONE_SECOND_MILLIS)
                _uiState.update { it.copy(lockoutSecondsRemaining = it.lockoutSecondsRemaining - 1) }
            }
        }
    }

    private companion object {
        const val MAX_PIN_LENGTH = 6
        const val MAX_FAILED_ATTEMPTS = 5
        const val LOCKOUT_SECONDS = 30
        const val ONE_SECOND_MILLIS = 1_000L
    }
}
