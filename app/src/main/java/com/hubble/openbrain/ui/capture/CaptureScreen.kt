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
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.hubble.openbrain.ui.theme.LocalOpenBrainStrings
import com.hubble.openbrain.ui.theme.LocalOpenBrainThemeId
import com.hubble.openbrain.ui.theme.LocalOpenBrainTokens
import com.hubble.openbrain.ui.theme.ThemeId
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
        if (!state.isProcessing) {
            if (state.isCapturing || permissionState.allPermissionsGranted) {
                viewModel.toggleCapture()
            } else {
                permissionState.launchMultiplePermissionRequest()
            }
        }
    }

    when (themeId) {
        ThemeId.SakuraMinimal -> SakuraCaptureScreen(state = state, onToggle = onToggle)
        ThemeId.ComradeNotes -> ComradeCaptureScreen(state = state, onToggle = onToggle)
        ThemeId.IlluminatedCodex -> CodexCaptureScreen(state = state, onToggle = onToggle)
        ThemeId.MaterialDefault -> MaterialCaptureScreen(state = state, onToggle = onToggle)
    }
}

@Composable
private fun MaterialCaptureScreen(state: CaptureUiState, onToggle: () -> Unit) {
    val strings = LocalOpenBrainStrings.current
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        PageHeader(
            title = "Capture",
            subtitle = when {
                state.isProcessing -> strings.processing.sub
                state.isCapturing -> "Capturing audio"
                else -> strings.pageSubtitle
            },
        )
        StatusCard(
            mainLabel = when {
                state.isProcessing -> strings.processing.main
                state.isCapturing -> strings.listening.main
                else -> strings.idle.main
            },
            subLabel = when {
                state.isProcessing -> strings.processing.sub
                state.isCapturing -> strings.listening.sub
                else -> strings.idle.sub
            },
            isCapturing = state.isCapturing,
            isProcessing = state.isProcessing,
        )
        CaptureFab(
            label = when {
                state.isProcessing -> strings.button.processing
                state.isCapturing -> strings.button.stop
                else -> strings.button.start
            },
            isCapturing = state.isCapturing,
            isProcessing = state.isProcessing,
            onToggle = onToggle,
        )
        StatsGrid(state = state)
        LatestThoughtCard(
            title = strings.latestTitle,
            text = state.latestThought,
            secondsAgo = state.latestThoughtSecondsAgo,
            isCapturing = state.isCapturing,
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
    isCapturing: Boolean,
    isProcessing: Boolean,
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
                StatusDot(isCapturing = isCapturing, isProcessing = isProcessing)
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
            if (isCapturing || isProcessing) {
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
private fun StatusDot(isCapturing: Boolean, isProcessing: Boolean) {
    if (!isCapturing && !isProcessing) {
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
    val color = if (isProcessing) MaterialTheme.colorScheme.primary
    else LocalOpenBrainTokens.current.extras.success
    Box(
        Modifier
            .size(24.dp)
            .scale(scale)
            .graphicsLayer { this.alpha = alpha }
            .background(color, CircleShape),
    )
}

@Composable
private fun CaptureFab(label: String, isCapturing: Boolean, isProcessing: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        val containerColor = when {
            isProcessing -> MaterialTheme.colorScheme.secondaryContainer
            isCapturing -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.primary
        }
        val contentColor = when {
            isProcessing -> MaterialTheme.colorScheme.onSecondaryContainer
            isCapturing -> MaterialTheme.colorScheme.onError
            else -> MaterialTheme.colorScheme.onPrimary
        }
        ExtendedFloatingActionButton(
            onClick = { if (!isProcessing) onToggle() },
            containerColor = containerColor,
            contentColor = contentColor,
            shape = RoundedCornerShape(16.dp),
            icon = {
                if (isProcessing) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = contentColor,
                    )
                } else {
                    Icon(
                        imageVector = if (isCapturing) Icons.Filled.Stop else Icons.Filled.Mic,
                        contentDescription = null,
                    )
                }
            },
            text = { Text(label) },
        )
    }
}

@Composable
private fun StatsGrid(state: CaptureUiState) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard(
                label = "Duration",
                value = formatDuration(state.durationMs),
                highlight = state.isCapturing,
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = "Captured",
                value = state.capturedCount.toString(),
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard(
                label = "Sent",
                value = state.sentCount.toString(),
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = "Queued",
                value = state.queuedCount.toString(),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    highlight: Boolean = false,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(
                text = label.uppercase(Locale.US),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    letterSpacing = 0.8.sp,
                ),
                fontWeight = FontWeight.W500,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                fontFamily = FontFamily.Monospace,
                fontSize = 28.sp,
                fontWeight = FontWeight.W300,
                color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun LatestThoughtCard(title: String, text: String?, secondsAgo: Long, isCapturing: Boolean) {
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
                    text = formatAge(secondsAgo, isCapturing),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
}

@Suppress("UNUSED_PARAMETER")
private fun formatAge(seconds: Long, isCapturing: Boolean): String = when {
    seconds < 2 -> "just now"
    seconds < 60 -> "${seconds}s ago"
    seconds < 3600 -> "${seconds / 60}m ago"
    else -> "${seconds / 3600}h ago"
}
