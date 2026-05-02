package com.hubble.openbrain.audio

/**
 * Accumulates 16-bit signed PCM frames from [AudioRecorder] for the duration of a single
 * user-bounded capture session, then on [finalize] returns the entire session as a single
 * normalized [FloatArray] suitable for one Whisper pass.
 *
 * In-memory only. At [MAX_SAMPLES] (10 min × 16 kHz = 9.6M samples) the buffer is full;
 * callers should auto-stop before reaching [hasCapacity] == false. Peak heap during a full
 * session: ~19 MB while accumulating + ~38 MB transient FloatArray on finalize.
 */
class SessionAudioBuffer(
    private val sampleRate: Int = AudioRecorder.SAMPLE_RATE,
    private val maxSamples: Int = MAX_SAMPLES,
) {

    private val chunks = ArrayList<ShortArray>()
    private var totalSamples: Int = 0

    /**
     * Append a captured frame. If the frame would exceed [maxSamples] only the prefix that
     * fits is appended and the rest is dropped — combined with the duration-driven auto-stop,
     * this should never happen in practice but bounds memory if the ticker runs late.
     */
    fun append(chunk: ShortArray) {
        if (chunk.isEmpty()) return
        val remaining = maxSamples - totalSamples
        if (remaining <= 0) return
        val keep = if (chunk.size <= remaining) chunk else chunk.copyOf(remaining)
        chunks.add(keep)
        totalSamples += keep.size
    }

    fun durationMs(): Long = totalSamples * 1000L / sampleRate

    fun isEmpty(): Boolean = totalSamples == 0

    fun hasCapacity(): Boolean = totalSamples < maxSamples

    /** Concatenate accumulated chunks into a single FloatArray normalized to [-1, 1]. */
    fun finalize(): FloatArray {
        val out = FloatArray(totalSamples)
        var offset = 0
        val scale = 1f / 32768f
        for (chunk in chunks) {
            for (i in chunk.indices) {
                out[offset + i] = chunk[i] * scale
            }
            offset += chunk.size
        }
        chunks.clear()
        totalSamples = 0
        return out
    }

    fun discard() {
        chunks.clear()
        totalSamples = 0
    }

    companion object {
        const val MAX_SAMPLES: Int = AudioRecorder.SAMPLE_RATE * 60 * 10
        const val MAX_DURATION_MS: Long = 10 * 60 * 1000L
        const val WARN_DURATION_MS: Long = 8 * 60 * 1000L
    }
}
