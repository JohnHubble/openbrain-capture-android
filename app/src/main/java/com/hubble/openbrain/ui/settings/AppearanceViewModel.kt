package com.hubble.openbrain.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hubble.openbrain.data.prefs.SettingsStore
import com.hubble.openbrain.ui.theme.ThemeId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppearanceViewModel @Inject constructor(
    private val settings: SettingsStore,
) : ViewModel() {

    val current: StateFlow<ThemeId> = settings.themeId
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeId.MaterialDefault)

    fun select(themeId: ThemeId) {
        if (!themeId.installed) return
        viewModelScope.launch { settings.setThemeId(themeId) }
    }
}
