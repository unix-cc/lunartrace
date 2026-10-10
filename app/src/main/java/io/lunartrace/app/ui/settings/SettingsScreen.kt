package io.lunartrace.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.lunartrace.app.R
import io.lunartrace.app.domain.model.PinPolicy
import io.lunartrace.app.ui.lunartraceViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = lunartraceViewModelFactory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            PrivacyCard(
                appLockEnabled = state.appLockEnabled,
                onLockToggled = { checked ->
                    if (checked) viewModel.onSetPinRequested() else viewModel.onDisablePinRequested()
                },
            )
            Spacer(Modifier.height(16.dp))
            DataCard(
                periodDayCount = state.periodDayCount,
                symptomEntryCount = state.symptomEntryCount,
                onWipeRequested = viewModel::onWipeRequested,
            )
            Spacer(Modifier.height(16.dp))
            AboutCard()
            Spacer(Modifier.height(24.dp))
        }
    }

    SettingsDialogs(state = state, viewModel = viewModel)
}

@Composable
private fun PrivacyCard(appLockEnabled: Boolean, onLockToggled: (Boolean) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "隐私", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "应用锁", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = "开启后每次冷启动都需要输入 PIN。忘记 PIN 只能清除数据。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = appLockEnabled, onCheckedChange = onLockToggled)
            }
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            Text(
                text = "本应用不声明任何联网权限：记录只保存在本机，不参与系统云备份，也没有任何上报。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DataCard(periodDayCount: Int, symptomEntryCount: Int, onWipeRequested: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "数据", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                text = "已记录 $periodDayCount 天经期、$symptomEntryCount 天症状。",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onWipeRequested, modifier = Modifier.fillMaxWidth()) {
                Text(text = "清除全部数据", color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "清除是真删除，没有任何恢复途径。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AboutCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "关于", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(text = stringResource(R.string.app_name), style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "本应用不是医疗设备，不提供任何医学判断；所有预计都只是基于你自己记录的统计推算。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingsDialogs(state: SettingsUiState, viewModel: SettingsViewModel) {
    when (state.dialog) {
        SettingsDialog.SET_PIN -> SetPinDialog(
            onDismiss = viewModel::onDialogDismissed,
            onConfirm = viewModel::enableAppLock,
        )
        SettingsDialog.DISABLE_PIN -> DisableLockDialog(
            rejected = state.pinRejected,
            onDismiss = viewModel::onDialogDismissed,
            onConfirm = viewModel::disableAppLock,
        )
        SettingsDialog.CONFIRM_WIPE -> WipeDialog(
            onDismiss = viewModel::onDialogDismissed,
            onConfirm = viewModel::wipeAllData,
        )
        SettingsDialog.NONE -> Unit
    }
}

@Composable
private fun SetPinDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var pin by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    val mismatch = confirmation.isNotEmpty() && pin != confirmation
    val valid = PinPolicy.isValid(pin) && pin == confirmation

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设置 PIN") },
        text = {
            Column {
                PinField(
                    value = pin,
                    label = "PIN（4–6 位数字）",
                    isError = false,
                    onValueChange = { pin = it },
                )
                Spacer(Modifier.height(8.dp))
                PinField(
                    value = confirmation,
                    label = "再输入一次",
                    isError = mismatch,
                    onValueChange = { confirmation = it },
                )
                if (mismatch) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "两次输入不一致",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(pin) }, enabled = valid) { Text("开启应用锁") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun DisableLockDialog(rejected: Boolean, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var pin by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("关闭应用锁") },
        text = {
            Column {
                Text(text = "请输入当前 PIN 以确认。", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                PinField(
                    value = pin,
                    label = "PIN",
                    isError = rejected,
                    onValueChange = { pin = it },
                )
                if (rejected) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "PIN 不正确",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(pin) }, enabled = PinPolicy.isValid(pin)) { Text("关闭") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun PinField(value: String, label: String, isError: Boolean, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { input -> onValueChange(input.filter(Char::isDigit).take(PinPolicy.MAX_LENGTH)) },
        label = { Text(label) },
        singleLine = true,
        isError = isError,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun WipeDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("清除全部数据？") },
        text = { Text(text = "所有经期记录、症状、备注与应用锁设置都会被删除，且不可恢复。") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = "确认清除", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
