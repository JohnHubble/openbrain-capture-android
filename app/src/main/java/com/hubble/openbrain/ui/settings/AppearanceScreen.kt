package com.hubble.openbrain.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hubble.openbrain.ui.theme.ThemeId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen(
    onBack: () -> Unit,
    viewModel: AppearanceViewModel = hiltViewModel(),
) {
    val current by viewModel.current.collectAsStateWithLifecycle()
    var comingSoonFor by remember { mutableStateOf<ThemeId?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Appearance") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner),
        ) {
            ThemeId.entries.forEach { themeId ->
                val isActive = themeId == current
                ListItem(
                    headlineContent = {
                        Text(
                            if (themeId.featured) "${themeId.displayName} · Featured"
                            else themeId.displayName,
                        )
                    },
                    supportingContent = {
                        Text(
                            when {
                                isActive -> "Active"
                                themeId.installed -> "Installed"
                                else -> "Coming soon"
                            },
                        )
                    },
                    trailingContent = if (isActive) {
                        { Icon(Icons.Filled.Check, contentDescription = "Active") }
                    } else null,
                    modifier = Modifier.clickable {
                        if (themeId.installed) {
                            viewModel.select(themeId)
                        } else {
                            comingSoonFor = themeId
                        }
                    },
                )
            }
        }
    }

    comingSoonFor?.let { theme ->
        AlertDialog(
            onDismissRequest = { comingSoonFor = null },
            confirmButton = {
                TextButton(onClick = { comingSoonFor = null }) { Text("OK") }
            },
            title = { Text("${theme.displayName} — coming soon") },
            text = { Text("This theme's bespoke layout isn't shipped yet. Material Default and Sakura Minimal are available today.") },
        )
    }
}
