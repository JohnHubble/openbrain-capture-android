package com.hubble.openbrain.transcribe

import android.util.Log
import com.whispercpp.whisper.TranscriptionResult
import com.whispercpp.whisper.WhisperContext
import java.io.File
import javax.inject.Inject

class WhisperTranscriber @Inject constructor() {

    private var context: WhisperContext? = null
    private var loadedPath: String? = null

    /**
     * Ensure a WhisperContext is loaded against the given model file. No-op if already loaded
     * from the same path. Must be called before [transcribe].
     */
    suspend fun ensureLoaded(modelFile: File) {
        val path = modelFile.absolutePath
        if (context != null && loadedPath == path) return
        release()
        Log.d(TAG, "Loading whisper model: $path (${modelFile.length()} bytes)")
        context = WhisperContext.createContextFromFile(path)
        loadedPath = path
    }

    /**
     * Transcribe 16 kHz mono PCM samples normalized to [-1.0, 1.0] into text plus the
     * maximum per-segment `no_speech_prob` so callers can drop hallucinations on silence.
     */
    suspend fun transcribe(pcm: FloatArray): TranscriptionResult {
        val ctx = context ?: error("WhisperTranscriber: model not loaded, call ensureLoaded() first")
        val raw = ctx.transcribeDataWithSpeechProb(pcm, printTimestamp = false)
        return raw.copy(text = raw.text.trim())
    }

    suspend fun release() {
        context?.release()
        context = null
        loadedPath = null
    }

    companion object {
        private const val TAG = "WhisperTranscriber"

        fun systemInfo(): String = WhisperContext.getSystemInfo()
    }
}
