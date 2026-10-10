package io.lunartrace.app.ui.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 环的角度约定：**0 度在正上方、顺时针、标记落在格中心**。
 *
 * 这是画面里最容易差一格的地方（差一格整圈标记都会偏），而画出来的东西没法靠单测发现，
 * 所以把角度换算抽成纯函数钉在这里。
 *
 * 坐标提醒：Compose 的 y 轴向下，因此 45 度是"右下方"、225 度是"左上方"。
 */
class CycleRingGeometryTest {

    @Test
    fun `四等分时四个格中心落在四个象限对角线`() {
        assertEquals(-45f, ringAngleForDay(dayIndex = 1, totalDays = 4), TOLERANCE)
        assertEquals(45f, ringAngleForDay(dayIndex = 2, totalDays = 4), TOLERANCE)
        assertEquals(135f, ringAngleForDay(dayIndex = 3, totalDays = 4), TOLERANCE)
        assertEquals(225f, ringAngleForDay(dayIndex = 4, totalDays = 4), TOLERANCE)
    }

    @Test
    fun `第一格落在第一圈的开头而不是边界上`() {
        val first = ringAngleForDay(dayIndex = 1, totalDays = 28)

        assertTrue("第一格应当比正上方顺时针一点点：$first", first > -90f)
        assertTrue("第一格不应当越过自己的格宽：$first", first < -90f + 360f / 28f)
    }

    @Test
    fun `最后一格落在第一圈结束之前`() {
        val last = ringAngleForDay(dayIndex = 28, totalDays = 28)

        assertTrue("最后一格应当在正上方之前结束：$last", last < -90f + 360f)
        assertTrue("最后一格不应当回绕到起点之前：$last", last > -90f + 360f - 360f / 28f)
    }

    private companion object {
        const val TOLERANCE = 0.001f
    }
}
