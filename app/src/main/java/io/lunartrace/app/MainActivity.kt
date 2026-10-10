package io.lunartrace.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import io.lunartrace.app.ui.LunartraceRoot
import io.lunartrace.app.ui.theme.LunartraceTheme

/** 单 Activity 应用：所有界面由 Compose 承载。 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LunartraceTheme {
                LunartraceRoot()
            }
        }
    }
}
