package io.github.kamellia.core

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class RequestTest {
    @Test
    fun testTextExtensionWithTextBody() = runTest {
        val request =
            Request(
                method = HttpMethod.GET,
                path = "/",
                headers = emptyMap(),
                queryParams = QueryParams.empty(),
                pathParams = PathParams.empty(),
                body = Body.Text("Hello, World!"),
                context = Context(),
            )

        assertEquals("Hello, World!", request.text())
    }

    @Test
    fun testTextExtensionWithBinaryBody() = runTest {
        val request =
            Request(
                method = HttpMethod.GET,
                path = "/",
                headers = emptyMap(),
                queryParams = QueryParams.empty(),
                pathParams = PathParams.empty(),
                body = Body.Binary("Hello".toByteArray()),
                context = Context(),
            )

        assertEquals("Hello", request.text())
    }

    @Test
    fun testTextExtensionWithEmptyBody() = runTest {
        val request =
            Request(
                method = HttpMethod.GET,
                path = "/",
                headers = emptyMap(),
                queryParams = QueryParams.empty(),
                pathParams = PathParams.empty(),
                body = Body.Empty,
                context = Context(),
            )

        assertEquals("", request.text())
    }

    @Test
    fun testRequestCreation() {
        val request =
            Request(
                method = HttpMethod.POST,
                path = "/users",
                headers = mapOf("Content-Type" to "application/json"),
                queryParams = QueryParams.of(mapOf("filter" to listOf("active"))),
                pathParams = PathParams.of(mapOf("id" to "123")),
                body = Body.Text("{}"),
                context = Context(),
            )

        assertEquals(HttpMethod.POST, request.method)
        assertEquals("/users", request.path)
        assertEquals("application/json", request.headers["Content-Type"])
        assertEquals(listOf("active"), request.queryParams.list("filter"))
        assertEquals("123", request.pathParams.string("id"))
    }
}
