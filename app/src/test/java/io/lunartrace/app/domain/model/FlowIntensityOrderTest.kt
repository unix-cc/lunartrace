package io.lunartrace.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 流量档位的**声明顺序**是 UI 的隐式依赖：记录弹层的水滴选择器按顺序填充，
 * "填充到第 N 个"表示第 N 档强度（见 `DayRecordSheet.FlowDropletPicker`）。
 * 在中间插入一个新档位会悄悄改变语义，所以把顺序钉在这里。
 *
 * 诚实说明：这条是**事后补的守护性测试**，不是红先绿后的一部分——
 * 该依赖是在实现水滴选择器时才出现的。
 */
class FlowIntensityOrderTest {

    @Test
    fun `流量档位必须由轻到重声明`() {
        assertEquals(
            listOf("SPOTTING", "LIGHT", "MEDIUM", "HEAVY"),
            FlowIntensity.entries.map { it.name },
        )
    }
}
