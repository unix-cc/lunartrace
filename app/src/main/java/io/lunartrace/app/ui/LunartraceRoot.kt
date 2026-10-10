package io.lunartrace.app.ui

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.lunartrace.app.ui.calendar.CalendarScreen
import io.lunartrace.app.ui.lock.LockScreen
import io.lunartrace.app.ui.lock.LockViewModel
import io.lunartrace.app.ui.settings.SettingsScreen
import io.lunartrace.app.ui.stats.StatsScreen

/**
 * 应用根节点：先决定"能不能看"，再决定"看哪个页面"。
 */
@Composable
fun LunartraceRoot() {
    val lockViewModel: LockViewModel = viewModel(factory = lunartraceViewModelFactory)
    val lockState by lockViewModel.uiState.collectAsStateWithLifecycle()

    SecureWindowEffect(enabled = lockState.lockEnabled == true)

    when {
        // 设置尚未读出：留白，避免解锁前闪出记录内容
        lockState.lockEnabled == null -> Surface(modifier = Modifier.fillMaxSize()) {}
        lockState.locked -> LockScreen(
            state = lockState,
            onPinChanged = lockViewModel::onPinChanged,
            onUnlockRequested = lockViewModel::onUnlockRequested,
        )
        else -> MainScaffold()
    }
}

/** 应用锁开启时给窗口加 FLAG_SECURE，防止最近任务列表缩略图泄漏记录。 */
@Composable
private fun SecureWindowEffect(enabled: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return

    DisposableEffect(enabled, view) {
        val window = (view.context as? Activity)?.window
        if (enabled) {
            window?.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        }
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }
}

@Composable
private fun MainScaffold() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute == Routes.CALENDAR || currentRoute == Routes.STATS

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomBar(navController = navController, currentRoute = currentRoute)
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.CALENDAR,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.CALENDAR) {
                CalendarScreen(onOpenSettings = { navController.navigate(Routes.SETTINGS) })
            }
            composable(Routes.STATS) {
                StatsScreen()
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

@Composable
private fun BottomBar(navController: NavHostController, currentRoute: String?) {
    NavigationBar {
        NavigationBarItem(
            selected = currentRoute == Routes.CALENDAR,
            onClick = {
                if (currentRoute != Routes.CALENDAR) {
                    navController.navigate(Routes.CALENDAR) { launchSingleTop = true }
                }
            },
            icon = { Icon(Icons.Filled.CalendarMonth, contentDescription = null) },
            label = { Text("日历") },
        )
        NavigationBarItem(
            selected = currentRoute == Routes.STATS,
            onClick = {
                if (currentRoute != Routes.STATS) {
                    navController.navigate(Routes.STATS) { launchSingleTop = true }
                }
            },
            icon = { Icon(Icons.Filled.BarChart, contentDescription = null) },
            label = { Text("统计") },
        )
    }
}
