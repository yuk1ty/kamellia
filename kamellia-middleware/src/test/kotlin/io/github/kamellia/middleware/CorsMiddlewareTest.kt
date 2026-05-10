package io.github.kamellia.middleware

import io.github.kamellia.core.Body
import io.github.kamellia.core.Context
import io.github.kamellia.core.HttpMethod
import io.github.kamellia.core.HttpStatus
import io.github.kamellia.core.PathParams
import io.github.kamellia.core.QueryParams
import io.github.kamellia.core.Request
import io.github.kamellia.core.Response
import io.github.kamellia.routing.RawHandler
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CorsMiddlewareTest {
    private fun req(method: HttpMethod, headers: Map<String, String> = emptyMap()): Request = Request(
        method = method,
        path = "/",
        headers = headers,
        queryParams = QueryParams.empty(),
        pathParams = PathParams.empty(),
        body = Body.Empty,
        context = Context(),
    )

    @Test
    fun `should respond OK with CORS headers when method is OPTIONS`() = runTest {
        val handler: RawHandler = { Response(HttpStatus.OK) }
        val middleware = corsMiddleware()

        val res = middleware(handler).invoke(req(HttpMethod.OPTIONS))

        assertEquals(HttpStatus.OK, res.status)
        assertEquals("*", res.headers["Access-Control-Allow-Origin"])
    }

    @Test
    fun `should add CORS headers to non-preflight responses`() = runTest {
        val handler: RawHandler = { Response(HttpStatus.OK, body = Body.Strict("hi".toByteArray())) }
        val middleware = corsMiddleware()

        val res = middleware(handler).invoke(req(HttpMethod.GET))

        assertEquals(HttpStatus.OK, res.status)
        assertEquals("*", res.headers["Access-Control-Allow-Origin"])
    }
}
