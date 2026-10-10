package io.lunartrace.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.lunartrace.app.core.time.Today
import io.lunartrace.app.data.prefs.SettingsStore
import io.lunartrace.app.domain.model.CycleDefaults
import io.lunartrace.app.domain.model.PeriodDay
import io.lunartrace.app.domain.prediction.CyclePredictor
import io.lunartrace.app.domain.repository.CycleRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * 主页状态机：与日历页读同一份数据、同一个 [CyclePredictor]，
 * 因此两页的位置与预测**不可能不一致**（差别只在展示的详略）。
 */
class HomeViewModel(
    repository: CycleRepository,
    private val today: Today,
    settingsStore: SettingsStore,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        repository.observePeriodDays(),
        settingsStore.cycleDefaults,
    ) { periods, defaults -> buildState(periods, defaults) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = buildState(periods = emptyList(), defaults = CycleDefaults.none),
        )

    private fun buildState(periods: List<PeriodDay>, defaults: CycleDefaults): HomeUiState {
        val todayDate = today.date()
        val periodDates = periods.map { it.date }.toSet()
        val insight = CyclePredictor.analyze(periodDates, todayDate, defaults)

        return HomeUiState(
            today = todayDate,
            phase = insight.position.phase,
            ring = CycleRingData.of(
                prediction = insight.prediction,
                position = insight.position,
                periodDates = periodDates,
                today = todayDate,
            ),
            nextPeriodStart = insight.prediction.nextPeriodStart,
            confidence = insight.prediction.confidence,
        )
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
