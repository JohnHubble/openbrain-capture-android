package com.hubble.openbrain.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/** Palette lifted from v3-obey.html — Soviet propaganda: red · cream · black · gold. */
internal val cm_red = Color(0xFFB31919)
internal val cm_redBright = Color(0xFFD41920)
internal val cm_cream = Color(0xFFF5E6C8)
internal val cm_creamDark = Color(0xFFD4C4A0)
internal val cm_black = Color(0xFF1A1A1A)
internal val cm_blackSoft = Color(0xFF2A2A2A)
internal val cm_gold = Color(0xFFC4A35A)

private val cm_success = Color(0xFF5C7A3F)
private val cm_successContainer = Color(0xFFE7E2C8)
private val cm_onSuccessContainer = Color(0xFF1F2B0F)
private val cm_warning = cm_gold
private val cm_warningContainer = Color(0xFFF2E2B8)
private val cm_onWarningContainer = Color(0xFF2E2307)

fun comradeNotesTokens(): OpenBrainTokens = OpenBrainTokens(
    colors = lightColorScheme(
        primary = cm_red,
        onPrimary = cm_cream,
        primaryContainer = cm_cream,
        onPrimaryContainer = cm_black,
        secondary = cm_gold,
        onSecondary = cm_black,
        secondaryContainer = Color(0xFFF2E2B8),
        onSecondaryContainer = cm_black,
        tertiary = cm_black,
        onTertiary = cm_cream,
        tertiaryContainer = cm_creamDark,
        onTertiaryContainer = cm_black,
        background = cm_cream,
        onBackground = cm_black,
        surface = cm_cream,
        onSurface = cm_black,
        surfaceVariant = cm_creamDark,
        onSurfaceVariant = cm_blackSoft,
        surfaceContainer = Color(0xFFEDDCB4),
        surfaceContainerHigh = Color(0xFFE4D4AC),
        surfaceContainerHighest = cm_creamDark,
        outline = cm_black,
        outlineVariant = cm_blackSoft,
        error = cm_red,
        onError = cm_cream,
        errorContainer = Color(0xFFF2C7C1),
        onErrorContainer = Color(0xFF3F0606),
    ),
    typography = Typography(
        displayLarge    = TextStyle(fontFamily = Anton, fontSize = 72.sp, letterSpacing = 0.04.em),
        displayMedium   = TextStyle(fontFamily = Anton, fontSize = 54.sp, letterSpacing = 0.04.em),
        displaySmall    = TextStyle(fontFamily = Anton, fontSize = 38.sp, letterSpacing = 0.04.em),
        headlineLarge   = TextStyle(fontFamily = Anton, fontSize = 40.sp, letterSpacing = 0.06.em),
        headlineMedium  = TextStyle(fontFamily = Anton, fontSize = 32.sp, letterSpacing = 0.08.em),
        headlineSmall   = TextStyle(fontFamily = Anton, fontSize = 24.sp, letterSpacing = 0.08.em),
        titleLarge      = TextStyle(fontFamily = Anton, fontSize = 22.sp, letterSpacing = 0.08.em),
        titleMedium     = TextStyle(fontFamily = Anton, fontSize = 18.sp, letterSpacing = 0.12.em),
        titleSmall      = TextStyle(fontFamily = Anton, fontSize = 14.sp, letterSpacing = 0.14.em),
        bodyLarge       = TextStyle(fontFamily = SpaceMono, fontSize = 16.sp),
        bodyMedium      = TextStyle(fontFamily = SpaceMono, fontSize = 14.sp),
        bodySmall       = TextStyle(fontFamily = SpaceMono, fontSize = 12.sp),
        labelLarge      = TextStyle(fontFamily = SpaceMono, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.08.em),
        labelMedium     = TextStyle(fontFamily = SpaceMono, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.1.em),
        labelSmall      = TextStyle(fontFamily = SpaceMono, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.14.em),
    ),
    shapes = Shapes(
        extraSmall = RoundedCornerShape(0.dp),
        small = RoundedCornerShape(0.dp),
        medium = RoundedCornerShape(2.dp),
        large = RoundedCornerShape(4.dp),
        extraLarge = RoundedCornerShape(6.dp),
    ),
    extras = OpenBrainExtras(
        success = cm_success,
        successContainer = cm_successContainer,
        onSuccessContainer = cm_onSuccessContainer,
        warning = cm_warning,
        warningContainer = cm_warningContainer,
        onWarningContainer = cm_onWarningContainer,
    ),
)
