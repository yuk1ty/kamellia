package io.github.kamellia.serialization

import io.github.kamellia.core.Body
import io.github.kamellia.core.Context
import io.github.kamellia.core.HttpMethod
import io.github.kamellia.core.PathParams
import io.github.kamellia.core.QueryParams
import io.github.kamellia.core.Request
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RequestJsonTest {
    @Serializable
    data class User(val id: String, val name: String)

    private fun req(headers: Map<String, String> = emptyMap(), body: Body = Body.Empty): Request = Request(
        method = HttpMethod.POST,
        path = "/users",
        headers = headers,
        queryParams = QueryParams.empty(),
        pathParams = PathParams.empty(),
        body = body,
        context = Context(),
    )

    @Test
    fun `should decode a Serializable value from a Body Strict request body`() = runTest {
        val request = req(
            headers = mapOf("content-type" to "application/json"),
            body = Body.Strict("""{"id":"1","name":"Alice"}""".toByteArray()),
        )

        val user = request.json<User>()

        assertEquals(User("1", "Alice"), user)
    }

    @Test
    fun `should decode a List of Serializable values via reified typeOf`() = runTest {
        val request = req(
            headers = mapOf("content-type" to "application/json"),
            body = Body.Strict("""[{"id":"1","name":"A"},{"id":"2","name":"B"}]""".toByteArray()),
        )

        val users = request.json<List<User>>()

        assertEquals(listOf(User("1", "A"), User("2", "B")), users)
    }

    @Test
    fun `should throw SerializationException when the request body is Empty`() = runTest {
        val request = req(
            headers = mapOf("content-type" to "application/json"),
            body = Body.Empty,
        )

        val ex = assertFailsWith<SerializationException> { request.json<User>() }
        assertTrue(ex.message?.contains("empty") == true)
    }

    @Test
    fun `should aggregate a Streamed body and decode it as a Serializable value`() = runTest {
        val chunks = listOf(
            """{"id":"1",""".toByteArray(),
            """"name":"Alice"}""".toByteArray(),
        )
        val request = req(
            headers = mapOf("content-type" to "application/json"),
            body = Body.Streamed(flowOf(*chunks.toTypedArray())),
        )

        val user = request.json<User>()

        assertEquals(User("1", "Alice"), user)
    }

    @Test
    fun `should throw SerializationException when a Streamed body emits no chunks`() = runTest {
        val request = req(
            headers = mapOf("content-type" to "application/json"),
            body = Body.Streamed(emptyFlow()),
        )

        assertFailsWith<SerializationException> { request.json<User>() }
    }

    @Test
    fun `should propagate SerializationException when the body is not valid JSON`() = runTest {
        val request = req(
            headers = mapOf("content-type" to "application/json"),
            body = Body.Strict("not json".toByteArray()),
        )

        assertFailsWith<SerializationException> { request.json<User>() }
    }

    @Test
    fun `should propagate SerializationException when a required field is missing`() = runTest {
        val request = req(
            headers = mapOf("content-type" to "application/json"),
            body = Body.Strict("""{"id":"1"}""".toByteArray()),
        )

        assertFailsWith<SerializationException> { request.json<User>() }
    }

    @Test
    fun `should throw SerializationException when Content-Type is not JSON and strict is default`() = runTest {
        val request = req(
            headers = mapOf("content-type" to "text/plain"),
            body = Body.Strict("""{"id":"1","name":"Alice"}""".toByteArray()),
        )

        val ex = assertFailsWith<SerializationException> { request.json<User>() }
        assertTrue(ex.message?.contains("Content-Type") == true)
    }

    @Test
    fun `should throw SerializationException when Content-Type header is missing and strict is default`() = runTest {
        val request = req(
            headers = emptyMap(),
            body = Body.Strict("""{"id":"1","name":"Alice"}""".toByteArray()),
        )

        assertFailsWith<SerializationException> { request.json<User>() }
    }

    @Test
    fun `should skip Content-Type validation when strict is explicitly false`() = runTest {
        val request = req(
            headers = mapOf("content-type" to "text/plain"),
            body = Body.Strict("""{"id":"1","name":"Alice"}""".toByteArray()),
        )

        val user = request.json<User>(strict = false)

        assertEquals(User("1", "Alice"), user)
    }

    @Test
    fun `should decode the body when strict is false even if the Content-Type header is missing`() = runTest {
        val request = req(
            headers = emptyMap(),
            body = Body.Strict("""{"id":"1","name":"Alice"}""".toByteArray()),
        )

        val user = request.json<User>(strict = false)

        assertEquals(User("1", "Alice"), user)
    }

    @Test
    fun `should accept Content-Type header regardless of key casing`() = runTest {
        val request = req(
            headers = mapOf("Content-Type" to "application/json"),
            body = Body.Strict("""{"id":"1","name":"Alice"}""".toByteArray()),
        )

        val user = request.json<User>()

        assertEquals(User("1", "Alice"), user)
    }

    @Test
    fun `should decode a Streamed body composed of many small chunks without quadratic blow-up`() = runTest {
        val full = """{"id":"1","name":"Alice"}""".toByteArray()
        val chunks = full.map { byteArrayOf(it) }.toTypedArray()
        val request = req(
            headers = mapOf("content-type" to "application/json"),
            body = Body.Streamed(flowOf(*chunks)),
        )

        val user = request.json<User>()

        assertEquals(User("1", "Alice"), user)
    }
}
