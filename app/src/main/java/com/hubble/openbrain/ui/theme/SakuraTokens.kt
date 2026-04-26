package com.hubble.openbrain.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val sk_primary = Color(0xFFE08CA1)
private val sk_onPrimary = Color(0xFFFFFFFF)
private val sk_primaryContainer = Color(0xFFFBE4EA)
private val sk_onPrimaryContainer = Color(0xFF3A1620)
private val sk_secondary = Color(0xFF8C6771)
private val sk_onSecondary = Color(0xFFFFFFFF)
private val sk_secondaryContainer = Color(0xFFF5DDE3)
private val sk_onSecondaryContainer = Color(0xFF2E151B)
private val sk_background = Color(0xFFFDFCFA)
private val sk_onBackground = Color(0xFF1A1A1A)
private val sk_surface = Color(0xFFFDFCFA)
private val sk_onSurface = Color(0xFF1A1A1A)
private val sk_surfaceVariant = Color(0xFFF4F2EE)
private val sk_onSurfaceVariant = Color(0xFF555555)
private val sk_surfaceContainer = Color(0xFFF7F5F1)
private val sk_surfaceContainerHigh = Color(0xFFF1EEE9)
private val sk_surfaceContainerHighest = Color(0xFFEBE8E2)
private val sk_outline = Color(0xFFD6D3CD)
private val sk_outlineVariant = Color(0xFFE6E3DD)
private val sk_error = Color(0xFFB3261E)
private val sk_onError = Color(0xFFFFFFFF)
private val sk_errorContainer = Color(0xFFF9DEDC)
private val sk_onErrorContainer = Color(0xFF410E0B)
private val sk_success = Color(0xFF527A59)
private val sk_successContainer = Color(0xFFDAE8DC)
private val sk_onSuccessContainer = Color(0xFF152418)
private val sk_warning = Color(0xFFB47539)
private val sk_warningContainer = Color(0xFFF2E2CF)
private val sk_onWarningContainer = Color(0xFF2C1B05)

fun sakuraMinimalTokens(): OpenBrainTokens = OpenBrainTokens(
    colors = lightColorScheme(
        primary = sk_primary,
        onPrimary = sk_onPrimary,
        primaryContainer = sk_primaryContainer,
        onPrimaryContainer = sk_onPrimaryContainer,
        secondary = sk_secondary,
        onSecondary = sk_onSecondary,
        secondaryContainer = sk_secondaryContainer,
        onSecondaryContainer = sk_onSecondaryContainer,
        background = sk_background,
        onBackground = sk_onBackground,
        surface = sk_surface,
        onSurface = sk_onSurface,
        surfaceVariant = sk_surfaceVariant,
        onSurfaceVariant = sk_onSurfaceVariant,
        surfaceContainer = sk_surfaceContainer,
        surfaceContainerHigh = sk_surfaceContainerHigh,
        surfaceContainerHighest = sk_surfaceContainerHighest,
        outline = sk_outline,
        outlineVariant = sk_outlineVariant,
        error = sk_error,
        onError = sk_onError,
        errorContainer = sk_errorContainer,
        onErrorContainer = sk_onErrorContainer,
    ),
    typography = Typography(),
    shapes = Shapes(
        extraSmall = RoundedCornerShape(0.dp),
        small = RoundedCornerShape(2.dp),
        medium = RoundedCornerShape(4.dp),
        large = RoundedCornerShape(6.dp),
        extraLarge = RoundedCornerShape(12.dp),
    ),
    extras = OpenBrainExtras(
        success = sk_success,
        successContainer = sk_successContainer,
        onSuccessContainer = sk_onSuccessContainer,
        warning = sk_warning,
        warningContainer = sk_warningContainer,
        onWarningContainer = sk_onWarningContainer,
    ),
)
