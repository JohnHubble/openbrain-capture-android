package com.hubble.openbrain.transcribe

import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException
import java.security.MessageDigest

class ModelDownloaderTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun sha256Hex(bytes: ByteArray): String {
        val d = MessageDigest.getInstance("SHA-256").digest(bytes)
        val hex = "0123456789abcdef".toCharArray()
        val sb = StringBuilder(d.size * 2)
        for (b in d) {
            val v = b.toInt() and 0xff
            sb.append(hex[v ushr 4]).append(hex[v and 0x0f])
        }
        return sb.toString()
    }

    @Test
    fun `successful download with matching hash writes final file and removes part`() {
        val payload = ByteArray(200_000) { (it % 251).toByte() }
        val expected = sha256Hex(payload)
        server.enqueue(MockResponse().setBody(Buffer().apply { write(payload) }))

        val partFile = tmp.newFile("ggml-test.bin.part").also { it.delete() }
        val finalFile = tmp.newFile("ggml-test.bin").also { it.delete() }

        ModelDownloader.downloadAndVerify(
            http = OkHttpClient(),
            url = server.url("/m").toString(),
            partFile = partFile,
            finalFile = finalFile,
            expectedSha256 = expected,
        )

        assertTrue("final file should exist", finalFile.exists())
        assertFalse("part file should be removed", partFile.exists())
        assertArrayEquals(payload, finalFile.readBytes())
    }

    @Test
    fun `hash mismatch leaves no final file and deletes part`() {
        val payload = ByteArray(50_000) { it.toByte() }
        server.enqueue(MockResponse().setBody(Buffer().apply { write(payload) }))

        val partFile = tmp.newFile("ggml-x.bin.part").also { it.delete() }
        val finalFile = tmp.newFile("ggml-x.bin").also { it.delete() }
        val wrongHash = "0".repeat(64)

        var threw: Throwable? = null
        try {
            ModelDownloader.downloadAndVerify(
                http = OkHttpClient(),
                url = server.url("/m").toString(),
                partFile = partFile,
                finalFile = finalFile,
                expectedSha256 = wrongHash,
            )
        } catch (t: Throwable) {
            threw = t
        }
        assertTrue(threw is IOException)
        assertTrue(threw!!.message!!.contains("integrity check failed"))
        assertFalse("part file should be cleaned up", partFile.exists())
        assertFalse("final file must not exist on hash mismatch", finalFile.exists())
    }

    @Test
    fun `non-2xx response throws and leaves no files`() {
        server.enqueue(MockResponse().setResponseCode(503))

        val partFile = tmp.newFile("ggml-y.bin.part").also { it.delete() }
        val finalFile = tmp.newFile("ggml-y.bin").also { it.delete() }

        var threw: Throwable? = null
        try {
            ModelDownloader.downloadAndVerify(
                http = OkHttpClient(),
                url = server.url("/m").toString(),
                partFile = partFile,
                finalFile = finalFile,
                expectedSha256 = "0".repeat(64),
            )
        } catch (t: Throwable) {
            threw = t
        }
        assertTrue(threw is IOException)
        assertFalse(partFile.exists())
        assertFalse(finalFile.exists())
    }

    @Test
    fun `progress sink receives monotonic non-decreasing bytes`() {
        val payload = ByteArray(800_000) { (it % 7).toByte() }
        val expected = sha256Hex(payload)
        server.enqueue(MockResponse().setBody(Buffer().apply { write(payload) }))

        val partFile = tmp.newFile("ggml-p.bin.part").also { it.delete() }
        val finalFile = tmp.newFile("ggml-p.bin").also { it.delete() }
        val progress = mutableListOf<Long>()

        ModelDownloader.downloadAndVerify(
            http = OkHttpClient(),
            url = server.url("/m").toString(),
            partFile = partFile,
            finalFile = finalFile,
            expectedSha256 = expected,
        ) { downloaded, _ -> progress.add(downloaded) }

        assertTrue("expected at least one progress update", progress.isNotEmpty())
        for (i in 1 until progress.size) {
            assertTrue("progress must be non-decreasing", progress[i] >= progress[i - 1])
        }
        assertEquals(payload.size.toLong(), finalFile.length())
    }
}
