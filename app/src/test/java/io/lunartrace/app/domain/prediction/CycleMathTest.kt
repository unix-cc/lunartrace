package io.lunartrace.app.domain.prediction

import io.lunartrace.app.domain.model.PeriodEpisode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class CycleMathTest {

    private fun date(text: String): LocalDate = LocalDate.parse(text)

    @Test
    fun `没有记录时返回空段列表`() {
        assertEquals(emptyList<PeriodEpisode>(), CycleMath.episodes(emptyList()))
    }

    @Test
    fun `连续两天合并为一段`() {
        val episodes = CycleMath.episodes(listOf(date("2026-03-01"), date("2026-03-02")))

        assertEquals(1, episodes.size)
        assertEquals(date("2026-03-01"), episodes.first().start)
        assertEquals(date("2026-03-02"), episodes.first().endInclusive)
        assertEquals(2, episodes.first().lengthDays)
    }

    @Test
    fun `缺失一天仍然属于同一段`() {
        // 记为 1 日与 3 日（2 日漏记）→ 同一段，长度 3 天
        val episodes = CycleMath.episodes(listOf(date("2026-03-01"), date("2026-03-03")))

        assertEquals(1, episodes.size)
        assertEquals(3, episodes.first().lengthDays)
    }

    @Test
    fun `缺失两天以上拆成两段`() {
        val episodes = CycleMath.episodes(listOf(date("2026-03-01"), date("2026-03-04")))

        assertEquals(2, episodes.size)
        assertEquals(date("2026-03-01"), episodes[0].start)
        assertEquals(date("2026-03-04"), episodes[1].start)
    }

    @Test
    fun `跨年记录不影响分段与长度`() {
        val episodes = CycleMath.episodes(
            listOf(
                date("2025-12-30"),
                date("2025-12-31"),
                date("2026-01-01"),
                date("2026-01-02"),
                date("2026-01-27"),
            ),
        )

        assertEquals(2, episodes.size)
        assertEquals(4, episodes[0].lengthDays)
        assertEquals(listOf(28), CycleMath.cycleLengths(episodes))
    }

    @Test
    fun `周期长度取相邻段起始日之差`() {
        val episodes = CycleMath.episodes(
            listOf(date("2026-01-01"), date("2026-01-29"), date("2026-03-01")),
        )

        assertEquals(listOf(28, 31), CycleMath.cycleLengths(episodes))
    }

    @Test
    fun `段数少于二时没有周期长度`() {
        val episodes = CycleMath.episodes(listOf(date("2026-01-01")))

        assertEquals(emptyList<Int>(), CycleMath.cycleLengths(episodes))
    }

    @Test
    fun `中位数对奇偶数量都成立`() {
        assertEquals(28, CycleMath.median(listOf(30, 28, 26)))
        assertEquals(29, CycleMath.median(listOf(30, 28)))
        assertNull(CycleMath.median(emptyList()))
    }

    @Test
    fun `平均值四舍五入且空列表返回 null`() {
        assertEquals(29, CycleMath.average(listOf(28, 29, 30)))
        assertEquals(29, CycleMath.average(listOf(28, 30)))
        assertNull(CycleMath.average(emptyList()))
    }
}
