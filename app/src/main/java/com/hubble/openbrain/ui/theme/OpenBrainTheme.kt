package com.hubble.openbrain.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember

@Composable
fun OpenBrainTheme(
    themeId: ThemeId,
    content: @Composable () -> Unit,
) {
    val tokens = remember(themeId) { resolveTokens(themeId) }
    val strings = remember(themeId) { resolveStrings(themeId) }
    CompositionLocalProvider(
        LocalOpenBrainTokens provides tokens,
        LocalOpenBrainStrings provides strings,
        LocalOpenBrainThemeId provides themeId,
    ) {
        MaterialTheme(
            colorScheme = tokens.colors,
            typography = tokens.typography,
            shapes = tokens.shapes,
            content = content,
        )
    }
}
