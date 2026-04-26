package com.hubble.openbrain.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

data class CaptureState(
    val isCapturing: Boolean = false,
    val isProcessing: Boolean = false,
    val sessionStartedAtMs: Long = 0L,
    val durationMs: Long = 0L,
    val sessionCapturedCount: Int = 0,
    val sessionSentCount: Int = 0,
    val latestThought: String? = null,
    val latestThoughtAtMs: Long = 0L,
    val lastError: String? = null,
)

@Singleton
class CaptureStateHolder @Inject constructor() {

    private val _state = MutableStateFlow(CaptureState())
    val state: StateFlow<CaptureState> = _state.asStateFlow()

    fun startSession(startedAtMs: Long) {
        _state.update {
            CaptureState(
                isCapturing = true,
                sessionStartedAtMs = startedAtMs,
                durationMs = 0L,
            )
        }
    }

    fun beginDraining() {
        _state.update { it.copy(isCapturing = false, isProcessing = true) }
    }

    fun stopSession() {
        _state.update { it.copy(isCapturing = false, isProcessing = false) }
    }

    fun tickDuration(elapsedMs: Long) {
        _state.update { if (it.isCapturing) it.copy(durationMs = elapsedMs) else it }
    }

    fun recordTranscription(text: String, atMs: Long) {
        _state.update {
            it.copy(
                sessionCapturedCount = it.sessionCapturedCount + 1,
                latestThought = text,
                latestThoughtAtMs = atMs,
            )
        }
    }

    fun recordSent() {
        _state.update { it.copy(sessionSentCount = it.sessionSentCount + 1) }
    }

    fun recordError(message: String) {
        _state.update { it.copy(lastError = message) }
    }
}
