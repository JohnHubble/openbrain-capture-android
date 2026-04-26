package com.hubble.openbrain.data.api

import kotlinx.serialization.json.Json

/**
 * OB1's edge function responds either with a single JSON object or with an SSE stream of
 * `data:`-prefixed lines (one `[DONE]` sentinel at the end). For `capture_thought` the
 * meaningful payload is always in the last `data:` line that parses as valid JSON.
 */
object SseParser {

    fun parse(body: String, json: Json): JsonRpcResponse {
        val trimmed = body.trim()
        if (trimmed.isEmpty()) error("empty response body")
        if (trimmed.startsWith("{")) {
            return json.decodeFromString(JsonRpcResponse.serializer(), trimmed)
        }
        val payloads = trimmed.lineSequence()
            .map { it.trim() }
            .filter { it.startsWith("data:") }
            .map { it.removePrefix("data:").trim() }
            .filter { it.isNotEmpty() && it != "[DONE]" }
            .toList()
        for (payload in payloads.asReversed()) {
            val parsed = runCatching { json.decodeFromString(JsonRpcResponse.serializer(), payload) }
            if (parsed.isSuccess) return parsed.getOrThrow()
        }
        error("no parseable JSON in SSE body (${body.take(200)}…)")
    }
}
