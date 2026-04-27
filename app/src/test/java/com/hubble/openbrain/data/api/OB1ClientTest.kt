package com.hubble.openbrain.data.api

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OB1ClientTest {

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

    private fun client(endpoint: String, key: String, requireHttps: Boolean = false): OB1Client {
        val settings = object : OB1Settings {
            override val endpoint = flowOf(endpoint)
            override val accessKey = flowOf(key)
        }
        return OB1Client(OkHttpClient(), settings, requireHttps)
    }

    @Test
    fun `request URL has no key query param and key is sent only as header`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"jsonrpc":"2.0","id":1,"result":{"content":[{"type":"text","text":"ok"}]}}""")
        )

        val secret = "secret-key-with-special/=chars"
        val result = client(server.url("/mcp").toString(), secret).captureThought("hello")

        assertTrue(result is OB1Client.CaptureResult.Success)
        val recorded = server.takeRequest()
        assertNull("URL must not contain a key query param", recorded.requestUrl?.queryParameter("key"))
        assertFalse("Raw URL must not contain key=", recorded.path!!.contains("key="))
        assertEquals(secret, recorded.getHeader("x-brain-key"))
    }

    @Test
    fun `blank endpoint returns failure without making a request`() = runTest {
        val result = client("", "k").captureThought("x")
        assertTrue(result is OB1Client.CaptureResult.Failure)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `placeholder endpoint returns failure without making a request`() = runTest {
        val result = client("https://YOUR_PROJECT.supabase.co/x", "k").captureThought("x")
        assertTrue(result is OB1Client.CaptureResult.Failure)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `blank key returns failure without making a request`() = runTest {
        val result = client(server.url("/mcp").toString(), "").captureThought("x")
        assertTrue(result is OB1Client.CaptureResult.Failure)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `http endpoint is rejected when requireHttps is true`() = runTest {
        val httpUrl = server.url("/mcp").toString()
        check(httpUrl.startsWith("http://")) { "MockWebServer should be plain http" }
        val result = client(httpUrl, "k", requireHttps = true).captureThought("x")
        assertTrue(result is OB1Client.CaptureResult.Failure)
        assertEquals(
            "Endpoint must use HTTPS",
            (result as OB1Client.CaptureResult.Failure).message,
        )
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `malformed endpoint URL is rejected`() = runTest {
        val result = client("not a url", "k").captureThought("x")
        assertTrue(result is OB1Client.CaptureResult.Failure)
        assertEquals(0, server.requestCount)
    }
}
