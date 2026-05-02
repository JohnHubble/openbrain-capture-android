package com.hubble.openbrain.ui.capture

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hubble.openbrain.data.prefs.SettingsStore
import com.hubble.openbrain.service.CapturePhase
import com.hubble.openbrain.service.CaptureService
import com.hubble.openbrain.service.CaptureStateHolder
import com.hubble.openbrain.service.isActive
import com.hubble.openbrain.service.isBusy
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class CaptureUiState(
    val phase: CapturePhase = CapturePhase.Idle,
    val durationMs: Long = 0L,
    val nearLimit: Boolean = false,
    val previewTranscript: String? = null,
    val lastSavedTranscript: String? = null,
    val lastSavedSecondsAgo: Long = 0L,
    val lastSavedDurationMs: Long = 0L,
    val errorMessage: String? = null,
    val modelLabel: String = "Whisper base",
)

@HiltViewModel
class CaptureViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    stateHolder: CaptureStateHolder,
    settingsStore: SettingsStore,
) : ViewModel() {

    val state: StateFlow<CaptureUiState> = combine(
        stateHolder.state,
        settingsStore.whisperModel,
    ) { s, model ->
        val phase = s.phase
        val durationMs = when (phase) {
            is CapturePhase.Recording -> phase.durationMs
            is CapturePhase.Preview -> phase.durationMs
            is CapturePhase.Saved -> phase.durationMs
            else -> 0L
        }
        val nearLimit = (phase as? CapturePhase.Recording)?.nearLimit ?: false
        val previewTranscript = (phase as? CapturePhase.Preview)?.transcript
        val errorMessage = (phase as? CapturePhase.Error)?.message
        val savedSecondsAgo = if (s.lastSavedAtMs > 0)
            (System.currentTimeMillis() - s.lastSavedAtMs) / 1000
        else 0L
        CaptureUiState(
            phase = phase,
            durationMs = durationMs,
            nearLimit = nearLimit,
            previewTranscript = previewTranscript,
            lastSavedTranscript = s.lastSavedTranscript,
            lastSavedSecondsAgo = savedSecondsAgo,
            lastSavedDurationMs = s.lastSavedDurationMs,
            errorMessage = errorMessage,
            modelLabel = "Whisper ${model.displayName.lowercase()}",
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, CaptureUiState())

    fun start() = CaptureService.start(context)
    fun stop() = CaptureService.stop(context)
    fun confirmSave() = CaptureService.confirmSave(context)
    fun discardPreview() = CaptureService.discard(context)

    fun toggleCapture() {
        val phase = state.value.phase
        if (phase.isBusy) return
        when (phase) {
            is CapturePhase.Recording -> stop()
            is CapturePhase.Preview -> Unit
            else -> if (!phase.isActive) start()
        }
    }
}
