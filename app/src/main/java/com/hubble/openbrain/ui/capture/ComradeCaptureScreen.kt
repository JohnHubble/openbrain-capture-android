package com.hubble.openbrain.ui.capture

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hubble.openbrain.R
import com.hubble.openbrain.service.CapturePhase
import com.hubble.openbrain.service.isBusy
import com.hubble.openbrain.service.isRecording
import com.hubble.openbrain.ui.theme.Anton
import com.hubble.openbrain.ui.theme.LocalOpenBrainStrings
import com.hubble.openbrain.ui.theme.SpaceMono
import com.hubble.openbrain.ui.theme.cm_black
import com.hubble.openbrain.ui.theme.cm_cream
import com.hubble.openbrain.ui.theme.cm_creamDark
import com.hubble.openbrain.ui.theme.cm_gold
import com.hubble.openbrain.ui.theme.cm_red
import java.util.Locale

/**
 * Comrade Notes — Soviet propaganda / Shepard Fairey.
 * Layout mirrors v7-comrade.html (iteration of v3-obey):
 *   red status bar ★ OB ★ · THE DISPATCH masthead · hero brain image · red ribbon ·
 *   starburst + round red toggle · sub-label · single duration banner · LAST DISPATCH.
 */
@Composable
fun ComradeCaptureScreen(
    state: CaptureUiState,
    onToggle: () -> Unit,
    onSavePreview: () -> Unit,
    onDiscardPreview: () -> Unit,
) {
    val strings = LocalOpenBrainStrings.current
    val isRecording = state.phase.isRecording
    val isBusy = state.phase.isBusy
    val isPreview = state.phase is CapturePhase.Preview

    val text = phaseLifecycleText(state.phase, state.nearLimit, strings)
    val buttonLabel = phaseButtonLabel(state.phase, strings)

    Box(
        Modifier
            .fillMaxSize()
            .background(cm_cream),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
        ) {
            StatusBanner(subtitle = strings.pageSubtitle)
            DispatchMasthead()
            HeroImage()
            Ribbon(text.main.uppercase(Locale.US))
            ToggleArea(
                label = buttonLabel,
                isCapturing = isRecording,
                isProcessing = isBusy || isPreview,
                onToggle = { if (!isPreview) onToggle() },
            )
            SubLabelSlot(text.sub.uppercase(Locale.US))
            if (state.nearLimit && isRecording) {
                NearLimitBanner(strings.nearLimit.main, strings.nearLimit.sub)
            }
            DurationBanner(formatDurationComrade(state.durationMs))
            if (isPreview) {
                PreviewBlock(
                    transcript = state.previewTranscript ?: "",
                    saveLabel = strings.savePreview,
                    discardLabel = strings.discardPreview,
                    onSave = onSavePreview,
                    onDiscard = onDiscardPreview,
                )
            }
            InterceptBlock(strings.latestTitle, state.lastSavedTranscript)
        }

        HalftoneOverlay()
    }
}

@Composable
private fun StatusBanner(subtitle: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(cm_red)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "★ OB ★",
            fontFamily = Anton,
            fontSize = 13.sp,
            letterSpacing = 1.3.sp,
            color = cm_cream,
        )
        Text(
            text = subtitle.uppercase(Locale.US),
            fontFamily = SpaceMono,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp,
            letterSpacing = 1.4.sp,
            color = cm_cream,
        )
    }
}

@Composable
private fun DispatchMasthead() {
    Column(
        Modifier
            .fillMaxWidth()
            .background(cm_cream),
    ) {
        HazardStripes()
        Column(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "THE DISPATCH",
                fontFamily = Anton,
                fontSize = 30.sp,
                letterSpacing = 3.6.sp,
                color = cm_black,
                style = TextStyle(
                    shadow = Shadow(color = cm_red, offset = Offset(3f, 3f), blurRadius = 0f),
                ),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "★ PROPERTY OF THE COLLECTIVE ★",
                fontFamily = SpaceMono,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                letterSpacing = 1.8.sp,
                color = cm_red,
            )
        }
        HazardStripes()
    }
}

@Composable
private fun HazardStripes() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(6.dp)
            .drawBehind {
                val stripeWidth = 8.dp.toPx()
                val totalWidth = size.width + stripeWidth
                var x = 0f
                var toggle = true
                val angle = 0.6f
                while (x < totalWidth) {
                    val color = if (toggle) cm_black else cm_cream
                    val topLeftX = x - size.height * angle
                    drawRect(
                        color = color,
                        topLeft = Offset(topLeftX, 0f),
                        size = Size(stripeWidth + size.height * angle, size.height),
                    )
                    x += stripeWidth
                    toggle = !toggle
                }
            },
    )
}

@Composable
private fun HeroImage() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(240.dp)
            .background(cm_red),
    ) {
        Image(
            painter = painterResource(R.drawable.comrade_brain),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = androidx.compose.ui.BiasAlignment(0f, -0.5f),
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier
                .fillMaxSize()
                .drawBehind {
                    val step = 4.dp.toPx()
                    val r = 0.8.dp.toPx()
                    var y = 0f
                    while (y < size.height) {
                        var x = 0f
                        while (x < size.width) {
                            drawCircle(
                                color = Color(0x38000000),
                                radius = r,
                                center = Offset(x, y),
                            )
                            x += step
                        }
                        y += step
                    }
                },
        )
    }
}

@Composable
private fun Ribbon(text: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp)
            .graphicsLayer { translationY = -4f }
            .background(cm_red),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 26.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                fontFamily = Anton,
                fontSize = 17.sp,
                letterSpacing = 3.4.sp,
                color = cm_cream,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ToggleArea(
    label: String,
    isCapturing: Boolean,
    isProcessing: Boolean,
    onToggle: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(top = 40.dp, bottom = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        val transition = rememberInfiniteTransition(label = "comrade-starburst")
        val rotation by transition.animateFloat(
            initialValue = 0f, targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(20_000, easing = LinearEasing)),
            label = "starburst-spin",
        )
        Box(
            Modifier
                .size(144.dp)
                .graphicsLayer { rotationZ = rotation }
                .drawBehind {
                    val center = Offset(size.width / 2, size.height / 2)
                    val radius = size.minDimension / 2
                    val rays = 30
                    for (i in 0 until rays) {
                        val angle = i * (360f / rays)
                        val rad = Math.toRadians(angle.toDouble())
                        val start = Offset(
                            x = center.x + (radius * 0.48f) * kotlin.math.cos(rad).toFloat(),
                            y = center.y + (radius * 0.48f) * kotlin.math.sin(rad).toFloat(),
                        )
                        val end = Offset(
                            x = center.x + radius * kotlin.math.cos(rad).toFloat(),
                            y = center.y + radius * kotlin.math.sin(rad).toFloat(),
                        )
                        drawLine(
                            color = cm_cream.copy(alpha = 0.18f),
                            start = start,
                            end = end,
                            strokeWidth = 3.dp.toPx(),
                        )
                    }
                },
        )
        Surface(
            modifier = Modifier.size(100.dp),
            shape = CircleShape,
            color = if (isProcessing) cm_gold else cm_red,
            border = BorderStroke(4.dp, cm_cream),
            onClick = { if (!isProcessing) onToggle() },
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = label.uppercase(Locale.US),
                    fontFamily = Anton,
                    fontSize = 18.sp,
                    letterSpacing = 1.44.sp,
                    color = if (isProcessing) cm_black else cm_cream,
                    style = TextStyle(
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.4f),
                            offset = Offset(1f, 1f),
                        ),
                    ),
                )
            }
        }
    }
}

@Composable
private fun SubLabelSlot(text: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 0.dp, bottom = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontFamily = SpaceMono,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 2.2.sp,
            color = cm_red,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun NearLimitBanner(main: String, sub: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(cm_gold)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(
            text = main.uppercase(Locale.US),
            fontFamily = Anton,
            fontSize = 14.sp,
            letterSpacing = 2.0.sp,
            color = cm_black,
        )
        Text(
            text = sub.uppercase(Locale.US),
            fontFamily = SpaceMono,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp,
            letterSpacing = 1.4.sp,
            color = cm_black,
        )
    }
}

@Composable
private fun DurationBanner(duration: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(cm_red)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = duration,
            fontFamily = Anton,
            fontSize = 26.sp,
            letterSpacing = 1.0.sp,
            color = cm_cream,
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = "ELAPSED",
            fontFamily = SpaceMono,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            letterSpacing = 2.0.sp,
            color = cm_cream.copy(alpha = 0.82f),
            modifier = Modifier.padding(bottom = 4.dp),
        )
    }
}

@Composable
private fun PreviewBlock(
    transcript: String,
    saveLabel: String,
    discardLabel: String,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
        Text(
            text = "PREVIEW · AWAITING ORDERS",
            fontFamily = Anton,
            fontSize = 13.sp,
            letterSpacing = 2.6.sp,
            color = cm_red,
        )
        Spacer(Modifier.height(8.dp))
        Row {
            Text(
                text = "//",
                fontFamily = SpaceMono,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = cm_red,
                modifier = Modifier.padding(end = 6.dp),
            )
            Text(
                text = transcript,
                fontFamily = SpaceMono,
                fontSize = 12.sp,
                lineHeight = 20.sp,
                color = cm_creamDark,
            )
        }
        Spacer(Modifier.height(14.dp))
        Row {
            ComradeButton(label = saveLabel, primary = true, onClick = onSave)
            Spacer(Modifier.width(10.dp))
            ComradeButton(label = discardLabel, primary = false, onClick = onDiscard)
        }
    }
}

@Composable
private fun ComradeButton(label: String, primary: Boolean, onClick: () -> Unit) {
    val bg = if (primary) cm_red else cm_cream
    val fg = if (primary) cm_cream else cm_red
    val border = if (primary) cm_red else cm_red
    Surface(
        onClick = onClick,
        color = bg,
        shape = RoundedCornerShape(0.dp),
        border = BorderStroke(2.dp, border),
    ) {
        Text(
            text = label.uppercase(Locale.US),
            fontFamily = Anton,
            fontSize = 14.sp,
            letterSpacing = 2.0.sp,
            color = fg,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun InterceptBlock(title: String, body: String?) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(
            text = title.uppercase(Locale.US),
            fontFamily = Anton,
            fontSize = 12.sp,
            letterSpacing = 3.6.sp,
            color = cm_cream,
        )
        Spacer(Modifier.height(6.dp))
        Row {
            Text(
                text = "//",
                fontFamily = SpaceMono,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = cm_red,
                modifier = Modifier.padding(end = 6.dp),
            )
            Text(
                text = body ?: "AWAITING SIGNAL",
                fontFamily = SpaceMono,
                fontSize = 12.sp,
                lineHeight = 20.sp,
                color = cm_creamDark,
            )
        }
    }
}

@Composable
private fun HalftoneOverlay() {
    Box(
        Modifier
            .fillMaxSize()
            .drawBehind {
                val step = 3.dp.toPx()
                val r = 0.6.dp.toPx()
                var y = 0f
                while (y < size.height) {
                    var x = 0f
                    while (x < size.width) {
                        drawCircle(
                            color = Color(0x22000000),
                            radius = r,
                            center = Offset(x, y),
                        )
                        x += step
                    }
                    y += step
                }
            }
            .graphicsLayer { alpha = 0.45f },
    )
}

private fun formatDurationComrade(ms: Long): String {
    val t = ms / 1000
    val h = t / 3600
    val m = (t % 3600) / 60
    val s = t % 60
    return String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
}
