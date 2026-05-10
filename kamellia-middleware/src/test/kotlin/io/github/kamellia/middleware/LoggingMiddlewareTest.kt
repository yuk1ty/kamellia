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

class LoggingMiddlewareTest {
    private fun req(): Request = Request(
        method = HttpMethod.GET,
        path = "/",
        headers = emptyMap(),
        queryParams = QueryParams.empty(),
        pathParams = PathParams.empty(),
        body = Body.Empty,
        context = Context(),
    )

    @Test
    fun `should pass through the response unchanged when logging middleware wraps a handler`() = runTest {
        val handler: RawHandler = { Response(HttpStatus.OK, body = Body.Strict("ok".toByteArray())) }
        val middleware = loggingMiddleware()(handler)

        val response = middleware(req())

        assertEquals(HttpStatus.OK, response.status)
        val body = response.body
        check(body is Body.Strict)
        assertEquals("ok", body.bytes.decodeToString())
    }
}
