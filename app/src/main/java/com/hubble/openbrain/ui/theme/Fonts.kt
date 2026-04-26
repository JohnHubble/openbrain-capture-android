package com.hubble.openbrain.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.hubble.openbrain.R

/**
 * Centralised font families — each theme picks its own palette of voices from here.
 * TTF files live in res/font and were downloaded from Google Fonts.
 *
 * Japanese (for Sakura) and Chinese/Korean characters fall back to the system CJK font;
 * we ship only Latin glyphs for app-custom display fonts to keep the APK lean.
 */

val Anton = FontFamily(
    Font(R.font.anton_regular, weight = FontWeight.Normal),
)

val SpaceMono = FontFamily(
    Font(R.font.space_mono_regular, weight = FontWeight.Normal),
    Font(R.font.space_mono_bold, weight = FontWeight.Bold),
)

/**
 * Cinzel and CormorantGaramond ship as variable fonts (single TTF with a wght axis).
 * Providing the same file for multiple declared weights lets Compose pick the right
 * weight at render time on Android 8+.
 */
val Cinzel = FontFamily(
    Font(R.font.cinzel_variable, weight = FontWeight.Normal),
    Font(R.font.cinzel_variable, weight = FontWeight.Medium),
    Font(R.font.cinzel_variable, weight = FontWeight.SemiBold),
    Font(R.font.cinzel_variable, weight = FontWeight.Bold),
)

val CormorantGaramond = FontFamily(
    Font(R.font.cormorant_garamond_variable, weight = FontWeight.Normal),
    Font(R.font.cormorant_garamond_variable, weight = FontWeight.Medium),
    Font(R.font.cormorant_garamond_variable, weight = FontWeight.SemiBold),
    Font(R.font.cormorant_garamond_italic_variable, weight = FontWeight.Normal, style = FontStyle.Italic),
    Font(R.font.cormorant_garamond_italic_variable, weight = FontWeight.SemiBold, style = FontStyle.Italic),
)

val UnifrakturMaguntia = FontFamily(
    Font(R.font.unifraktur_maguntia_regular, weight = FontWeight.Normal),
)

/** Sakura falls back to system Serif — Android's Noto Serif CJK covers Japanese glyphs. */
val SakuraSerif: FontFamily = FontFamily.Serif
val SakuraSans: FontFamily = FontFamily.SansSerif
