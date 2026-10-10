package io.lunartrace.app.ui.calendar

import io.lunartrace.app.domain.model.FlowIntensity
import io.lunartrace.app.domain.model.Mood
import io.lunartrace.app.domain.model.Symptom
import io.lunartrace.app.domain.model.SymptomEntry
import java.time.LocalDate

/**
 * 记录面板的纯编辑逻辑。
 *
 * 为什么需要它：记录改为"点选即时保存"后，**UI 显示值不能直接读已持久化的数据**——
 * 持久化是异步的，连点两次症状时第二次会读到尚未更新的旧集合，于是第一次的点选被覆盖。
 * 所以显示值来自一个本地覆盖层（`DayDraft`），由本文件的函数维护；写库是它的下游。
 */
object DayRecordEditing {

    /** 由已持久化的数据构造面板显示值。 */
    fun fromStored(date: LocalDate, flow: FlowIntensity?, entry: SymptomEntry?): DayDraft = DayDraft(
        date = date,
        flow = flow,
        symptoms = entry?.symptoms.orEmpty(),
        mood = entry?.mood,
        note = entry?.note.orEmpty(),
    )

    /** 设置流量；传 `null` 表示取消经期标记（**不得**影响症状、心情与备注）。 */
    fun setFlow(draft: DayDraft, flow: FlowIntensity?): DayDraft = draft.copy(flow = flow)

    fun toggleSymptom(draft: DayDraft, symptom: Symptom): DayDraft = draft.copy(
        symptoms = if (symptom in draft.symptoms) draft.symptoms - symptom else draft.symptoms + symptom,
    )

    /** 心情是单选且可取消：再点同一个即清除。 */
    fun toggleMood(draft: DayDraft, mood: Mood): DayDraft = draft.copy(
        mood = if (draft.mood == mood) null else mood,
    )

    /** 备注长度上限与领域保持一致（在输入处就截断，避免把超长文本写进库）。 */
    fun withNote(draft: DayDraft, note: String): DayDraft = draft.copy(
        note = note.take(SymptomEntry.MAX_NOTE_LENGTH),
    )
}
