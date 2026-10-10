package io.lunartrace.app.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.lunartrace.app.domain.model.Confidence
import io.lunartrace.app.ui.common.label
import io.lunartrace.app.ui.common.monthDayText
import io.lunartrace.app.ui.lunartraceViewModelFactory
import java.time.LocalDate

/**
 * 主页：一眼看到"我现在在哪"，然后一步去记录。
 *
 * 这里**只读**：记录仍然只在日历页发生（记录面板与日历强耦合，见 `specs/changes/home-tab/spec.md`）。
 */
@Composable
fun HomeScreen(
    onGoToCalendar: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = lunartraceViewModelFactory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))
        CycleRingSection(data = state.ring, today = state.today, phase = state.phase)
        Spacer(Modifier.height(24.dp))
        NextPeriodLine(nextPeriodStart = state.nextPeriodStart, confidence = state.confidence)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onGoToCalendar, modifier = Modifier.fillMaxWidth()) {
            Text("去做记录")
        }
        Spacer(Modifier.height(24.dp))
    }
}

/**
 * "下次经期预计 X"。
 * 推不出来时**整行都不显示**（返回早退）：环的圆心已经说了"还没有记录"，
 * 再换句话重复一遍只会让人以为出了两回事。
 */
@Composable
private fun NextPeriodLine(nextPeriodStart: LocalDate?, confidence: Confidence) {
    if (nextPeriodStart == null) return

    Text(
        text = "下次经期预计 ${nextPeriodStart.monthDayText()}",
        style = MaterialTheme.typography.titleMedium,
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = confidence.label(),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
