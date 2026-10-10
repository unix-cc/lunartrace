package io.lunartrace.app

import android.app.Application
import android.content.Context
import androidx.room.Room
import io.lunartrace.app.core.time.SystemToday
import io.lunartrace.app.core.time.Today
import io.lunartrace.app.data.local.LunartraceDatabase
import io.lunartrace.app.data.prefs.SettingsStore
import io.lunartrace.app.data.prefs.settingsDataStore
import io.lunartrace.app.data.repository.RoomCycleRepository
import io.lunartrace.app.domain.repository.CycleRepository

/**
 * 手动依赖容器。本项目注入点不足 10 个，Hilt 的注解处理成本大于收益（见 `CLAUDE.md` §3）。
 */
class AppContainer(context: Context) {

    private val database: LunartraceDatabase = Room
        .databaseBuilder(context, LunartraceDatabase::class.java, LunartraceDatabase.NAME)
        .build()

    val today: Today = SystemToday()

    val cycleRepository: CycleRepository = RoomCycleRepository(
        periodDayDao = database.periodDayDao(),
        symptomEntryDao = database.symptomEntryDao(),
    )

    val settingsStore: SettingsStore = SettingsStore(context.settingsDataStore)
}

class LunartraceApp : Application() {

    /** 延迟创建：Application 构造时 `applicationContext` 尚不可用。 */
    val container: AppContainer by lazy { AppContainer(applicationContext) }
}
