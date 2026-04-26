package com.hubble.openbrain.ui.capture

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hubble.openbrain.data.prefs.SettingsStore
import com.hubble.openbrain.data.prefs.WhisperModel
import com.hubble.openbrain.service.CaptureService
import com.hubble.openbrain.service.CaptureStateHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class CaptureUiState(
    val isCapturing: Boolean = false,
    val isProcessing: Boolean = false,
    val durationMs: Long = 0L,
    val capturedCount: Int = 0,
    val sentCount: Int = 0,
    val queuedCount: Int = 0,
    val latestThought: String? = null,
    val latestThoughtSecondsAgo: Long = 0L,
    val vadLabel: String = "VAD: off",
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
        val latestSecondsAgo = if (s.latestThoughtAtMs > 0)
            (System.currentTimeMillis() - s.latestThoughtAtMs) / 1000
        else 0L
        val queued = (s.sessionCapturedCount - s.sessionSentCount).coerceAtLeast(0)
        CaptureUiState(
            isCapturing = s.isCapturing,
            isProcessing = s.isProcessing,
            durationMs = s.durationMs,
            capturedCount = s.sessionCapturedCount,
            sentCount = s.sessionSentCount,
            queuedCount = queued,
            latestThought = s.latestThought,
            latestThoughtSecondsAgo = latestSecondsAgo,
            modelLabel = "Whisper ${model.displayName.lowercase()}",
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, CaptureUiState())

    fun start() = CaptureService.start(context)
    fun stop() = CaptureService.stop(context)

    fun toggleCapture() {
        val s = state.value
        if (s.isProcessing) return
        if (s.isCapturing) stop() else start()
    }
}
