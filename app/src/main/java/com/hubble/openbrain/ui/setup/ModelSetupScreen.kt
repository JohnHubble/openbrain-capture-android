package com.hubble.openbrain.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hubble.openbrain.data.prefs.WhisperModel
import com.hubble.openbrain.transcribe.ModelState
import java.util.Locale

@Composable
fun ModelSetupScreen(viewModel: ModelSetupViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val selected by viewModel.selectedModel.collectAsStateWithLifecycle()

    Scaffold { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            when (val s = state) {
                ModelState.Checking -> CheckingBlock()
                is ModelState.NotPresent -> NotPresentBlock(
                    selected = selected,
                    onSelect = viewModel::selectModel,
                    onStart = viewModel::startDownload,
                )
                is ModelState.Downloading -> DownloadingBlock(
                    modelName = s.model.displayName,
                    bytes = s.bytes,
                    total = s.total,
                    onCancel = viewModel::cancelDownload,
                )
                is ModelState.Failed -> FailedBlock(
                    message = s.message,
                    onRetry = viewModel::startDownload,
                )
                is ModelState.Ready -> Box(Modifier.fillMaxSize()) // NavHost will take over
            }
        }
    }
}

@Composable
private fun CheckingBlock() {
    LinearProgressIndicator(Modifier.fillMaxWidth())
    Spacer(Modifier.height(16.dp))
    Text("Checking for model…", style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun NotPresentBlock(
    selected: WhisperModel,
    onSelect: (WhisperModel) -> Unit,
    onStart: () -> Unit,
) {
    Icon(
        imageVector = Icons.Filled.CloudDownload,
        contentDescription = null,
        modifier = Modifier.size(64.dp),
        tint = MaterialTheme.colorScheme.primary,
    )
    Spacer(Modifier.height(16.dp))
    Text(
        text = "Download Whisper",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.W500,
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = "Open Brain transcribes on device. Bigger models are more accurate but slower and use more storage.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
    Spacer(Modifier.height(20.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        WhisperModel.entries.forEach { model ->
            FilterChip(
                selected = model == selected,
                onClick = { onSelect(model) },
                label = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            model.displayName,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.W500,
                        )
                        Text(
                            "${model.sizeMb} MB",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                modifier = Modifier.weight(1f),
                colors = FilterChipDefaults.filterChipColors(),
            )
        }
    }
    Spacer(Modifier.height(24.dp))
    Button(onClick = onStart) {
        Icon(Icons.Filled.CloudDownload, contentDescription = null)
        Spacer(Modifier.size(8.dp))
        Text("Download ${selected.displayName} (~${selected.sizeMb} MB)")
    }
}

@Composable
private fun DownloadingBlock(modelName: String, bytes: Long, total: Long, onCancel: () -> Unit) {
    // Connection establishment can take a while before the first byte arrives. Until the
    // server replies with bytes, show a distinct "Connecting" label so the user doesn't
    // think the indeterminate bar is stuck.
    val connecting = bytes == 0L
    val fraction = if (total > 0 && !connecting) (bytes.toFloat() / total).coerceIn(0f, 1f) else null
    Text(
        text = if (connecting) "Connecting to Hugging Face…" else "Downloading $modelName",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.W500,
    )
    Spacer(Modifier.height(16.dp))
    if (fraction != null) {
        LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
    } else {
        LinearProgressIndicator(Modifier.fillMaxWidth())
    }
    Spacer(Modifier.height(8.dp))
    val progressLine = when {
        connecting -> "Establishing connection…"
        total > 0 -> "${formatMb(bytes)} / ${formatMb(total)}  ·  ${(fraction!! * 100).toInt()}%"
        else -> formatMb(bytes)
    }
    Text(
        text = progressLine,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(24.dp))
    TextButton(onClick = onCancel) { Text("Cancel") }
}

@Composable
private fun FailedBlock(message: String, onRetry: () -> Unit) {
    Icon(
        imageVector = Icons.Filled.ErrorOutline,
        contentDescription = null,
        modifier = Modifier.size(64.dp),
        tint = MaterialTheme.colorScheme.error,
    )
    Spacer(Modifier.height(16.dp))
    Text(
        text = "Download failed",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.W500,
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
    Spacer(Modifier.height(24.dp))
    Button(onClick = onRetry) { Text("Retry") }
}

private fun formatMb(bytes: Long): String =
    String.format(Locale.US, "%.1f MB", bytes.toDouble() / (1024 * 1024))
