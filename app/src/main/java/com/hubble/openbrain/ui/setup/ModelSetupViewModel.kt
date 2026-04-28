package com.hubble.openbrain.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hubble.openbrain.data.prefs.SettingsStore
import com.hubble.openbrain.data.prefs.WhisperModel
import com.hubble.openbrain.transcribe.ModelState
import com.hubble.openbrain.transcribe.WhisperModelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ModelSetupViewModel @Inject constructor(
    private val repository: WhisperModelRepository,
    private val settings: SettingsStore,
) : ViewModel() {

    val state: StateFlow<ModelState> = repository.state

    val selectedModel: StateFlow<WhisperModel> = settings.whisperModel
        .stateIn(viewModelScope, SharingStarted.Eagerly, WhisperModel.BASE)

    fun selectModel(model: WhisperModel) = viewModelScope.launch {
        settings.setWhisperModel(model)
        repository.refresh()
    }

    fun startDownload() = repository.startDownload()
    fun cancelDownload() = repository.cancelDownload()
}
