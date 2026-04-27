package com.hubble.openbrain.audio

import app.cash.turbine.test
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PcmChunkerTest {

    private val sampleRate = 16_000

    @Test
    fun `emits a full window when exactly one window of samples arrives`() = runTest {
        val window = sampleRate * 30
        val src = flowOf(ShortArray(window) { 16_384 }) // half-scale
        src.chunkToFloatWindows(sampleRate, 30, flushMinSamples = sampleRate).test {
            val out = awaitItem()
            assertEquals(window, out.size)
            // 16384 / 32768 = 0.5
            assertEquals(0.5f, out[0], 1e-4f)
            awaitComplete()
        }
    }

    @Test
    fun `splits a multi-window block into multiple emissions`() = runTest {
        val window = sampleRate * 30
        val totalSamples = window * 2 + window / 2 // 2.5 windows
        val src = flowOf(ShortArray(totalSamples) { 0 })
        val emitted = mutableListOf<Int>()
        src.chunkToFloatWindows(sampleRate, 30, flushMinSamples = sampleRate).test {
            emitted += awaitItem().size
            emitted += awaitItem().size
            emitted += awaitItem().size // partial flush
            awaitComplete()
        }
        assertEquals(listOf(window, window, window / 2), emitted)
    }

    @Test
    fun `partial buffer below flushMinSamples is dropped`() = runTest {
        val src = flowOf(ShortArray(sampleRate / 2) { 0 }) // 0.5s, below the 1s threshold
        src.chunkToFloatWindows(
            sampleRate = sampleRate,
            windowSeconds = 30,
            flushMinSamples = sampleRate,
        ).test {
            awaitComplete()
        }
    }

    @Test
    fun `partial buffer at or above flushMinSamples is emitted on completion`() = runTest {
        val src = flowOf(ShortArray(sampleRate * 2) { 100 }) // 2s, above the 1s threshold
        src.chunkToFloatWindows(
            sampleRate = sampleRate,
            windowSeconds = 30,
            flushMinSamples = sampleRate,
        ).test {
            val out = awaitItem()
            assertEquals(sampleRate * 2, out.size)
            awaitComplete()
        }
    }

    @Test
    fun `handles many small upstream blocks that span window boundary`() = runTest {
        val window = sampleRate * 30
        val blockSize = 1_000 // exact divisor of window so block math is clean
        val blocks = List(window / blockSize) { ShortArray(blockSize) { 0 } }
        // Add a half-window tail to test partial-flush after many blocks
        val tail = ShortArray(window / 2) { 0 }
        val src = (blocks + tail).asSequence().asFlow()
        val sizes = mutableListOf<Int>()
        src.chunkToFloatWindows(sampleRate, 30, flushMinSamples = sampleRate).test {
            sizes += awaitItem().size
            sizes += awaitItem().size
            awaitComplete()
        }
        assertEquals(listOf(window, window / 2), sizes)
    }

    @Test
    fun `normalization maps int16 range into closed -1, 1 floats`() = runTest {
        val samples = shortArrayOf(Short.MAX_VALUE, Short.MIN_VALUE, 0)
        // Pad to flush threshold so we get an emission.
        val padded = samples + ShortArray(sampleRate) { 0 }
        flowOf(padded).chunkToFloatWindows(
            sampleRate = sampleRate,
            windowSeconds = 30,
            flushMinSamples = sampleRate,
        ).test {
            val out = awaitItem()
            // 32767/32768 ~= 0.99997
            assertTrue(out[0] in 0.999f..1.0f)
            assertEquals(-1.0f, out[1], 1e-6f)
            assertEquals(0.0f, out[2], 1e-6f)
            awaitComplete()
        }
    }
}

// Tiny helper to avoid importing kotlinx-coroutines-flow extensions explicitly.
private fun <T> Sequence<T>.asFlow() = kotlinx.coroutines.flow.flow {
    forEach { emit(it) }
}
