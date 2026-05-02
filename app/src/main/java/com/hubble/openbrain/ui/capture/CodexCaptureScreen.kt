package com.hubble.openbrain.ui.capture

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hubble.openbrain.R
import com.hubble.openbrain.service.CapturePhase
import com.hubble.openbrain.service.isBusy
import com.hubble.openbrain.service.isRecording
import com.hubble.openbrain.ui.theme.Cinzel
import com.hubble.openbrain.ui.theme.CormorantGaramond
import com.hubble.openbrain.ui.theme.LocalOpenBrainStrings
import com.hubble.openbrain.ui.theme.UnifrakturMaguntia
import com.hubble.openbrain.ui.theme.cx_gold
import com.hubble.openbrain.ui.theme.cx_ink
import com.hubble.openbrain.ui.theme.cx_inkFaded
import com.hubble.openbrain.ui.theme.cx_ultramarine
import com.hubble.openbrain.ui.theme.cx_vellum
import com.hubble.openbrain.ui.theme.cx_vermilion
import java.util.Locale

/**
 * Illuminated Codex — 13th-century scriptorium.
 * Layout mirrors v8-codex.html (iteration of v4-monk):
 *   vellum page · ornament band + SCRIPTORIUM title · brain hero image with caption ·
 *   Latin state word · main+sub picker · circular seal · single duration counter ·
 *   ornament divider · VERBUM ULTIMUM with drop cap.
 */
@Composable
fun CodexCaptureScreen(
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
    val latinState = when (state.phase) {
        is CapturePhase.Recording -> "AUDIENS"
        CapturePhase.Transcribing -> "SCRIBENS"
        is CapturePhase.Preview -> "REVIDENS"
        CapturePhase.Saving -> "LIGANS"
        is CapturePhase.Saved -> "SERVATUM"
        is CapturePhase.Error -> "EXTINCTUM"
        else -> "QUIESCENS"
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(cx_vellum)
            .drawBehind {
                val brush1 = Brush.radialGradient(
                    colors = listOf(Color(0x228B5A2B), Color(0x00000000)),
                    center = Offset(size.width * 0.15f, size.height * 0.25f),
                    radius = size.width * 0.6f,
                )
                val brush2 = Brush.radialGradient(
                    colors = listOf(Color(0x1E785020), Color(0x00000000)),
                    center = Offset(size.width * 0.85f, size.height * 0.6f),
                    radius = size.width * 0.5f,
                )
                drawRect(brush = brush1)
                drawRect(brush = brush2)
            },
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 20.dp),
        ) {
            CodexStatusBar()
            OrnamentBand("SCRIPTORIUM")
            BrainHero()
            LatinStateWord(latinState)
            PickerStatus(main = text.main, sub = text.sub)
            Seal(buttonLabel, onClick = { if (!isBusy && !isPreview) onToggle() })
            if (state.nearLimit && isRecording) {
                NearLimitNotice(strings.nearLimit.main, strings.nearLimit.sub)
            }
            DurationCounter(formatDurationCodex(state.durationMs))
            if (isPreview) {
                PreviewSection(
                    transcript = state.previewTranscript ?: "",
                    saveLabel = strings.savePreview,
                    discardLabel = strings.discardPreview,
                    onSave = onSavePreview,
                    onDiscard = onDiscardPreview,
                )
            }
            OrnamentDivider()
            VerbumUltimum(strings.latestTitle, state.lastSavedTranscript)
        }
    }
}

@Composable
private fun CodexStatusBar() {
    Row(
        Modifier
            .fillMaxWidth()
            .background(cx_vellum)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "OB · CODEX",
            fontFamily = Cinzel,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            letterSpacing = 2.2.sp,
            color = cx_ink,
        )
        Text(
            text = "ANNO MMXXVI",
            fontFamily = Cinzel,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            letterSpacing = 1.8.sp,
            color = cx_vermilion,
        )
    }
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(cx_gold),
    )
}

@Composable
private fun OrnamentBand(title: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(cx_vellum)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val ornaments = listOf("❦", "✦", "❧", "✦", "❦", "✦", "❧", "✦", "❦", "✦", "❧", "✦", "❦")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ornaments.forEachIndexed { i, g ->
                Text(
                    text = g,
                    fontSize = 14.sp,
                    color = if (i % 2 == 0) cx_gold else cx_vermilion,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = title,
            fontFamily = Cinzel,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            letterSpacing = 3.6.sp,
            color = cx_ink,
        )
    }
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(cx_gold.copy(alpha = 0.3f)),
    )
}

@Composable
private fun BrainHero() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Image(
            painter = painterResource(R.drawable.codex_brain),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Et verbum factum est",
            fontFamily = CormorantGaramond,
            fontStyle = FontStyle.Italic,
            fontSize = 15.sp,
            color = cx_inkFaded,
            letterSpacing = 0.5.sp,
        )
    }
}

@Composable
private fun LatinStateWord(word: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(top = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = word,
            fontFamily = Cinzel,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            letterSpacing = 3.9.sp,
            color = cx_inkFaded,
        )
    }
}

@Composable
private fun PickerStatus(main: String, sub: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 0.dp)
            .padding(top = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = main,
            fontFamily = Cinzel,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            letterSpacing = 0.72.sp,
            color = cx_ultramarine,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = sub,
            fontFamily = CormorantGaramond,
            fontStyle = FontStyle.Italic,
            fontSize = 15.sp,
            color = cx_inkFaded,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun Seal(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .size(110.dp),
            shape = CircleShape,
            color = cx_ultramarine,
            border = BorderStroke(4.dp, cx_gold),
            onClick = onClick,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = label,
                    fontFamily = UnifrakturMaguntia,
                    fontSize = 24.sp,
                    letterSpacing = 1.4.sp,
                    color = cx_gold,
                    style = TextStyle(
                        shadow = Shadow(
                            color = cx_gold.copy(alpha = 0.5f),
                            offset = Offset(0f, 0f),
                            blurRadius = 6f,
                        ),
                    ),
                )
            }
        }
    }
}

@Composable
private fun NearLimitNotice(main: String, sub: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = main,
            fontFamily = Cinzel,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            letterSpacing = 1.6.sp,
            color = cx_vermilion,
            textAlign = TextAlign.Center,
        )
        Text(
            text = sub,
            fontFamily = CormorantGaramond,
            fontStyle = FontStyle.Italic,
            fontSize = 13.sp,
            color = cx_inkFaded,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun DurationCounter(duration: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .border(1.dp, cx_gold, RoundedCornerShape(3.dp))
            .background(cx_gold.copy(alpha = 0.05f), RoundedCornerShape(3.dp)),
        horizontalArrangement = Arrangement.Center,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "HORA",
                fontFamily = Cinzel,
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
                letterSpacing = 2.0.sp,
                color = cx_inkFaded,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = duration,
                fontFamily = CormorantGaramond,
                fontStyle = FontStyle.Italic,
                fontSize = 22.sp,
                color = cx_ink,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "tempus",
                fontFamily = CormorantGaramond,
                fontStyle = FontStyle.Italic,
                fontSize = 12.sp,
                color = cx_inkFaded.copy(alpha = 0.85f),
            )
        }
    }
}

@Composable
private fun PreviewSection(
    transcript: String,
    saveLabel: String,
    discardLabel: String,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .border(1.dp, cx_vermilion, RoundedCornerShape(3.dp))
            .background(cx_gold.copy(alpha = 0.04f), RoundedCornerShape(3.dp))
            .padding(14.dp),
    ) {
        Text(
            text = "REVISIO",
            fontFamily = Cinzel,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            letterSpacing = 3.0.sp,
            color = cx_vermilion,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = transcript,
            fontFamily = CormorantGaramond,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            color = cx_ink,
        )
        Spacer(Modifier.height(14.dp))
        Row {
            CodexButton(label = saveLabel, primary = true, onClick = onSave)
            Spacer(Modifier.width(12.dp))
            CodexButton(label = discardLabel, primary = false, onClick = onDiscard)
        }
    }
}

@Composable
private fun CodexButton(label: String, primary: Boolean, onClick: () -> Unit) {
    val bg = if (primary) cx_ultramarine else Color.Transparent
    val fg = if (primary) cx_gold else cx_ultramarine
    Surface(
        onClick = onClick,
        color = bg,
        shape = RoundedCornerShape(2.dp),
        border = BorderStroke(1.dp, cx_ultramarine),
    ) {
        Text(
            text = label,
            fontFamily = Cinzel,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            letterSpacing = 2.0.sp,
            color = fg,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun OrnamentDivider() {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        val glyphs = listOf("❦", "◆", "❦", "◆", "❦", "◆", "❦")
        glyphs.forEachIndexed { i, g ->
            Text(
                text = g,
                fontSize = 10.sp,
                color = if (i % 2 == 0) cx_gold else cx_vermilion,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}

@Composable
private fun VerbumUltimum(pickerTitle: String, body: String?) {
    Column(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 6.dp),
        ) {
            Text("❦", color = cx_gold, fontSize = 14.sp)
            Spacer(Modifier.width(6.dp))
            Text(
                text = "VERBUM ULTIMUM",
                fontFamily = Cinzel,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                letterSpacing = 2.4.sp,
                color = cx_ink,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "· $pickerTitle",
                fontFamily = CormorantGaramond,
                fontStyle = FontStyle.Italic,
                fontSize = 14.sp,
                color = cx_inkFaded,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "· · · · · ·",
                fontSize = 11.sp,
                letterSpacing = 2.sp,
                color = cx_gold,
                modifier = Modifier.weight(1f, fill = false),
            )
            Spacer(Modifier.width(6.dp))
            Text("❦", color = cx_gold, fontSize = 14.sp)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = cx_gold.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(topEnd = 2.dp, bottomEnd = 2.dp),
                )
                .drawBehind {
                    drawRect(
                        color = cx_gold,
                        topLeft = Offset(0f, 0f),
                        size = Size(2.dp.toPx(), size.height),
                    )
                }
                .padding(14.dp),
        ) {
            val dropChar = firstLetter(body)
            Text(
                text = dropChar.toString(),
                fontFamily = UnifrakturMaguntia,
                fontSize = 44.sp,
                color = cx_vermilion,
                style = TextStyle(
                    shadow = Shadow(color = cx_gold, offset = Offset(1f, 1f), blurRadius = 0f),
                ),
                modifier = Modifier.padding(end = 6.dp, top = 2.dp),
            )
            Text(
                text = body?.let { if (it.isNotEmpty()) it.substring(1) else "" } ?: "ilentium — the codex is yet silent.",
                fontFamily = CormorantGaramond,
                fontSize = 17.sp,
                lineHeight = 26.sp,
                color = cx_ink,
                fontStyle = if (body == null) FontStyle.Italic else FontStyle.Normal,
            )
        }
    }
}

private fun firstLetter(s: String?): Char {
    val trimmed = s?.trimStart() ?: return 'S'
    return trimmed.firstOrNull()?.uppercaseChar() ?: 'S'
}

private fun formatDurationCodex(ms: Long): String {
    val t = ms / 1000
    val h = t / 3600
    val m = (t % 3600) / 60
    val s = t % 60
    return String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
}
