package io.lunartrace.app.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.lunartrace.app.R
import io.lunartrace.app.domain.model.CycleDefaults
import io.lunartrace.app.domain.model.CycleInsight
import io.lunartrace.app.domain.model.PredictionBasis
import io.lunartrace.app.domain.prediction.CyclePredictor
import io.lunartrace.app.ui.common.excludedSamplesText
import io.lunartrace.app.ui.common.label
import io.lunartrace.app.ui.common.monthDayText
import io.lunartrace.app.ui.common.text
import io.lunartrace.app.ui.common.titleText
import io.lunartrace.app.ui.common.userDefaultBasisText
import io.lunartrace.app.ui.lunartraceViewModelFactory
import io.lunartrace.app.ui.theme.LunartraceHeaderGradient
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onOpenSettings: () -> Unit,
    viewModel: CalendarViewModel = viewModel(factory = lunartraceViewModelFactory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showLastPeriodPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "设置")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White,
                ),
                modifier = Modifier.background(Brush.verticalGradient(LunartraceHeaderGradient)),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            PredictionSection(
                state = state,
                onRecordLastPeriodStart = { showLastPeriodPicker = true },
                onOpenSettings = onOpenSettings,
            )
            Spacer(Modifier.height(8.dp))
            MonthSwitcher(
                month = state.month,
                onPrevious = viewModel::onPreviousMonth,
                onNext = viewModel::onNextMonth,
                onToday = viewModel::onJumpToToday,
            )
            MonthGrid(
                days = state.days,
                today = state.today,
                selectedDate = state.selectedDate,
                periodDates = state.periods.keys,
                predictedDates = state.predictedDates,
                onDateClick = viewModel::onDateSelected,
            )
            CalendarLegend()
            if (state.insight.stats.excludedCycleCount > 0) {
                Spacer(Modifier.height(8.dp))
                ExcludedSamplesNote(count = state.insight.stats.excludedCycleCount)
            }
            Spacer(Modifier.height(16.dp))
            // 记录区就在日历下方（参考设计的做法）：改记录时仍然看得见日历。
            DayRecordPanel(draft = state.record, actions = viewModel.dayRecordActions())
            Spacer(Modifier.height(12.dp))
            Button(onClick = viewModel::onTodaySelected, modifier = Modifier.fillMaxWidth()) {
                Text("把编辑切回今天")
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showLastPeriodPicker) {
        LastPeriodStartDialog(
            today = state.today,
            onDismiss = { showLastPeriodPicker = false },
            onConfirm = { date ->
                showLastPeriodPicker = false
                viewModel.onLastPeriodStartPicked(date)
            },
        )
    }
}

/**
 * 零记录用户的日期选择。不允许确认未来的日期（"上次经期开始日"不可能是将来），
 * 且转换固定按 UTC 解释（见 [DatePickerConversion]）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LastPeriodStartDialog(
    today: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
) {
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = DatePickerConversion.toUtcMillis(today),
    )
    val selectedDate = pickerState.selectedDateMillis?.let(DatePickerConversion::toLocalDate)
    val isFutureSelected = selectedDate != null && selectedDate.isAfter(today)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { selectedDate?.let(onConfirm) },
                enabled = selectedDate != null && !isFutureSelected,
            ) {
                Text("保存")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    ) {
        Column {
            DatePicker(
                state = pickerState,
                title = {
                    Text(
                        text = "上次经期是哪天开始的？",
                        modifier = Modifier.padding(start = 24.dp, top = 16.dp),
                    )
                },
                showModeToggle = false,
            )
            if (isFutureSelected) {
                Text(
                    text = "不能选择未来的日期",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
            }
        }
    }
}

/**
 * 页面顶部：预测卡（含"该提示什么"的推导）。
 * 抽出来是因为 `CalendarScreen` 已经贴着 detekt 的 `LongMethod` 阈值——
 * 再往里加东西就会让人读不下去。
 *
 * 周期环不在这里：它已经在主页（见 `specs/changes/home-tab/spec.md`）。
 */
@Composable
private fun PredictionSection(
    state: CalendarUiState,
    onRecordLastPeriodStart: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    PredictionCard(
        insight = state.insight,
        cycleDefaults = state.cycleDefaults,
        hint = PredictionHints.of(
            hasPeriodRecords = state.periods.isNotEmpty(),
            hasDefaultCycleLength = state.cycleDefaults.cycleLengthDays != null,
            hasPrediction = state.insight.prediction.nextPeriodStart != null,
        ),
        onRecordLastPeriodStart = onRecordLastPeriodStart,
        onOpenSettings = onOpenSettings,
    )
}

@Composable
private fun PredictionCard(
    insight: CycleInsight,
    cycleDefaults: CycleDefaults,
    hint: PredictionHint,
    onRecordLastPeriodStart: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val prediction = insight.prediction
    val nextStart = prediction.nextPeriodStart

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (nextStart == null) {
                Text(text = "还不能给出预计", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                hint.text(requiredEpisodesForPhase = CyclePredictor.MIN_EPISODES_FOR_PHASE)?.let { hintText ->
                    Text(
                        text = hintText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (hint == PredictionHint.RECORD_FIRST_PERIOD ||
                    hint == PredictionHint.RECORD_FIRST_PERIOD_OR_SET_DEFAULTS
                ) {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onRecordLastPeriodStart) { Text("记录上次经期开始") }
                        if (hint == PredictionHint.RECORD_FIRST_PERIOD_OR_SET_DEFAULTS) {
                            TextButton(onClick = onOpenSettings) { Text("去设置填周期长度") }
                        }
                    }
                }
            } else {
                Text(
                    text = "下次经期预计 ${nextStart.monthDayText()}",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = prediction.confidence.label(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (prediction.basis == PredictionBasis.USER_DEFAULT) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = userDefaultBasisText(
                            cycleLengthDays = cycleDefaults.cycleLengthDays,
                            requiredEpisodes = CyclePredictor.MIN_EPISODES_FOR_PHASE,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                // 排卵信息是**单日**：区间只保留"易孕窗口"一个，避免两个天数不同的区间互相打架（ADR-0004）
                prediction.ovulationDay?.let { ovulationDay ->
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "排卵日预计 ${ovulationDay.monthDayText()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                val fertileWindow = prediction.fertileWindow
                if (fertileWindow != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "易孕窗口预计 ${fertileWindow.start.monthDayText()} – " +
                            fertileWindow.endInclusive.monthDayText(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthSwitcher(
    month: YearMonth,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onPrevious) { Text("上一月") }
        Text(text = month.titleText(), style = MaterialTheme.typography.titleMedium)
        Row {
            TextButton(onClick = onToday) { Text("今天") }
            TextButton(onClick = onNext) { Text("下一月") }
        }
    }
}

@Composable
private fun CalendarLegend() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        LegendItem(text = "经期") {
            Box(
                modifier = Modifier
                    .size(width = 16.dp, height = 8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
        LegendItem(text = "预测经期") {
            Box(
                modifier = Modifier
                    .size(width = 16.dp, height = 8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(4.dp)),
            )
        }
        LegendItem(text = "易孕期") {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.tertiaryContainer),
            )
        }
        LegendItem(text = "排卵日") {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiary),
            )
        }
    }
}

@Composable
private fun LegendItem(text: String, marker: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        marker()
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ExcludedSamplesNote(count: Int) {
    Text(
        text = excludedSamplesText(count),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
