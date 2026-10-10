package io.lunartrace.app.ui.lock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * 应用锁界面。
 *
 * 红线：本界面**不展示任何日期、症状或记录内容**，未解锁前不得泄漏数据
 * （见 `specs/capabilities/data-privacy.md` §2）。
 */
@Composable
fun LockScreen(
    state: LockUiState,
    onPinChanged: (String) -> Unit,
    onUnlockRequested: () -> Unit,
) {
    val lockedOut = state.lockoutSecondsRemaining > 0

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(16.dp))
            Text(text = "输入 PIN 解锁", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(
                value = state.pinInput,
                onValueChange = onPinChanged,
                label = { Text("PIN") },
                singleLine = true,
                enabled = !lockedOut,
                isError = state.pinRejected,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.pinRejected) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "PIN 不正确",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (lockedOut) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "尝试次数过多，请 ${state.lockoutSecondsRemaining} 秒后再试",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onUnlockRequested,
                enabled = state.pinInput.isNotBlank() && !lockedOut,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("解锁")
            }
            Spacer(Modifier.height(32.dp))
            Text(
                text = "忘记 PIN 只能清除全部数据，且不可恢复。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
