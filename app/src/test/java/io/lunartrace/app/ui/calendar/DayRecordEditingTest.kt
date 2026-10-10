package io.lunartrace.app.ui.calendar

import io.lunartrace.app.domain.model.FlowIntensity
import io.lunartrace.app.domain.model.Mood
import io.lunartrace.app.domain.model.Symptom
import io.lunartrace.app.domain.model.SymptomEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * 记录面板的纯编辑逻辑。红先绿后：本文件先于实现写出。
 *
 * 最关键的一条是"取消经期标记不得顺手动掉症状/心情/备注"——这类"顺手清掉用户已记内容"的 bug
 * 在弹层时代不容易发生（有显式保存），改成即时保存后就成了真实风险。
 */
class DayRecordEditingTest {

    private val date: LocalDate = LocalDate.parse("2026-10-10")

    private fun emptyDraft() = DayDraft(
        date = date,
        flow = null,
        symptoms = emptySet(),
        mood = null,
        note = "",
    )

    @Test
    fun `无条目时由存储构造出空面板`() {
        val draft = DayRecordEditing.fromStored(date = date, flow = FlowIntensity.LIGHT, entry = null)

        assertEquals(FlowIntensity.LIGHT, draft.flow)
        assertEquals(emptySet<Symptom>(), draft.symptoms)
        assertNull(draft.mood)
        assertEquals("", draft.note)
    }

    @Test
    fun `有条目时四个字段都来自条目`() {
        val entry = SymptomEntry(
            date = date,
            symptoms = setOf(Symptom.CRAMPS, Symptom.FATIGUE),
            mood = Mood.LOW,
            note = "今天很累",
        )

        val draft = DayRecordEditing.fromStored(date = date, flow = FlowIntensity.HEAVY, entry = entry)

        assertEquals(FlowIntensity.HEAVY, draft.flow)
        assertEquals(setOf(Symptom.CRAMPS, Symptom.FATIGUE), draft.symptoms)
        assertEquals(Mood.LOW, draft.mood)
        assertEquals("今天很累", draft.note)
    }

    @Test
    fun `勾选与取消症状都不动心情与备注`() {
        val base = emptyDraft().copy(mood = Mood.CALM, note = "备注")

        val added = DayRecordEditing.toggleSymptom(base, Symptom.CRAMPS)
        assertEquals(setOf(Symptom.CRAMPS), added.symptoms)
        assertEquals(Mood.CALM, added.mood)
        assertEquals("备注", added.note)

        val removed = DayRecordEditing.toggleSymptom(added, Symptom.CRAMPS)
        assertEquals(emptySet<Symptom>(), removed.symptoms)
        assertEquals(Mood.CALM, removed.mood)
        assertEquals("备注", removed.note)
    }

    @Test
    fun `取消经期标记会保留症状心情与备注`() {
        val base = emptyDraft().copy(
            flow = FlowIntensity.MEDIUM,
            symptoms = setOf(Symptom.HEADACHE),
            mood = Mood.IRRITABLE,
            note = "痛",
        )

        val cleared = DayRecordEditing.setFlow(base, null)

        assertNull(cleared.flow)
        assertEquals(setOf(Symptom.HEADACHE), cleared.symptoms)
        assertEquals(Mood.IRRITABLE, cleared.mood)
        assertEquals("痛", cleared.note)
    }

    @Test
    fun `心情再点一次即取消且不动其他字段`() {
        val base = emptyDraft().copy(flow = FlowIntensity.HEAVY, note = "x")

        val selected = DayRecordEditing.toggleMood(base, Mood.HAPPY)
        assertEquals(Mood.HAPPY, selected.mood)

        val toggledOff = DayRecordEditing.toggleMood(selected, Mood.HAPPY)
        assertNull(toggledOff.mood)
        assertEquals(FlowIntensity.HEAVY, toggledOff.flow)
        assertEquals("x", toggledOff.note)
    }

    @Test
    fun `备注超过上限被截断`() {
        val tooLong = "字".repeat(SymptomEntry.MAX_NOTE_LENGTH + 50)

        val draft = DayRecordEditing.withNote(emptyDraft(), tooLong)

        assertEquals(SymptomEntry.MAX_NOTE_LENGTH, draft.note.length)
        assertTrue(draft.note.all { it == '字' })
    }
}
