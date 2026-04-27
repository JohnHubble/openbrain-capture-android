package com.hubble.openbrain.data.api

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OB1Client @Inject constructor(
    private val http: OkHttpClient,
    private val settings: OB1Settings,
) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val idCounter = AtomicLong(System.currentTimeMillis())

    sealed interface CaptureResult {
        data class Success(val text: String) : CaptureResult
        data class Failure(val message: String, val retriable: Boolean) : CaptureResult
    }

    suspend fun captureThought(content: String): CaptureResult = withContext(Dispatchers.IO) {
        val endpoint = settings.endpoint.first()
        val key = settings.accessKey.first()
        if (endpoint.isBlank() || endpoint.contains("YOUR_PROJECT")) {
            return@withContext CaptureResult.Failure("Endpoint not configured", retriable = false)
        }
        if (key.isBlank()) {
            return@withContext CaptureResult.Failure("Access key not set", retriable = false)
        }

        val body = buildBody(content)
        val request = Request.Builder()
            .url(endpoint)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json, text/event-stream")
            .header("x-brain-key", key)
            .post(body.toRequestBody(JSON_MEDIA))
            .build()

        runCatching {
            http.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val retriable = response.code in 500..599 || response.code == 429 || response.code == 408
                    return@use CaptureResult.Failure("HTTP ${response.code}: ${raw.take(200)}", retriable)
                }
                val parsed = SseParser.parse(raw, json)
                parsed.error?.let { return@use CaptureResult.Failure(errorMessage(it), retriable = false) }
                val text = parsed.result?.content?.firstOrNull { it.type == "text" }?.text
                    ?: return@use CaptureResult.Failure("No text in response", retriable = false)
                CaptureResult.Success(text)
            }
        }.getOrElse { err ->
            Log.w(TAG, "captureThought network error", err)
            CaptureResult.Failure(err.message ?: "network error", retriable = true)
        }
    }

    private fun buildBody(content: String): String {
        val req = JsonRpcRequest(
            id = idCounter.incrementAndGet(),
            method = "tools/call",
            params = ToolsCallParams(
                name = "capture_thought",
                arguments = buildJsonObject { put("content", JsonPrimitive(content)) },
            ),
        )
        return json.encodeToString(JsonRpcRequest.serializer(), req)
    }

    private fun errorMessage(error: kotlinx.serialization.json.JsonElement): String = runCatching {
        when (error) {
            is JsonPrimitive -> error.content
            is JsonObject -> error["message"]?.jsonPrimitive?.content ?: error.toString()
            else -> error.toString()
        }
    }.getOrDefault(error.toString())

    companion object {
        private const val TAG = "OB1Client"
        private val JSON_MEDIA = "application/json".toMediaType()
    }
}
