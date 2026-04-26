package com.hubble.openbrain.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hubble.openbrain.data.prefs.WhisperModel
import com.hubble.openbrain.ui.theme.LocalOpenBrainStrings
import com.hubble.openbrain.ui.theme.ThemedPageTitle
import com.hubble.openbrain.ui.theme.ThemedSectionHeader
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onOpenAppearance: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val storage by viewModel.storageBytes.collectAsStateWithLifecycle()
    val testState by viewModel.testState.collectAsStateWithLifecycle()

    var editingEndpoint by remember { mutableStateOf(false) }
    var editingKey by remember { mutableStateOf(false) }
    var pickingModel by remember { mutableStateOf(false) }
    var confirmingClearQueue by remember { mutableStateOf(false) }
    var confirmingReset by remember { mutableStateOf(false) }

    val strings = LocalOpenBrainStrings.current
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        ThemedPageTitle(tab = strings.settingsTab)

        ThemedSectionHeader("Appearance")
        SettingsItem(
            icon = Icons.Filled.Palette,
            title = "Theme",
            subtitle = state.themeId.displayName,
            onClick = onOpenAppearance,
        )

        ThemedSectionHeader("Connection")
        SettingsItem(
            icon = Icons.Filled.Link,
            title = "Open Brain endpoint",
            subtitle = state.endpoint,
            subtitleMono = true,
            onClick = { editingEndpoint = true },
        )
        SettingsItem(
            icon = Icons.Filled.Key,
            title = "Access key",
            subtitle = if (state.accessKey.isBlank()) "Not set" else maskKey(state.accessKey),
            subtitleMono = state.accessKey.isNotBlank(),
            onClick = { editingKey = true },
        )
        SettingsItem(
            icon = Icons.Filled.WifiTethering,
            title = "Test connection",
            subtitle = when (testState) {
                is TestState.Running -> "Contacting OB1…"
                is TestState.Success -> "Last attempt: success"
                is TestState.Failure -> "Last attempt: failed"
                else -> "Send a test thought and see if OB1 accepts it"
            },
            onClick = viewModel::testConnection,
        )

        ThemedSectionHeader("Transcription")
        SettingsItem(
            icon = Icons.Filled.Mic,
            title = "Whisper model",
            subtitle = "${state.whisperModel.displayName} · ${state.whisperModel.sizeMb} MB",
            onClick = { pickingModel = true },
        )
        SwitchItem(
            icon = Icons.Filled.GraphicEq,
            title = "Keep raw audio",
            subtitle = "Retain PCM after transcription",
            checked = state.audioRetention,
            onCheckedChange = viewModel::setAudioRetention,
        )

        ThemedSectionHeader("Storage")
        StorageItem(usedBytes = storage.first, totalBytes = storage.second)

        ThemedSectionHeader("Danger zone", danger = true)
        SettingsItem(
            icon = Icons.Filled.DeleteSweep,
            title = "Clear queue",
            subtitle = "Delete pending and failed thoughts",
            danger = true,
            onClick = { confirmingClearQueue = true },
        )
        SettingsItem(
            icon = Icons.Filled.RestartAlt,
            title = "Reset all settings",
            subtitle = "Wipe preferences and all thoughts",
            danger = true,
            onClick = { confirmingReset = true },
        )
    }

    if (editingEndpoint) {
        TextEditDialog(
            title = "Open Brain endpoint",
            initial = state.endpoint,
            keyboardType = KeyboardType.Uri,
            onDismiss = { editingEndpoint = false },
            onConfirm = { viewModel.setEndpoint(it); editingEndpoint = false },
        )
    }
    if (editingKey) {
        TextEditDialog(
            title = "Access key",
            initial = state.accessKey,
            keyboardType = KeyboardType.Password,
            masked = true,
            onDismiss = { editingKey = false },
            onConfirm = { viewModel.setAccessKey(it); editingKey = false },
        )
    }
    if (pickingModel) {
        WhisperModelDialog(
            current = state.whisperModel,
            onDismiss = { pickingModel = false },
            onSelect = { viewModel.setWhisperModel(it); pickingModel = false },
        )
    }
    if (confirmingClearQueue) {
        ConfirmDialog(
            title = "Clear queue?",
            message = "This deletes pending and failed thoughts. Sent thoughts stay.",
            confirmLabel = "Clear",
            danger = true,
            onDismiss = { confirmingClearQueue = false },
            onConfirm = { viewModel.clearQueue(); confirmingClearQueue = false },
        )
    }
    if (confirmingReset) {
        ConfirmDialog(
            title = "Reset everything?",
            message = "This wipes all settings and every thought in the database. Not recoverable.",
            confirmLabel = "Reset",
            danger = true,
            onDismiss = { confirmingReset = false },
            onConfirm = { viewModel.resetAll(); confirmingReset = false },
        )
    }
    when (val ts = testState) {
        is TestState.Success -> AlertDialog(
            onDismissRequest = viewModel::dismissTest,
            confirmButton = { TextButton(onClick = viewModel::dismissTest) { Text("OK") } },
            title = { Text("Connection OK") },
            text = { Text("OB1 accepted the thought.\n\nResponse:\n${ts.responseText}") },
        )
        is TestState.Failure -> AlertDialog(
            onDismissRequest = viewModel::dismissTest,
            confirmButton = { TextButton(onClick = viewModel::dismissTest) { Text("OK") } },
            title = { Text("Connection failed") },
            text = {
                Text(
                    text = ts.message,
                    color = MaterialTheme.colorScheme.error,
                )
            },
        )
        else -> Unit
    }
}

@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    subtitleMono: Boolean = false,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    val textColor = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    ListItem(
        headlineContent = {
            Text(
                text = title,
                color = textColor,
                fontWeight = FontWeight.W500,
            )
        },
        supportingContent = {
            Text(
                text = subtitle,
                style = if (subtitleMono) MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                else MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(20.dp),
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun SwitchItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.W500) },
        supportingContent = {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        leadingContent = {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp))
        },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable { onCheckedChange(!checked) },
    )
}

@Composable
private fun StorageItem(usedBytes: Long, totalBytes: Long) {
    val fraction = if (totalBytes <= 0L) 0f else (usedBytes.toFloat() / totalBytes).coerceIn(0f, 1f)
    ListItem(
        headlineContent = { Text("Storage used", fontWeight = FontWeight.W500) },
        supportingContent = {
            Column {
                Text(
                    text = "${formatBytes(usedBytes)} of ${formatBytes(totalBytes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                )
            }
        },
        leadingContent = {
            Icon(Icons.Filled.Storage, contentDescription = null, modifier = Modifier.size(20.dp))
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

@Composable
private fun TextEditDialog(
    title: String,
    initial: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    masked: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                visualTransformation = if (masked) PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(value) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun WhisperModelDialog(
    current: WhisperModel,
    onDismiss: () -> Unit,
    onSelect: (WhisperModel) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Whisper model") },
        text = {
            Column {
                WhisperModel.entries.forEach { model ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(model) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = model == current, onClick = { onSelect(model) })
                        Spacer(Modifier.size(8.dp))
                        Column {
                            Text(model.displayName, fontWeight = FontWeight.W500)
                            Text(
                                text = "${model.sizeMb} MB",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

@Composable
private fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    danger: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    confirmLabel,
                    color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun maskKey(key: String): String = if (key.length <= 8) "••••" else "••••${key.takeLast(4)}"

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val units = listOf("KB", "MB", "GB", "TB")
    var value = bytes.toDouble() / 1024
    var unit = 0
    while (value >= 1024 && unit < units.lastIndex) {
        value /= 1024
        unit++
    }
    return String.format(Locale.US, "%.1f %s", value, units[unit])
}
