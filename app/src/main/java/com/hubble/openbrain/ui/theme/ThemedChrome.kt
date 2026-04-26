package com.hubble.openbrain.ui.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Theme-aware page title for History / Settings. Reads the theme's tab vocabulary from
 * [ThemeStrings] so each theme labels its own pages in its own voice — "memory 記",
 * "ARCHIVE", "Codex", or "History" — without each screen hardcoding strings.
 */
@Composable
fun ThemedPageTitle(tab: NavLabel, subtitle: String? = null) {
    val themeId = LocalOpenBrainThemeId.current
    Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = if (themeId == ThemeId.SakuraMinimal) tab.label else tab.label.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = if (themeId == ThemeId.ComradeNotes) FontWeight.Black else FontWeight.W500,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (tab.ornament.isNotBlank()) {
                Spacer(Modifier.width(10.dp))
                Text(
                    text = tab.ornament,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/**
 * Theme-aware section header with appropriate ornamentation:
 *   · Sakura — lowercase + hairline feel (inherits)
 *   · Comrade — UPPERCASE flanked by ★
 *   · Codex — SMALL CAPS flanked by ✦
 *   · Material — plain colored label (current behavior)
 */
@Composable
fun ThemedSectionHeader(title: String, danger: Boolean = false) {
    val themeId = LocalOpenBrainThemeId.current
    val color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val decorated = when (themeId) {
        ThemeId.SakuraMinimal -> title.lowercase()
        ThemeId.ComradeNotes -> "★  ${title.uppercase()}  ★"
        ThemeId.IlluminatedCodex -> "✦  ${title.uppercase()}  ✦"
        ThemeId.MaterialDefault -> title
    }
    Text(
        text = decorated,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 4.dp),
        style = MaterialTheme.typography.labelMedium.copy(
            letterSpacing = when (themeId) {
                ThemeId.ComradeNotes, ThemeId.IlluminatedCodex -> 2.sp
                else -> 0.sp
            },
        ),
        fontWeight = FontWeight.W500,
        color = color,
    )
}
