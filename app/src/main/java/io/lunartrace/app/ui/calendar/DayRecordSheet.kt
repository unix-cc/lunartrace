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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
 * 单日记录弹层。
 *
 * 结构是**统一的"标签行 + 内容"**（经期流量 / 症状 / 心情 / 备注），而不是一片平铺的 chip：
 * 上一版把 4 个流量 chip、10 个症状 chip、6 个心情 chip 平等铺开，用户在 20 个 chip 里找东西，
 * 层级完全依赖底色。流量改为"填充到第 N 个"的水滴，点某个水滴即选中该档。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayRecordSheet(draft: DayDraft, actions: DayRecordActions) {
    ModalBottomSheet(
        onDismissRequest = actions.onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
        ) {
            SheetHeader(draft)
            RecordRow(label = "经期流量") {
                FlowDropletPicker(draft = draft, actions = actions)
            }
            HorizontalDivider(modifier = Modifier.padding(top = 16.dp))
            SymptomSection(draft = draft, actions = actions)
            MoodSection(draft = draft, actions = actions)
            NoteSection(draft = draft, actions = actions)
            Spacer(Modifier.height(16.dp))
            SheetButtons(actions = actions)
            Spacer(Modifier.height(8.dp))
            Text(
                text = "记录只保存在本机，不会上传。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SheetHeader(draft: DayDraft) {
    Text(text = draft.date.fullDateWithWeekdayText(), style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(4.dp))
    Text(
        text = if (draft.flow == null) "这一天尚未标记为经期" else "已标记：${draft.flow.label()}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
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
 * 每个水滴的 `contentDescription` 是对应档位文字，读屏用户不会只听到"图标"。
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

@Composable
private fun SheetButtons(actions: DayRecordActions) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = actions.onClearDay, modifier = Modifier.weight(1f)) {
            Text("清空这一天")
        }
        Button(onClick = actions.onSave, modifier = Modifier.weight(1f)) {
            Text("保存")
        }
    }
}
