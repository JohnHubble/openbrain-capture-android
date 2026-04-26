package com.hubble.openbrain.ui.capture

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hubble.openbrain.R
import com.hubble.openbrain.ui.theme.LocalOpenBrainStrings
import com.hubble.openbrain.ui.theme.LocalOpenBrainTokens
import com.hubble.openbrain.ui.theme.SakuraSans
import com.hubble.openbrain.ui.theme.SakuraSerif
import java.util.Locale

/**
 * Sakura Minimal — quiet, elemental, Muji restraint. Layout mirrors v5-sakura.html:
 * hairline rules, centered toggle with sakura-branch circular backdrop, serif titles
 * with kanji annotations, stat rows instead of cards.
 */
@Composable
fun SakuraCaptureScreen(
    state: CaptureUiState,
    onToggle: () -> Unit,
) {
    val strings = LocalOpenBrainStrings.current
    val ink = MaterialTheme.colorScheme.onSurface
    val inkFaded = MaterialTheme.colorScheme.onSurfaceVariant
    val hairline = MaterialTheme.colorScheme.outline

    val main = when {
        state.isProcessing -> strings.processing.main
        state.isCapturing -> strings.listening.main
        else -> strings.idle.main
    }
    val sub = when {
        state.isProcessing -> strings.processing.sub
        state.isCapturing -> strings.listening.sub
        else -> strings.idle.sub
    }
    val buttonLabel = when {
        state.isProcessing -> strings.button.processing
        state.isCapturing -> strings.button.stop
        else -> strings.button.start
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp),
    ) {
        // ── Page header ──────────────────────────────
        Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 28.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "listen",
                    fontFamily = SakuraSerif,
                    fontWeight = FontWeight.Light,
                    fontSize = 26.sp,
                    color = ink,
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "聴",
                    fontFamily = SakuraSans,
                    fontWeight = FontWeight.Light,
                    fontSize = 18.sp,
                    color = inkFaded,
                    modifier = Modifier.padding(bottom = 3.dp),
                )
            }
            Text(
                text = strings.pageSubtitle,
                fontFamily = SakuraSans,
                fontWeight = FontWeight.Light,
                fontSize = 11.sp,
                color = inkFaded,
                letterSpacing = 0.18.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        Hairline(hairline, vPadding = 24.dp)

        // ── Focal area: sakura backdrop + toggle ── v5 spec: 160dp circle at 38% opacity
        Box(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 28.dp),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.sakura_branch),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(160.dp)
                    .clip(CircleShape)
                    .alpha(0.38f),
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                SakuraToggleButton(
                    label = buttonLabel,
                    isCapturing = state.isCapturing,
                    isProcessing = state.isProcessing,
                    onClick = onToggle,
                )
                Spacer(Modifier.height(14.dp))
                SakuraStatusRow(
                    mainLabel = main,
                    subLabel = sub,
                    isCapturing = state.isCapturing,
                    isProcessing = state.isProcessing,
                )
            }
        }

        Hairline(hairline, vPadding = 0.dp, hPadding = 24.dp)

        // ── Stats ── v5 spec: plain "this session" label (no kanji), 32sp timer
        Column(Modifier.padding(horizontal = 24.dp, vertical = 24.dp)) {
            SectionLabel("this session", kanji = null, inkFaded = inkFaded)
            Spacer(Modifier.height(14.dp))
            Text(
                text = formatDuration(state.durationMs),
                fontFamily = SakuraSans,
                fontWeight = FontWeight.ExtraLight,
                fontSize = 32.sp,
                color = ink,
            )
            Text(
                text = "elapsed",
                fontFamily = SakuraSans,
                fontSize = 10.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 2.0.sp,
                color = inkFaded,
                modifier = Modifier.padding(top = 2.dp),
            )
            Spacer(Modifier.height(20.dp))
            StatRow(state.capturedCount.toString(), "captured", ink, inkFaded)
            Spacer(Modifier.height(10.dp))
            StatRow(state.sentCount.toString(), "sent", ink, inkFaded)
            Spacer(Modifier.height(10.dp))
            StatRow(state.queuedCount.toString(), "queued", ink, inkFaded)
        }

        Hairline(hairline, vPadding = 0.dp, hPadding = 24.dp)

        // ── Last thought ── v5 spec: "最後" (last/latest) as the kanji suffix
        Column(Modifier.padding(horizontal = 24.dp, vertical = 24.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                SectionLabel(strings.latestTitle.lowercase(Locale.US), kanji = "最後", inkFaded = inkFaded)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = state.latestThought ?: "—",
                fontFamily = SakuraSerif,
                fontWeight = FontWeight.Light,
                fontSize = 15.sp,
                lineHeight = 24.sp,
                color = ink,
            )
        }
    }
}

@Composable
private fun SakuraToggleButton(
    label: String,
    isCapturing: Boolean,
    isProcessing: Boolean,
    onClick: () -> Unit,
) {
    val tokens = LocalOpenBrainTokens.current
    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outline
    val bg = when {
        isProcessing -> MaterialTheme.colorScheme.surface
        isCapturing -> primary
        else -> androidx.compose.ui.graphics.Color.Transparent
    }
    val border = when {
        isProcessing -> outline
        isCapturing -> primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val fg = when {
        isProcessing -> MaterialTheme.colorScheme.onSurfaceVariant
        isCapturing -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurface
    }
    Surface(
        modifier = Modifier
            .size(98.dp)
            .clip(CircleShape),
        shape = CircleShape,
        color = bg,
        border = BorderStroke(1.dp, border),
        onClick = { if (!isProcessing) onClick() },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label.lowercase(Locale.US),
                fontFamily = SakuraSans,
                fontWeight = FontWeight.Light,
                fontSize = 12.sp,
                letterSpacing = 1.2.sp,
                color = fg,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun SakuraStatusRow(
    mainLabel: String,
    subLabel: String,
    isCapturing: Boolean,
    isProcessing: Boolean,
) {
    val inkFaded = MaterialTheme.colorScheme.onSurfaceVariant
    val primary = MaterialTheme.colorScheme.primary
    val transition = rememberInfiniteTransition(label = "sakura-pulse")
    val dotAlpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "sakura-pulse-alpha",
    )
    val dotColor = when {
        isProcessing -> MaterialTheme.colorScheme.tertiary
        isCapturing -> primary
        else -> inkFaded
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(6.dp)
                    .graphicsLayer {
                        this.alpha = if (isCapturing || isProcessing) dotAlpha else 1f
                    }
                    .background(dotColor, CircleShape),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = mainLabel,
                fontFamily = SakuraSerif,
                fontWeight = FontWeight.Light,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = subLabel,
            fontFamily = SakuraSans,
            fontWeight = FontWeight.Light,
            fontSize = 10.sp,
            letterSpacing = 1.0.sp,
            color = inkFaded,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun Hairline(
    color: androidx.compose.ui.graphics.Color,
    vPadding: androidx.compose.ui.unit.Dp,
    hPadding: androidx.compose.ui.unit.Dp = 24.dp,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = hPadding, vertical = vPadding)
            .height(1.dp)
            .background(color),
    )
}

@Composable
private fun SectionLabel(
    text: String,
    kanji: String?,
    inkFaded: androidx.compose.ui.graphics.Color,
) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = text.uppercase(Locale.US),
            fontFamily = SakuraSans,
            fontWeight = FontWeight.Normal,
            fontSize = 10.sp,
            letterSpacing = 2.0.sp,
            color = inkFaded,
        )
        if (!kanji.isNullOrBlank()) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = kanji,
                fontFamily = SakuraSans,
                fontWeight = FontWeight.Light,
                fontSize = 11.sp,
                color = inkFaded,
            )
        }
    }
}

@Composable
private fun StatRow(
    num: String,
    label: String,
    ink: androidx.compose.ui.graphics.Color,
    inkFaded: androidx.compose.ui.graphics.Color,
) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = num,
            fontFamily = SakuraSans,
            fontWeight = FontWeight.ExtraLight,
            fontSize = 20.sp,
            color = ink,
            modifier = Modifier.width(40.dp),
        )
        Text(
            text = label,
            fontFamily = SakuraSans,
            fontWeight = FontWeight.Light,
            fontSize = 11.sp,
            letterSpacing = 0.5.sp,
            color = inkFaded,
        )
    }
}

private fun formatDuration(ms: Long): String {
    val t = ms / 1000
    val h = t / 3600
    val m = (t % 3600) / 60
    val s = t % 60
    return String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
}
