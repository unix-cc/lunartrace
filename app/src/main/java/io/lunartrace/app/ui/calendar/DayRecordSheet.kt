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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.lunartrace.app.domain.model.FlowIntensity
import io.lunartrace.app.domain.model.Mood
import io.lunartrace.app.domain.model.Symptom
import io.lunartrace.app.domain.model.SymptomEntry
import io.lunartrace.app.ui.common.fullDateText
import io.lunartrace.app.ui.common.label

/**
 * 单日记录弹层：经期流量、症状、心情、备注。
 * 编辑状态由 ViewModel 持有（草稿），点"保存"才落库。
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
            FlowSection(draft = draft, actions = actions)
            HorizontalDivider()
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
    Text(text = draft.date.fullDateText(), style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(4.dp))
    Text(
        text = if (draft.flow == null) "这一天尚未标记为经期" else "已标记：${draft.flow.label()}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowSection(draft: DayDraft, actions: DayRecordActions) {
    SectionTitle("经期流量")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowIntensity.entries.forEach { flow ->
            FilterChip(
                selected = draft.flow == flow,
                onClick = { actions.onFlowSelected(flow) },
                label = { Text(flow.label()) },
            )
        }
    }
    if (draft.flow != null) {
        TextButton(onClick = actions.onPeriodCleared) { Text("取消经期标记") }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SymptomSection(draft: DayDraft, actions: DayRecordActions) {
    SectionTitle("症状")
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MoodSection(draft: DayDraft, actions: DayRecordActions) {
    SectionTitle("心情")
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

@Composable
private fun NoteSection(draft: DayDraft, actions: DayRecordActions) {
    SectionTitle("备注")
    OutlinedTextField(
        value = draft.note,
        onValueChange = actions.onNoteChanged,
        modifier = Modifier.fillMaxWidth(),
        minLines = 2,
        maxLines = 4,
        supportingText = { Text("${draft.note.length} / ${SymptomEntry.MAX_NOTE_LENGTH}") },
    )
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

@Composable
private fun SectionTitle(text: String) {
    Spacer(Modifier.height(12.dp))
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(bottom = 6.dp),
    )
}
