package com.hubble.openbrain.data.api

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SseParserTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Test
    fun `parses a plain JSON-RPC response object`() {
        val body = """{"jsonrpc":"2.0","id":1,"result":{"content":[{"type":"text","text":"ok"}]}}"""
        val parsed = SseParser.parse(body, json)
        assertEquals("ok", parsed.result?.content?.first()?.text)
        assertNull(parsed.error)
    }

    @Test
    fun `parses the last data line of an SSE stream and ignores DONE`() {
        val body = """
            data: {"jsonrpc":"2.0","id":1,"result":{"content":[{"type":"text","text":"first"}]}}

            data: {"jsonrpc":"2.0","id":2,"result":{"content":[{"type":"text","text":"final"}]}}

            data: [DONE]
        """.trimIndent()
        val parsed = SseParser.parse(body, json)
        assertEquals("final", parsed.result?.content?.first()?.text)
    }

    @Test
    fun `falls back to an earlier data line if later one fails to parse`() {
        val body = """
            data: {"jsonrpc":"2.0","id":1,"result":{"content":[{"type":"text","text":"good"}]}}

            data: {malformed
        """.trimIndent()
        val parsed = SseParser.parse(body, json)
        assertEquals("good", parsed.result?.content?.first()?.text)
    }

    @Test(expected = IllegalStateException::class)
    fun `empty body throws`() {
        SseParser.parse("   ", json)
    }

    @Test(expected = IllegalStateException::class)
    fun `body with no parseable data lines throws`() {
        SseParser.parse("data: not json\n\ndata: also not json", json)
    }

    @Test
    fun `surfaces JSON-RPC error field`() {
        val body = """{"jsonrpc":"2.0","id":1,"error":{"code":-1,"message":"nope"}}"""
        val parsed = SseParser.parse(body, json)
        assertNotNull(parsed.error)
        val err = parsed.error as JsonObject
        assertEquals("nope", err["message"]?.jsonPrimitive?.content)
        assertNull(parsed.result)
    }

    @Test
    fun `tolerates Windows line endings in SSE body`() {
        val body = "data: {\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{\"content\":[{\"type\":\"text\",\"text\":\"crlf\"}]}}\r\n\r\ndata: [DONE]\r\n"
        val parsed = SseParser.parse(body, json)
        assertTrue(parsed.result?.content?.first()?.text == "crlf")
    }
}
