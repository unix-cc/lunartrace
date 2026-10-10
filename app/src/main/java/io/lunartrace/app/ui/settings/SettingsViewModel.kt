package io.lunartrace.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.lunartrace.app.data.prefs.SettingsStore
import io.lunartrace.app.domain.model.CycleDefaults
import io.lunartrace.app.domain.repository.CycleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 当前打开的对话框。用枚举而不是多个布尔量，避免出现"两个对话框同时打开"的非法状态。 */
enum class SettingsDialog {
    NONE,
    SET_PIN,
    DISABLE_PIN,
    CONFIRM_WIPE,
    EDIT_CYCLE_LENGTH,
    EDIT_PERIOD_LENGTH,
}

data class SettingsUiState(
    val appLockEnabled: Boolean = false,
    val periodDayCount: Int = 0,
    val symptomEntryCount: Int = 0,
    /** 用户填写的缺省周期长度/经期长度（ADR-0005）；`null` 字段表示未填写。 */
    val cycleDefaults: CycleDefaults = CycleDefaults.none,
    val dialog: SettingsDialog = SettingsDialog.NONE,
    /** 关闭应用锁时 PIN 校验失败。 */
    val pinRejected: Boolean = false,
)

class SettingsViewModel(
    private val settingsStore: SettingsStore,
    private val repository: CycleRepository,
) : ViewModel() {

    private val dialog = MutableStateFlow(SettingsDialog.NONE)
    private val pinRejected = MutableStateFlow(false)

    private val counts = combine(
        repository.observePeriodDays(),
        repository.observeSymptomEntries(),
    ) { periods, entries -> periods.size to entries.count { !it.isEmpty } }

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsStore.appLockEnabled,
        counts,
        dialog,
        pinRejected,
        settingsStore.cycleDefaults,
    ) { appLockEnabled, recordCounts, currentDialog, rejected, defaults ->
        SettingsUiState(
            appLockEnabled = appLockEnabled,
            periodDayCount = recordCounts.first,
            symptomEntryCount = recordCounts.second,
            cycleDefaults = defaults,
            dialog = currentDialog,
            pinRejected = rejected,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = SettingsUiState(),
    )

    fun onSetPinRequested() = openDialog(SettingsDialog.SET_PIN)

    fun onDisablePinRequested() {
        if (uiState.value.appLockEnabled) openDialog(SettingsDialog.DISABLE_PIN) else dialog.value = SettingsDialog.NONE
    }

    fun onWipeRequested() = openDialog(SettingsDialog.CONFIRM_WIPE)

    fun onCycleLengthEditRequested() = openDialog(SettingsDialog.EDIT_CYCLE_LENGTH)

    fun onPeriodLengthEditRequested() = openDialog(SettingsDialog.EDIT_PERIOD_LENGTH)

    /** `days == null` 表示清除该项：不填就不预测，而不是回退到某个应用自选的数字（ADR-0005）。 */
    fun setDefaultCycleLength(days: Int?) {
        viewModelScope.launch {
            settingsStore.setCycleDefaults(uiState.value.cycleDefaults.copy(cycleLengthDays = days))
            onDialogDismissed()
        }
    }

    fun setDefaultPeriodLength(days: Int?) {
        viewModelScope.launch {
            settingsStore.setCycleDefaults(uiState.value.cycleDefaults.copy(periodLengthDays = days))
            onDialogDismissed()
        }
    }

    fun onDialogDismissed() {
        dialog.value = SettingsDialog.NONE
        pinRejected.value = false
    }

    /** 设置 PIN 并开启应用锁。调用方负责校验 PIN 形态（[io.lunartrace.app.domain.model.PinPolicy]）。 */
    fun enableAppLock(pin: String) {
        viewModelScope.launch {
            settingsStore.enableAppLock(pin)
            onDialogDismissed()
        }
    }

    fun disableAppLock(pin: String) {
        viewModelScope.launch {
            if (settingsStore.verifyPin(pin)) {
                settingsStore.disableAppLock()
                onDialogDismissed()
            } else {
                pinRejected.value = true
            }
        }
    }

    /** 清除全部数据：记录 + 设置（含缺省值，真删除，不可恢复）。 */
    fun wipeAllData() {
        viewModelScope.launch {
            repository.wipeAll()
            settingsStore.wipe()
            onDialogDismissed()
        }
    }

    private fun openDialog(target: SettingsDialog) {
        pinRejected.value = false
        dialog.update { target }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
