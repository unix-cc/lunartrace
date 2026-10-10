package io.lunartrace.app.ui.calendar

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 空状态提示的判定。红先绿后：本文件先于实现写出。
 */
class PredictionHintTest {

    @Test
    fun `有预测时不需要额外提示`() {
        val hint = PredictionHints.of(
            hasPeriodRecords = true,
            hasDefaultCycleLength = true,
            hasPrediction = true,
        )

        assertEquals(PredictionHint.NONE, hint)
    }

    @Test
    fun `零记录但填了缺省值时只需再记录一次`() {
        val hint = PredictionHints.of(
            hasPeriodRecords = false,
            hasDefaultCycleLength = true,
            hasPrediction = false,
        )

        assertEquals(PredictionHint.RECORD_FIRST_PERIOD, hint)
    }

    @Test
    fun `零记录且未填缺省值时给出两条路`() {
        val hint = PredictionHints.of(
            hasPeriodRecords = false,
            hasDefaultCycleLength = false,
            hasPrediction = false,
        )

        assertEquals(PredictionHint.RECORD_FIRST_PERIOD_OR_SET_DEFAULTS, hint)
    }

    @Test
    fun `有记录却推不出来时提示检查记录`() {
        val hint = PredictionHints.of(
            hasPeriodRecords = true,
            hasDefaultCycleLength = true,
            hasPrediction = false,
        )

        assertEquals(PredictionHint.CHECK_RECORDS, hint)
    }
}
