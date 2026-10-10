package io.lunartrace.app.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.lunartrace.app.LunartraceApp
import io.lunartrace.app.ui.calendar.CalendarViewModel
import io.lunartrace.app.ui.home.HomeViewModel
import io.lunartrace.app.ui.lock.LockViewModel
import io.lunartrace.app.ui.settings.SettingsViewModel
import io.lunartrace.app.ui.stats.StatsViewModel

/**
 * 全部 ViewModel 的唯一工厂（手动 DI 的注入点）。
 * ViewModel 只拿到自己需要的东西，不认识 `Context`，也拿不到 DAO。
 */
val lunartraceViewModelFactory: ViewModelProvider.Factory = viewModelFactory {
    initializer {
        CalendarViewModel(app().container.cycleRepository, app().container.today, app().container.settingsStore)
    }
    initializer {
        HomeViewModel(app().container.cycleRepository, app().container.today, app().container.settingsStore)
    }
    initializer {
        StatsViewModel(app().container.cycleRepository, app().container.today, app().container.settingsStore)
    }
    initializer { SettingsViewModel(app().container.settingsStore, app().container.cycleRepository) }
    initializer { LockViewModel(app().container.settingsStore) }
}

private fun CreationExtras.app(): LunartraceApp {
    val application = checkNotNull(this[APPLICATION_KEY]) { "缺少 Application，无法构造 ViewModel" }
    require(application is LunartraceApp) { "Application 必须声明为 LunartraceApp" }
    return application
}
