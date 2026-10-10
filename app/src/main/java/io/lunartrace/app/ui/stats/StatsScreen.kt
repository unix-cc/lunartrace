package io.lunartrace.app.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.lunartrace.app.domain.model.CycleStats
import io.lunartrace.app.ui.common.excludedSamplesText
import io.lunartrace.app.ui.common.label
import io.lunartrace.app.ui.lunartraceViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(viewModel: StatsViewModel = viewModel(factory = lunartraceViewModelFactory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(topBar = { TopAppBar(title = { Text("统计") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            MetricsSection(stats = state.insight.stats)
            Spacer(Modifier.height(16.dp))
            TrendCard(stats = state.insight.stats)
            Spacer(Modifier.height(16.dp))
            SymptomFrequencyCard(frequencies = state.symptomFrequencies)
            Spacer(Modifier.height(16.dp))
            RecordCountCard(state = state)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun MetricsSection(stats: CycleStats) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        MetricCard(title = "平均周期长度", value = stats.averageCycleLength.dayText(), modifier = Modifier.weight(1f))
        MetricCard(title = "中位周期长度", value = stats.medianCycleLength.dayText(), modifier = Modifier.weight(1f))
    }
    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        MetricCard(title = "平均经期长度", value = stats.averagePeriodLength.dayText(), modifier = Modifier.weight(1f))
        MetricCard(title = "参与预计的样本", value = "${stats.validSampleCount} 个", modifier = Modifier.weight(1f))
    }
}

@Composable
private fun MetricCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun TrendCard(stats: CycleStats) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "周期长度趋势", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            CycleLengthChart(values = stats.recentCycleLengths)
        }
    }
}

@Composable
private fun SymptomFrequencyCard(frequencies: List<SymptomFrequency>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "症状频次 Top ${frequencies.size}", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            if (frequencies.isEmpty()) {
                Text(
                    text = "还没有症状记录。在日历里点任意日期即可添加。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Column
            }
            val maxCount = frequencies.first().count.coerceAtLeast(1)
            frequencies.forEach { frequency ->
                SymptomFrequencyRow(frequency = frequency, maxCount = maxCount)
            }
        }
    }
}

@Composable
private fun RecordCountCard(state: StatsUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "记录量", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                text = "经期天数 ${state.periodDayCount} 天 · 症状记录 ${state.symptomEntryCount} 天 · 备注 ${state.noteCount} 条",
                style = MaterialTheme.typography.bodyMedium,
            )
            if (state.insight.stats.excludedCycleCount > 0) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = excludedSamplesText(state.insight.stats.excludedCycleCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SymptomFrequencyRow(frequency: SymptomFrequency, maxCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = frequency.symptom.label(),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(88.dp),
        )
        LinearProgressIndicator(
            progress = { frequency.count.toFloat() / maxCount },
            modifier = Modifier
                .weight(1f)
                .height(8.dp),
        )
        Text(
            text = frequency.count.toString(),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

private fun Int?.dayText(): String = this?.let { "$it 天" } ?: "—"
