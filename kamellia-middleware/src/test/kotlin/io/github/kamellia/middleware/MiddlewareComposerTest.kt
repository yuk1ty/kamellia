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

class MiddlewareComposerTest {
    private val request = Request(
        method = HttpMethod.GET,
        path = "/",
        headers = emptyMap(),
        queryParams = QueryParams.empty(),
        pathParams = PathParams.empty(),
        body = Body.Empty,
        context = Context(),
    )

    @Test
    fun `should run handler unchanged when no middleware is given`() = runTest {
        val handler: RawHandler = { Response(HttpStatus.OK, body = Body.Strict("hi".toByteArray())) }

        val composed = composeMiddlewares(emptyList(), handler)
        val res = composed(request)

        assertEquals(HttpStatus.OK, res.status)
    }

    @Test
    fun `should compose multiple middlewares in registration order`() = runTest {
        val trace = mutableListOf<String>()
        val handler: RawHandler = {
            trace.add("handler")
            Response(HttpStatus.OK)
        }
        val outer: Middleware = { next ->
            { req ->
                trace.add("outer-pre")
                val res = next(req)
                trace.add("outer-post")
                res
            }
        }
        val inner: Middleware = { next ->
            { req ->
                trace.add("inner-pre")
                val res = next(req)
                trace.add("inner-post")
                res
            }
        }

        val composed = composeMiddlewares(listOf(outer, inner), handler)
        composed(request)

        assertEquals(
            listOf("outer-pre", "inner-pre", "handler", "inner-post", "outer-post"),
            trace,
        )
    }
}
