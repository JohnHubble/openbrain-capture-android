package com.hubble.openbrain.service

/**
 * One capture session walks through these phases. Replaces the old `isCapturing`/`isProcessing`
 * boolean pair so the UI, tile, widget, and notification can render distinct states for
 * Recording vs. Transcribing vs. Preview.
 */
sealed interface CapturePhase {
    data object Idle : CapturePhase
    data class Recording(val durationMs: Long, val nearLimit: Boolean) : CapturePhase
    data object Transcribing : CapturePhase
    data class Preview(val transcript: String, val durationMs: Long) : CapturePhase
    data object Saving : CapturePhase
    data class Saved(val transcript: String, val savedAtMs: Long, val durationMs: Long) : CapturePhase
    data class Error(val message: String) : CapturePhase
}

val CapturePhase.isActive: Boolean
    get() = when (this) {
        is CapturePhase.Recording, CapturePhase.Transcribing,
        is CapturePhase.Preview, CapturePhase.Saving -> true
        else -> false
    }

val CapturePhase.isRecording: Boolean
    get() = this is CapturePhase.Recording

val CapturePhase.isBusy: Boolean
    get() = this is CapturePhase.Transcribing || this is CapturePhase.Saving
