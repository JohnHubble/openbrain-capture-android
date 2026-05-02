package com.hubble.openbrain.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One source of truth for the current [CapturePhase] plus persistent "last saved" memory
 * that survives across sessions while the process is alive.
 */
data class CaptureState(
    val phase: CapturePhase = CapturePhase.Idle,
    val lastSavedTranscript: String? = null,
    val lastSavedAtMs: Long = 0L,
    val lastSavedDurationMs: Long = 0L,
)

@Singleton
class CaptureStateHolder @Inject constructor() {

    private val _state = MutableStateFlow(CaptureState())
    val state: StateFlow<CaptureState> = _state.asStateFlow()

    fun setPhase(phase: CapturePhase) {
        _state.update { it.copy(phase = phase) }
    }

    fun startRecording() {
        _state.update { it.copy(phase = CapturePhase.Recording(durationMs = 0L, nearLimit = false)) }
    }

    fun tickRecording(elapsedMs: Long, nearLimit: Boolean) {
        _state.update {
            val p = it.phase
            if (p is CapturePhase.Recording) it.copy(phase = p.copy(durationMs = elapsedMs, nearLimit = nearLimit))
            else it
        }
    }

    fun beginTranscribing() {
        _state.update { it.copy(phase = CapturePhase.Transcribing) }
    }

    fun showPreview(transcript: String, durationMs: Long) {
        _state.update { it.copy(phase = CapturePhase.Preview(transcript, durationMs)) }
    }

    fun beginSaving() {
        _state.update { it.copy(phase = CapturePhase.Saving) }
    }

    fun recordSaved(transcript: String, savedAtMs: Long, durationMs: Long) {
        _state.update {
            it.copy(
                phase = CapturePhase.Saved(transcript, savedAtMs, durationMs),
                lastSavedTranscript = transcript,
                lastSavedAtMs = savedAtMs,
                lastSavedDurationMs = durationMs,
            )
        }
    }

    fun goIdle() {
        _state.update { it.copy(phase = CapturePhase.Idle) }
    }

    fun recordError(message: String) {
        _state.update { it.copy(phase = CapturePhase.Error(message)) }
    }
}
