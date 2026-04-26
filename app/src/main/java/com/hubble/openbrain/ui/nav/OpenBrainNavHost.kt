package com.hubble.openbrain.ui.nav

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hubble.openbrain.ui.capture.CaptureScreen
import com.hubble.openbrain.ui.history.HistoryScreen
import com.hubble.openbrain.ui.settings.AppearanceScreen
import com.hubble.openbrain.ui.settings.SettingsScreen
import com.hubble.openbrain.ui.theme.LocalOpenBrainStrings
import com.hubble.openbrain.ui.theme.NavLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenBrainNavHost() {
    val navController = rememberNavController()
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route
    val showAppBar = currentRoute in Destination.bottomBar.map { it.route }
    val strings = LocalOpenBrainStrings.current

    Scaffold(
        topBar = {
            if (showAppBar) {
                TopAppBar(
                    title = { Text(strings.appTitle) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
            }
        },
        bottomBar = {
            if (showAppBar) {
                NavigationBar {
                    Destination.bottomBar.forEach { destination ->
                        val selected = currentEntry?.destination?.hierarchy?.any {
                            it.route == destination.route
                        } == true
                        val navLabel = when (destination) {
                            Destination.Capture -> strings.captureTab
                            Destination.History -> strings.historyTab
                            Destination.Settings -> strings.settingsTab
                        }
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(destination.route) {
                                    launchSingleTop = true
                                    restoreState = true
                                    popUpTo(Destination.Capture.route) { saveState = true }
                                }
                            },
                            icon = { Icon(destination.icon, contentDescription = navLabel.label) },
                            label = { NavLabelText(navLabel) },
                        )
                    }
                }
            }
        },
    ) { inner ->
        NavHost(
            navController = navController,
            startDestination = Destination.Capture.route,
            modifier = Modifier.padding(inner),
        ) {
            composable(Destination.Capture.route) { CaptureScreen() }
            composable(Destination.History.route) { HistoryScreen() }
            composable(Destination.Settings.route) {
                SettingsScreen(onOpenAppearance = { navController.navigate(Destination.APPEARANCE_ROUTE) })
            }
            composable(Destination.APPEARANCE_ROUTE) {
                AppearanceScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

@Composable
private fun NavLabelText(navLabel: NavLabel) {
    if (navLabel.ornament.isBlank()) {
        Text(navLabel.label)
    } else {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(navLabel.label)
            Spacer(Modifier.width(4.dp))
            Text(navLabel.ornament, style = MaterialTheme.typography.labelSmall)
        }
    }
}
