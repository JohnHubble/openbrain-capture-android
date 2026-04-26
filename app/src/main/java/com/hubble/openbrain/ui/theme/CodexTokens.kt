package com.hubble.openbrain.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/** Palette lifted from v4-monk.html — medieval scriptorium: vellum · ultramarine · gold · vermilion. */
internal val cx_vellum = Color(0xFFF0E5C8)
internal val cx_vellumDark = Color(0xFFD9C9A2)
internal val cx_ultramarine = Color(0xFF1B3A6F)
internal val cx_gold = Color(0xFFC9A961)
internal val cx_goldBright = Color(0xFFE0BD6F)
internal val cx_vermilion = Color(0xFFC8442E)
internal val cx_forest = Color(0xFF2D4A22)
internal val cx_ink = Color(0xFF2A1810)
internal val cx_inkFaded = Color(0xFF4A3520)

private val cx_successContainer = Color(0xFFC6D3BE)
private val cx_onSuccessContainer = Color(0xFF132009)

fun illuminatedCodexTokens(): OpenBrainTokens = OpenBrainTokens(
    colors = lightColorScheme(
        primary = cx_ultramarine,
        onPrimary = cx_gold,
        primaryContainer = cx_vellum,
        onPrimaryContainer = cx_ultramarine,
        secondary = cx_gold,
        onSecondary = cx_ultramarine,
        secondaryContainer = Color(0xFFECDBB0),
        onSecondaryContainer = cx_ink,
        tertiary = cx_vermilion,
        onTertiary = cx_vellum,
        tertiaryContainer = Color(0xFFEFC6BC),
        onTertiaryContainer = Color(0xFF3E0E05),
        background = cx_vellum,
        onBackground = cx_ink,
        surface = cx_vellum,
        onSurface = cx_ink,
        surfaceVariant = cx_vellumDark,
        onSurfaceVariant = cx_inkFaded,
        surfaceContainer = Color(0xFFE6DBB8),
        surfaceContainerHigh = Color(0xFFDDD0A9),
        surfaceContainerHighest = cx_vellumDark,
        outline = cx_gold,
        outlineVariant = Color(0xFFD4BD84),
        error = cx_vermilion,
        onError = cx_vellum,
        errorContainer = Color(0xFFEFC6BC),
        onErrorContainer = Color(0xFF3E0E05),
    ),
    typography = Typography(
        displayLarge    = TextStyle(fontFamily = UnifrakturMaguntia, fontSize = 72.sp, letterSpacing = 0.03.em),
        displayMedium   = TextStyle(fontFamily = UnifrakturMaguntia, fontSize = 54.sp, letterSpacing = 0.03.em),
        displaySmall    = TextStyle(fontFamily = UnifrakturMaguntia, fontSize = 40.sp, letterSpacing = 0.03.em),
        headlineLarge   = TextStyle(fontFamily = UnifrakturMaguntia, fontSize = 36.sp),
        headlineMedium  = TextStyle(fontFamily = UnifrakturMaguntia, fontSize = 30.sp),
        headlineSmall   = TextStyle(fontFamily = Cinzel, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, letterSpacing = 0.14.em),
        titleLarge      = TextStyle(fontFamily = Cinzel, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, letterSpacing = 0.18.em),
        titleMedium     = TextStyle(fontFamily = Cinzel, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, letterSpacing = 0.22.em),
        titleSmall      = TextStyle(fontFamily = Cinzel, fontWeight = FontWeight.Normal, fontSize = 14.sp, letterSpacing = 0.28.em),
        bodyLarge       = TextStyle(fontFamily = CormorantGaramond, fontSize = 19.sp),
        bodyMedium      = TextStyle(fontFamily = CormorantGaramond, fontSize = 17.sp),
        bodySmall       = TextStyle(fontFamily = CormorantGaramond, fontStyle = FontStyle.Italic, fontSize = 15.sp),
        labelLarge      = TextStyle(fontFamily = Cinzel, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 0.2.em),
        labelMedium     = TextStyle(fontFamily = Cinzel, fontWeight = FontWeight.Normal, fontSize = 13.sp, letterSpacing = 0.24.em),
        labelSmall      = TextStyle(fontFamily = Cinzel, fontWeight = FontWeight.Normal, fontSize = 12.sp, letterSpacing = 0.28.em),
    ),
    shapes = Shapes(
        extraSmall = RoundedCornerShape(0.dp),
        small = RoundedCornerShape(2.dp),
        medium = RoundedCornerShape(3.dp),
        large = RoundedCornerShape(4.dp),
        extraLarge = RoundedCornerShape(6.dp),
    ),
    extras = OpenBrainExtras(
        success = cx_forest,
        successContainer = cx_successContainer,
        onSuccessContainer = cx_onSuccessContainer,
        warning = cx_goldBright,
        warningContainer = Color(0xFFF3E2B0),
        onWarningContainer = Color(0xFF2A1B04),
    ),
)
