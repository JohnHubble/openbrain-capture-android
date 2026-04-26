package com.hubble.openbrain.ui.setup

import androidx.lifecycle.ViewModel
import com.hubble.openbrain.transcribe.ModelState
import com.hubble.openbrain.transcribe.WhisperModelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class ModelSetupViewModel @Inject constructor(
    private val repository: WhisperModelRepository,
) : ViewModel() {

    val state: StateFlow<ModelState> = repository.state

    fun startDownload() = repository.startDownload()
    fun cancelDownload() = repository.cancelDownload()
}
