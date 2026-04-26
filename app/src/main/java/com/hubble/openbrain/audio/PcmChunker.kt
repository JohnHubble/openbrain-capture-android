package com.hubble.openbrain.audio

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Accumulate signed 16-bit PCM samples into fixed windows and emit each completed window as
 * a [FloatArray] normalized to the closed range [-1.0, 1.0] that Whisper expects. When the
 * upstream flow completes cleanly (user stop), any partial buffer ≥ [flushMinSamples] is
 * emitted so the tail of the session still reaches Whisper.
 */
fun Flow<ShortArray>.chunkToFloatWindows(
    sampleRate: Int = AudioRecorder.SAMPLE_RATE,
    windowSeconds: Int = 30,
    flushMinSamples: Int = sampleRate,
): Flow<FloatArray> = flow {
    val windowSamples = sampleRate * windowSeconds
    val buffer = ShortArray(windowSamples)
    var filled = 0
    collect { block ->
        var offset = 0
        while (offset < block.size) {
            val toCopy = minOf(block.size - offset, windowSamples - filled)
            block.copyInto(
                destination = buffer,
                destinationOffset = filled,
                startIndex = offset,
                endIndex = offset + toCopy,
            )
            filled += toCopy
            offset += toCopy
            if (filled == windowSamples) {
                emit(buffer.normalizeToFloat())
                filled = 0
            }
        }
    }
    if (filled >= flushMinSamples) {
        emit(buffer.copyOf(filled).normalizeToFloat())
    }
}

private fun ShortArray.normalizeToFloat(): FloatArray {
    val out = FloatArray(size)
    val scale = 1f / 32768f
    for (i in indices) out[i] = this[i] * scale
    return out
}
