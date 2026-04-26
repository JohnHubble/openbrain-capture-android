package com.hubble.openbrain.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

class AudioRecorder @Inject constructor() {

    private val stopRequested = AtomicBoolean(false)

    /**
     * Request the current [stream] to complete cleanly so downstream flow operators receive
     * a normal completion (rather than cancellation) and can flush partial buffers.
     */
    fun requestStop() {
        stopRequested.set(true)
    }

    /**
     * Open [AudioRecord] at 16 kHz mono 16-bit PCM and emit fixed-size frames until either
     * the collecting coroutine is cancelled or [requestStop] is called. Requires
     * [android.Manifest.permission.RECORD_AUDIO].
     */
    @SuppressLint("MissingPermission")
    fun stream(): Flow<ShortArray> = flow {
        stopRequested.set(false)
        val minBuf = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        require(minBuf > 0) { "AudioRecord.getMinBufferSize failed: $minBuf" }
        val bufBytes = maxOf(minBuf, FRAME_SAMPLES * 2 * 4)
        val recorder = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufBytes,
        )
        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            error("AudioRecord failed to initialize (state=${recorder.state})")
        }
        try {
            recorder.startRecording()
            val frame = ShortArray(FRAME_SAMPLES)
            while (currentCoroutineContext().isActive && !stopRequested.get()) {
                val n = recorder.read(frame, 0, frame.size)
                if (n > 0) emit(frame.copyOf(n)) else if (n < 0) break
            }
        } finally {
            runCatching { recorder.stop() }
            recorder.release()
        }
    }.flowOn(Dispatchers.IO)

    companion object {
        const val SAMPLE_RATE = 16_000
        private const val FRAME_MS = 100
        private const val FRAME_SAMPLES = SAMPLE_RATE * FRAME_MS / 1000
    }
}
