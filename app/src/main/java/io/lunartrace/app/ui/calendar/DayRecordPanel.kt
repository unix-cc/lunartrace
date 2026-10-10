package io.lunartrace.app.ui.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.lunartrace.app.domain.model.FlowIntensity
import io.lunartrace.app.domain.model.Mood
import io.lunartrace.app.domain.model.Symptom
import io.lunartrace.app.domain.model.SymptomEntry
import io.lunartrace.app.ui.common.fullDateWithWeekdayText
import io.lunartrace.app.ui.common.label

private val DROPLET_SIZE = 36.dp
private const val UNSELECTED_DROPLET_ALPHA = 0.28f

/**
 * 内联的日记录面板（在日历下方，取代了原来的模态弹层）。
 *
 * **点选即时保存**：面板里没有"保存"按钮，每次操作立刻写库。因此：
 * - 显示值必须来自 ViewModel 的覆盖层（`CalendarUiState.record`），不能直接读数据库；
 * - 界面要明确告诉用户"改动会立即保存"，否则用户会去找保存按钮。
 */
@Composable
fun DayRecordPanel(
    draft: DayDraft,
    actions: DayRecordActions,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = draft.date.fullDateWithWeekdayText(), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (draft.flow == null) "这一天尚未标记为经期" else "已标记：${draft.flow.label()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            RecordRow(label = "经期流量") {
                FlowDropletPicker(draft = draft, actions = actions)
            }
            HorizontalDivider(modifier = Modifier.padding(top = 16.dp))
            SymptomSection(draft = draft, actions = actions)
            MoodSection(draft = draft, actions = actions)
            NoteSection(draft = draft, actions = actions)

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "改动会立即保存",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = actions.onClearDay) { Text("清空这一天") }
            }
        }
    }
}

/** 统一的区块结构：标签在上、内容在下。 */
@Composable
private fun RecordRow(label: String, content: @Composable () -> Unit) {
    Spacer(Modifier.height(16.dp))
    Text(text = label, style = MaterialTheme.typography.titleSmall)
    Spacer(Modifier.height(6.dp))
    content()
}

/**
 * 水滴流量选择器：填充到第 N 个 = 第 N 档强度。
 * 依赖 `FlowIntensity` 的声明顺序由轻到重，该顺序由 `FlowIntensityOrderTest` 钉住。
 */
@Composable
private fun FlowDropletPicker(draft: DayDraft, actions: DayRecordActions) {
    val colors = MaterialTheme.colorScheme
    val selectedOrdinal = draft.flow?.ordinal

    Row(verticalAlignment = Alignment.CenterVertically) {
        FlowIntensity.entries.forEachIndexed { index, flow ->
            val filled = selectedOrdinal != null && index <= selectedOrdinal
            IconButton(
                onClick = { actions.onFlowSelected(flow) },
                modifier = Modifier.size(DROPLET_SIZE),
            ) {
                Icon(
                    imageVector = Icons.Filled.WaterDrop,
                    contentDescription = flow.label(),
                    tint = if (filled) colors.primary else colors.onSurfaceVariant.copy(alpha = UNSELECTED_DROPLET_ALPHA),
                )
            }
        }
        Spacer(Modifier.width(4.dp))
        Text(
            text = draft.flow?.label() ?: "未标记",
            style = MaterialTheme.typography.bodyMedium,
            color = if (draft.flow == null) colors.onSurfaceVariant else colors.onSurface,
        )
        if (draft.flow != null) {
            TextButton(onClick = actions.onPeriodCleared) { Text("取消") }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SymptomSection(draft: DayDraft, actions: DayRecordActions) {
    RecordRow(label = "症状") {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Symptom.entries.forEach { symptom ->
                FilterChip(
                    selected = symptom in draft.symptoms,
                    onClick = { actions.onSymptomToggled(symptom) },
                    label = { Text(symptom.label()) },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MoodSection(draft: DayDraft, actions: DayRecordActions) {
    RecordRow(label = "心情") {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Mood.entries.forEach { mood ->
                FilterChip(
                    selected = draft.mood == mood,
                    onClick = { actions.onMoodToggled(mood) },
                    label = { Text(mood.label()) },
                )
            }
        }
    }
}

@Composable
private fun NoteSection(draft: DayDraft, actions: DayRecordActions) {
    RecordRow(label = "备注") {
        OutlinedTextField(
            value = draft.note,
            onValueChange = actions.onNoteChanged,
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4,
            supportingText = { Text("${draft.note.length} / ${SymptomEntry.MAX_NOTE_LENGTH}") },
        )
    }
}
