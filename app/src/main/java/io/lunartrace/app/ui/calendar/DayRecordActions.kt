package io.lunartrace.app.ui.calendar

import io.lunartrace.app.domain.model.FlowIntensity
import io.lunartrace.app.domain.model.Mood
import io.lunartrace.app.domain.model.Symptom

/**
 * 记录弹层能触发的全部动作。
 * 收成一个参数，避免 Composable 参数表膨胀，也让调用处一眼看全这个界面能做什么。
 */
data class DayRecordActions(
    val onFlowSelected: (FlowIntensity) -> Unit,
    val onPeriodCleared: () -> Unit,
    val onSymptomToggled: (Symptom) -> Unit,
    val onMoodToggled: (Mood) -> Unit,
    val onNoteChanged: (String) -> Unit,
    val onSave: () -> Unit,
    val onClearDay: () -> Unit,
    val onDismiss: () -> Unit,
)
