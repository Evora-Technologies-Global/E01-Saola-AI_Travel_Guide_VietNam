package com.evora.technologies.saola.data.remote.gemini

import com.evora.technologies.saola.data.remote.gemini.dto.DiscoveryPayload
import com.evora.technologies.saola.data.remote.gemini.dto.Part
import com.evora.technologies.saola.domain.util.AppError
import com.evora.technologies.saola.domain.util.AppResult
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.serializer
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.Test

/**
 * Covers the behaviour the app actually depends on in the field: what happens when
 * Gemini is busy, retired, or answers with something unparseable.
 *
 * Gemini Flash answers 503 often enough during peak hours that the fallback chain
 * is not a nicety — without it, a capture in front of a monument simply fails.
 */
class GeminiClientTest {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = false
        isLenient = true
        coerceInputValues = true
    }

    private fun clientWith(
        handlers: List<suspend (String) -> MockResponse>,
        apiKey: String? = "test-key",
    ): Pair<GeminiClient, MutableList<String>> {
        val requestedModels = mutableListOf<String>()
        var index = 0

        val engine = MockEngine { request ->
            val model = request.url.encodedPath.substringAfterLast("/models/").substringBefore(':')
            requestedModels += model
            val handler = handlers[index.coerceAtMost(handlers.lastIndex)]
            index += 1
            val response = handler(model)
            respond(
                content = response.body,
                status = response.status,
                headers = headersOf("Content-Type", "application/json"),
            )
        }

        val httpClient = HttpClient(engine) {
            expectSuccess = false
            install(ContentNegotiation) { json(json) }
        }

        val client = GeminiClient(
            httpClient = httpClient,
            apiKeyProvider = object : ApiKeyProvider {
                override suspend fun apiKey(): String? = apiKey
            },
            json = json,
        )
        return client to requestedModels
    }

    @Test
    fun `walks down the model chain when the preferred model is busy`() = runTest {
        val (client, requested) = clientWith(
            listOf(
                { MockResponse(HttpStatusCode.ServiceUnavailable, ERROR_503) },
                { MockResponse(HttpStatusCode.OK, successBody("Temple of Literature")) },
            ),
        )

        val result = client.generateStructured(
            modelChain = listOf("gemini-3.5-flash", "gemini-3.1-flash-lite"),
            systemInstruction = "system",
            parts = listOf(Part(text = "prompt")),
            schema = buildJsonObject { },
            deserializer = serializer<DiscoveryPayload>(),
        )

        assertTrue(result is AppResult.Success)
        val success = result as AppResult.Success
        assertEquals("Temple of Literature", success.data.value.title)
        // The fallback model actually answered, and the UI is told which one did.
        assertEquals("gemini-3.1-flash-lite", success.data.modelUsed)
        assertEquals(listOf("gemini-3.5-flash", "gemini-3.1-flash-lite"), requested)
    }

    @Test
    fun `reports every model busy when the whole chain is unavailable`() = runTest {
        val (client, requested) = clientWith(
            listOf { MockResponse(HttpStatusCode.ServiceUnavailable, ERROR_503) },
        )

        val result = client.generateStructured(
            modelChain = listOf("a", "b", "c"),
            systemInstruction = "system",
            parts = listOf(Part(text = "prompt")),
            schema = buildJsonObject { },
            deserializer = serializer<DiscoveryPayload>(),
        )

        assertTrue(result is AppResult.Failure)
        assertEquals(3, requested.size)
    }

    @Test
    fun `stops immediately on an invalid key instead of burning the whole chain`() = runTest {
        val (client, requested) = clientWith(
            listOf { MockResponse(HttpStatusCode.Forbidden, ERROR_FORBIDDEN) },
        )

        val result = client.generateStructured(
            modelChain = listOf("a", "b", "c"),
            systemInstruction = "system",
            parts = listOf(Part(text = "prompt")),
            schema = buildJsonObject { },
            deserializer = serializer<DiscoveryPayload>(),
        )

        assertEquals(AppResult.Failure(AppError.InvalidApiKey), result)
        // Trying the other models could not have helped and would waste quota.
        assertEquals(1, requested.size)
    }

    @Test
    fun `moves past a model that has been retired for this key`() = runTest {
        val (client, requested) = clientWith(
            listOf(
                { MockResponse(HttpStatusCode.NotFound, ERROR_RETIRED) },
                { MockResponse(HttpStatusCode.OK, successBody("Phở")) },
            ),
        )

        val result = client.generateStructured(
            modelChain = listOf("gemini-2.5-flash", "gemini-3.1-flash-lite"),
            systemInstruction = "system",
            parts = listOf(Part(text = "prompt")),
            schema = buildJsonObject { },
            deserializer = serializer<DiscoveryPayload>(),
        )

        assertTrue(result is AppResult.Success)
        assertEquals(2, requested.size)
    }

    @Test
    fun `fails fast without a key rather than issuing a doomed request`() = runTest {
        val (client, requested) = clientWith(
            listOf { MockResponse(HttpStatusCode.OK, successBody("unused")) },
            apiKey = null,
        )

        val result = client.generateStructured(
            modelChain = listOf("gemini-3.5-flash"),
            systemInstruction = "system",
            parts = listOf(Part(text = "prompt")),
            schema = buildJsonObject { },
            deserializer = serializer<DiscoveryPayload>(),
        )

        assertEquals(AppResult.Failure(AppError.MissingApiKey), result)
        assertTrue(requested.isEmpty())
    }

    @Test
    fun `surfaces malformed payloads as a typed failure`() = runTest {
        val (client, _) = clientWith(
            listOf { MockResponse(HttpStatusCode.OK, textResponseBody("this is not json")) },
        )

        val result = client.generateStructured(
            modelChain = listOf("gemini-3.5-flash"),
            systemInstruction = "system",
            parts = listOf(Part(text = "prompt")),
            schema = buildJsonObject { },
            deserializer = serializer<DiscoveryPayload>(),
        )

        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).error is AppError.Malformed)
    }

    /**
     * Gemini 3 emits reasoning parts alongside the answer. Concatenating every part
     * blindly would splice the thought text into the JSON and break parsing.
     */
    @Test
    fun `ignores reasoning parts when reading the answer`() = runTest {
        val body = """
            {
              "candidates": [{
                "content": {
                  "parts": [
                    {"text": "Let me think about this temple...", "thought": true},
                    {"text": "{\"recognized\":true,\"title\":\"One Pillar Pagoda\",\"category\":\"LANDMARK\",\"confidence\":0.9,\"summary\":\"s\"}",
                     "thoughtSignature": "abc123"}
                  ],
                  "role": "model"
                },
                "finishReason": "STOP"
              }],
              "modelVersion": "gemini-3.5-flash"
            }
        """.trimIndent()

        val (client, _) = clientWith(listOf { MockResponse(HttpStatusCode.OK, body) })

        val result = client.generateStructured(
            modelChain = listOf("gemini-3.5-flash"),
            systemInstruction = "system",
            parts = listOf(Part(text = "prompt")),
            schema = buildJsonObject { },
            deserializer = serializer<DiscoveryPayload>(),
        )

        assertTrue(result is AppResult.Success)
        assertEquals("One Pillar Pagoda", (result as AppResult.Success).data.value.title)
    }

    @Test
    fun `treats an empty candidate list as nothing recognised`() = runTest {
        val (client, _) = clientWith(
            listOf { MockResponse(HttpStatusCode.OK, """{"candidates":[]}""") },
        )

        val result = client.generateStructured(
            modelChain = listOf("gemini-3.5-flash"),
            systemInstruction = "system",
            parts = listOf(Part(text = "prompt")),
            schema = buildJsonObject { },
            deserializer = serializer<DiscoveryPayload>(),
        )

        assertTrue((result as AppResult.Failure).error is AppError.NotRecognized)
    }

    private data class MockResponse(val status: HttpStatusCode, val body: String)

    private companion object {
        const val ERROR_503 =
            """{"error":{"code":503,"message":"This model is currently experiencing high demand.","status":"UNAVAILABLE"}}"""
        const val ERROR_FORBIDDEN =
            """{"error":{"code":403,"message":"Permission denied","status":"PERMISSION_DENIED"}}"""
        const val ERROR_RETIRED =
            """{"error":{"code":404,"message":"This model is no longer available to new users.","status":"NOT_FOUND"}}"""

        fun successBody(title: String): String {
            val payload = """{"recognized":true,"title":"$title","category":"LANDMARK","confidence":0.92,"summary":"A summary."}"""
            return textResponseBody(payload)
        }

        fun textResponseBody(text: String): String {
            val escaped = text.replace("\\", "\\\\").replace("\"", "\\\"")
            return """{"candidates":[{"content":{"parts":[{"text":"$escaped"}],"role":"model"},"finishReason":"STOP"}]}"""
        }
    }
}
