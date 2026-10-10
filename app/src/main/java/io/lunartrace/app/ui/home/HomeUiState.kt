package io.lunartrace.app.ui.home

import io.lunartrace.app.domain.model.Confidence
import io.lunartrace.app.domain.model.CyclePhase
import java.time.LocalDate

/**
 * 主页状态：**只读**，没有任何写操作（记录仍然只在日历页发生）。
 *
 * 事实与推算分开存放：`today` / `phase` / [ring] 里的"第 N 天"是事实，
 * [nextPeriodStart] / [confidence] 是推算，文案上必须带"预计"。
 */
data class HomeUiState(
    val today: LocalDate,
    val phase: CyclePhase,
    val ring: CycleRingData,
    /**
     * 下次经期预计日；`null` 表示还推不出来。
     * 此时主页**不显示那一行**——环的圆心已经说了"还没有记录"，不必再换句话重复一遍。
     */
    val nextPeriodStart: LocalDate?,
    val confidence: Confidence,
)
