package com.hubble.openbrain.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

enum class ThemeId(val displayName: String, val installed: Boolean, val featured: Boolean = false) {
    MaterialDefault("Material Default", installed = true),
    SakuraMinimal("Sakura Minimal", installed = true),
    IlluminatedCodex("Illuminated Codex", installed = true, featured = true),
    ComradeNotes("Comrade Notes", installed = true);
}

data class OpenBrainExtras(
    val success: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
)

data class OpenBrainTokens(
    val colors: ColorScheme,
    val typography: Typography,
    val shapes: Shapes,
    val extras: OpenBrainExtras,
)

val LocalOpenBrainTokens = staticCompositionLocalOf<OpenBrainTokens> {
    error("OpenBrainTokens not provided")
}

fun resolveTokens(themeId: ThemeId): OpenBrainTokens = when (themeId) {
    ThemeId.MaterialDefault -> materialDefaultTokens()
    ThemeId.SakuraMinimal -> sakuraMinimalTokens()
    ThemeId.IlluminatedCodex -> illuminatedCodexTokens()
    ThemeId.ComradeNotes -> comradeNotesTokens()
}
