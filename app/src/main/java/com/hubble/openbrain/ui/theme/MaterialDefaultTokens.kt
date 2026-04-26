package com.hubble.openbrain.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val md_primary = Color(0xFF6750A4)
private val md_onPrimary = Color(0xFFFFFFFF)
private val md_primaryContainer = Color(0xFFEADDFF)
private val md_onPrimaryContainer = Color(0xFF21005D)
private val md_secondary = Color(0xFF625B71)
private val md_onSecondary = Color(0xFFFFFFFF)
private val md_secondaryContainer = Color(0xFFE8DEF8)
private val md_onSecondaryContainer = Color(0xFF1D192B)
private val md_background = Color(0xFFFEF7FF)
private val md_onBackground = Color(0xFF1D1B20)
private val md_surface = Color(0xFFFEF7FF)
private val md_onSurface = Color(0xFF1D1B20)
private val md_surfaceVariant = Color(0xFFE7E0EC)
private val md_onSurfaceVariant = Color(0xFF49454F)
private val md_surfaceContainer = Color(0xFFF2EDF7)
private val md_surfaceContainerHigh = Color(0xFFECE6F0)
private val md_surfaceContainerHighest = Color(0xFFE6E0EA)
private val md_outline = Color(0xFF79747E)
private val md_outlineVariant = Color(0xFFCAC4D0)
private val md_error = Color(0xFFBA1A1A)
private val md_onError = Color(0xFFFFFFFF)
private val md_errorContainer = Color(0xFFFFDAD6)
private val md_onErrorContainer = Color(0xFF410002)
private val md_success = Color(0xFF2E7D32)
private val md_successContainer = Color(0xFFC8E6C9)
private val md_onSuccessContainer = Color(0xFF002106)
private val md_warning = Color(0xFFF57C00)
private val md_warningContainer = Color(0xFFFFE0B2)
private val md_onWarningContainer = Color(0xFF3A1E00)

fun materialDefaultTokens(): OpenBrainTokens = OpenBrainTokens(
    colors = lightColorScheme(
        primary = md_primary,
        onPrimary = md_onPrimary,
        primaryContainer = md_primaryContainer,
        onPrimaryContainer = md_onPrimaryContainer,
        secondary = md_secondary,
        onSecondary = md_onSecondary,
        secondaryContainer = md_secondaryContainer,
        onSecondaryContainer = md_onSecondaryContainer,
        background = md_background,
        onBackground = md_onBackground,
        surface = md_surface,
        onSurface = md_onSurface,
        surfaceVariant = md_surfaceVariant,
        onSurfaceVariant = md_onSurfaceVariant,
        surfaceContainer = md_surfaceContainer,
        surfaceContainerHigh = md_surfaceContainerHigh,
        surfaceContainerHighest = md_surfaceContainerHighest,
        outline = md_outline,
        outlineVariant = md_outlineVariant,
        error = md_error,
        onError = md_onError,
        errorContainer = md_errorContainer,
        onErrorContainer = md_onErrorContainer,
    ),
    typography = Typography(),
    shapes = Shapes(
        extraSmall = RoundedCornerShape(4.dp),
        small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(12.dp),
        large = RoundedCornerShape(16.dp),
        extraLarge = RoundedCornerShape(28.dp),
    ),
    extras = OpenBrainExtras(
        success = md_success,
        successContainer = md_successContainer,
        onSuccessContainer = md_onSuccessContainer,
        warning = md_warning,
        warningContainer = md_warningContainer,
        onWarningContainer = md_onWarningContainer,
    ),
)
