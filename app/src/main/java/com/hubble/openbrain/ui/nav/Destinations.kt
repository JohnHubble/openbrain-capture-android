package com.hubble.openbrain.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class Destination(val route: String, val label: String, val icon: ImageVector) {
    Capture(route = "capture", label = "Capture", icon = Icons.Outlined.Mic),
    History(route = "history", label = "History", icon = Icons.Outlined.History),
    Settings(route = "settings", label = "Settings", icon = Icons.Outlined.Settings);

    companion object {
        val bottomBar: List<Destination> = listOf(Capture, History, Settings)
        const val APPEARANCE_ROUTE = "appearance"
    }
}
