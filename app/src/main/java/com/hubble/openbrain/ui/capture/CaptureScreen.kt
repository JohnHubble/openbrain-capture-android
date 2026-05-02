package com.hubble.openbrain.ui.capture

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hubble.openbrain.service.CapturePhase
import com.hubble.openbrain.service.isBusy
import com.hubble.openbrain.service.isRecording
import com.hubble.openbrain.ui.theme.LifecycleText
import com.hubble.openbrain.ui.theme.LocalOpenBrainStrings
import com.hubble.openbrain.ui.theme.LocalOpenBrainThemeId
import com.hubble.openbrain.ui.theme.LocalOpenBrainTokens
import com.hubble.openbrain.ui.theme.ThemeId
import com.hubble.openbrain.ui.theme.ThemeStrings
import java.util.Locale

@OptIn(com.google.accompanist.permissions.ExperimentalPermissionsApi::class)
@Composable
fun CaptureScreen(viewModel: CaptureViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val themeId = LocalOpenBrainThemeId.current

    val permissions = buildList {
        add(android.Manifest.permission.RECORD_AUDIO)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            add(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    val permissionState = com.google.accompanist.permissions.rememberMultiplePermissionsState(permissions)

    val onToggle: () -> Unit = {
        if (!state.phase.isBusy && state.phase !is CapturePhase.Preview) {
            if (state.phase.isRecording || permissionState.allPermissionsGranted) {
                viewModel.toggleCapture()
            } else {
                permissionState.launchMultiplePermissionRequest()
            }
        }
    }

    when (themeId) {
        ThemeId.SakuraMinimal -> SakuraCaptureScreen(
            state = state, onToggle = onToggle,
            onSavePreview = viewModel::confirmSave, onDiscardPreview = viewModel::discardPreview,
        )
        ThemeId.ComradeNotes -> ComradeCaptureScreen(
            state = state, onToggle = onToggle,
            onSavePreview = viewModel::confirmSave, onDiscardPreview = viewModel::discardPreview,
        )
        ThemeId.IlluminatedCodex -> CodexCaptureScreen(
            state = state, onToggle = onToggle,
            onSavePreview = viewModel::confirmSave, onDiscardPreview = viewModel::discardPreview,
        )
        ThemeId.MaterialDefault -> MaterialCaptureScreen(
            state = state, onToggle = onToggle,
            onSavePreview = viewModel::confirmSave, onDiscardPreview = viewModel::discardPreview,
        )
    }
}

@Composable
private fun MaterialCaptureScreen(
    state: CaptureUiState,
    onToggle: () -> Unit,
    onSavePreview: () -> Unit,
    onDiscardPreview: () -> Unit,
) {
    val strings = LocalOpenBrainStrings.current
    val text = phaseLifecycleText(state.phase, state.nearLimit, strings)
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        PageHeader(title = "Capture", subtitle = text.sub)
        StatusCard(
            mainLabel = text.main,
            subLabel = text.sub,
            phase = state.phase,
        )
        if (state.nearLimit && state.phase is CapturePhase.Recording) {
            NearLimitBanner(strings.nearLimit)
        }
        CaptureFab(
            label = phaseButtonLabel(state.phase, strings),
            phase = state.phase,
            onToggle = onToggle,
        )
        DurationCard(durationMs = state.durationMs, isRecording = state.phase is CapturePhase.Recording)
        if (state.phase is CapturePhase.Preview) {
            PreviewCard(
                transcript = state.previewTranscript ?: "",
                strings = strings,
                onSave = onSavePreview,
                onDiscard = onDiscardPreview,
            )
        }
        LastSessionCard(
            title = strings.latestTitle,
            text = state.lastSavedTranscript,
            secondsAgo = state.lastSavedSecondsAgo,
            durationMs = state.lastSavedDurationMs,
        )
    }
}

@Composable
private fun PageHeader(title: String, subtitle: String) {
    Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 16.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.W500,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun StatusCard(
    mainLabel: String,
    subLabel: String,
    phase: CapturePhase,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(phase = phase)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = mainLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.W500,
                    )
                    Text(
                        text = subLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (phase is CapturePhase.Recording || phase == CapturePhase.Transcribing || phase == CapturePhase.Saving) {
                Spacer(Modifier.height(14.dp))
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                )
            }
        }
    }
}

@Composable
private fun StatusDot(phase: CapturePhase) {
    if (phase is CapturePhase.Idle || phase is CapturePhase.Error || phase is CapturePhase.Saved) {
        Box(
            Modifier
                .size(24.dp)
                .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape),
        )
        return
    }
    val transition = rememberInfiniteTransition(label = "status-dot")
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scale",
    )
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "alpha",
    )
    val color = when (phase) {
        is CapturePhase.Recording -> LocalOpenBrainTokens.current.extras.success
        is CapturePhase.Preview -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }
    Box(
        Modifier
            .size(24.dp)
            .scale(scale)
            .graphicsLayer { this.alpha = alpha }
            .background(color, CircleShape),
    )
}

@Composable
private fun NearLimitBanner(text: LifecycleText) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.WarningAmber,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = text.main,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
                Text(
                    text = text.sub,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
    }
}

@Composable
private fun CaptureFab(label: String, phase: CapturePhase, onToggle: () -> Unit) {
    val isRecording = phase is CapturePhase.Recording
    val isBusy = phase.isBusy || phase is CapturePhase.Preview
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        val containerColor = when {
            isBusy -> MaterialTheme.colorScheme.secondaryContainer
            isRecording -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.primary
        }
        val contentColor = when {
            isBusy -> MaterialTheme.colorScheme.onSecondaryContainer
            isRecording -> MaterialTheme.colorScheme.onError
            else -> MaterialTheme.colorScheme.onPrimary
        }
        ExtendedFloatingActionButton(
            onClick = { if (!isBusy) onToggle() },
            containerColor = containerColor,
            contentColor = contentColor,
            shape = RoundedCornerShape(16.dp),
            icon = {
                if (isBusy) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = contentColor,
                    )
                } else {
                    Icon(
                        imageVector = if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
                        contentDescription = null,
                    )
                }
            },
            text = { Text(label) },
        )
    }
}

@Composable
private fun DurationCard(durationMs: Long, isRecording: Boolean) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(
                text = "DURATION",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    letterSpacing = 0.8.sp,
                ),
                fontWeight = FontWeight.W500,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = formatDuration(durationMs),
                fontFamily = FontFamily.Monospace,
                fontSize = 28.sp,
                fontWeight = FontWeight.W300,
                color = if (isRecording) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun PreviewCard(
    transcript: String,
    strings: ThemeStrings,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = "PREVIEW",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    letterSpacing = 0.8.sp,
                ),
                fontWeight = FontWeight.W500,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = transcript,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onSave,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    modifier = Modifier.weight(1f),
                ) { Text(strings.savePreview) }
                OutlinedButton(
                    onClick = onDiscard,
                    modifier = Modifier.weight(1f),
                ) { Text(strings.discardPreview) }
            }
        }
    }
}

@Composable
private fun LastSessionCard(title: String, text: String?, secondsAgo: Long, durationMs: Long) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = title.uppercase(Locale.US),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        letterSpacing = 0.8.sp,
                    ),
                    fontWeight = FontWeight.W500,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(10.dp))
            if (text == null) {
                Text(
                    text = "Capture your thoughts and they'll appear here",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "${formatAge(secondsAgo)} · ${formatDuration(durationMs)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

internal fun phaseLifecycleText(
    phase: CapturePhase,
    nearLimit: Boolean,
    strings: ThemeStrings,
): LifecycleText = when (phase) {
    is CapturePhase.Idle -> strings.idle
    is CapturePhase.Recording -> if (nearLimit) strings.nearLimit else strings.listening
    CapturePhase.Transcribing -> strings.transcribing
    is CapturePhase.Preview -> strings.preview
    CapturePhase.Saving -> strings.saving
    is CapturePhase.Saved -> strings.saved
    is CapturePhase.Error -> LifecycleText(strings.error.main, phase.message)
}

internal fun phaseButtonLabel(phase: CapturePhase, strings: ThemeStrings): String = when (phase) {
    is CapturePhase.Recording -> strings.button.stop
    CapturePhase.Transcribing, CapturePhase.Saving -> strings.button.processing
    is CapturePhase.Preview -> strings.savePreview
    else -> strings.button.start
}

internal fun formatDuration(ms: Long): String {
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
}

internal fun formatAge(seconds: Long): String = when {
    seconds < 2 -> "just now"
    seconds < 60 -> "${seconds}s ago"
    seconds < 3600 -> "${seconds / 60}m ago"
    else -> "${seconds / 3600}h ago"
}
