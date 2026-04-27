package com.hubble.openbrain.transcribe

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.security.MessageDigest

/**
 * Pure download + SHA-256 verify helper, extracted so it can be unit-tested without
 * an Android Context. Streams the response into [partFile], hashes as it writes, and
 * either renames to [finalFile] on hash match or deletes [partFile] and throws.
 *
 * On any failure the part file is removed.
 */
internal object ModelDownloader {

    fun interface ProgressSink {
        fun onProgress(downloaded: Long, total: Long)
    }

    @Throws(IOException::class)
    fun downloadAndVerify(
        http: OkHttpClient,
        url: String,
        partFile: File,
        finalFile: File,
        expectedSha256: String,
        progress: ProgressSink = ProgressSink { _, _ -> },
    ) {
        val request = Request.Builder().url(url).build()
        try {
            http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
                val body = response.body ?: throw IOException("Empty response body")
                val total = body.contentLength().takeIf { it > 0 } ?: -1L
                val digest = MessageDigest.getInstance("SHA-256")
                partFile.outputStream().use { out ->
                    body.byteStream().use { input ->
                        val buf = ByteArray(64 * 1024)
                        var downloaded = 0L
                        var lastReport = 0L
                        while (true) {
                            val n = input.read(buf)
                            if (n <= 0) break
                            out.write(buf, 0, n)
                            digest.update(buf, 0, n)
                            downloaded += n
                            if (downloaded - lastReport > 256 * 1024) {
                                progress.onProgress(downloaded, total)
                                lastReport = downloaded
                            }
                        }
                    }
                }
                val actual = digest.digest().toHex()
                val expected = expectedSha256.lowercase()
                if (actual != expected) {
                    throw IOException(
                        "Model integrity check failed for ${finalFile.name}: " +
                            "expected $expected, got $actual",
                    )
                }
                if (finalFile.exists()) finalFile.delete()
                if (!partFile.renameTo(finalFile)) {
                    throw IOException("Rename ${partFile.name} -> ${finalFile.name} failed")
                }
            }
        } catch (t: Throwable) {
            if (partFile.exists()) partFile.delete()
            throw t
        }
    }

    private fun ByteArray.toHex(): String {
        val hex = "0123456789abcdef".toCharArray()
        val out = StringBuilder(size * 2)
        for (b in this) {
            val v = b.toInt() and 0xff
            out.append(hex[v ushr 4]).append(hex[v and 0x0f])
        }
        return out.toString()
    }
}
