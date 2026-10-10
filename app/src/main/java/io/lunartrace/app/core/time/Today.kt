package io.lunartrace.app.core.time

import java.time.LocalDate

/**
 * "今天是哪一天"的唯一来源。
 *
 * 为什么需要它：预测与日历展示都依赖"今天"，如果各处直接调 `LocalDate.now()`，
 * 单元测试无法固定日期，同一个界面里也会出现两个不同的"今天"。
 */
interface Today {
    fun date(): LocalDate
}

/** 生产实现：本地时区的当前自然日。 */
class SystemToday : Today {
    override fun date(): LocalDate = LocalDate.now()
}
