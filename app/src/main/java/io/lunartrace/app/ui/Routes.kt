package io.lunartrace.app.ui

/** 导航路由。单独成文件，让入口文件专注"能不能看 + 看哪个页面"。 */
object Routes {
    /** 启动页：一眼看到"我现在在哪"（见 `specs/changes/home-tab/spec.md`）。 */
    const val HOME = "home"
    const val CALENDAR = "calendar"
    const val STATS = "stats"
    const val SETTINGS = "settings"
}
