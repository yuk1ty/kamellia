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
    fun `should run handler unchanged when no middleware is given`() = runTest {
        val handler: RawHandler = { Response(HttpStatus.OK, body = Body.Strict("hi".toByteArray())) }

        val composed = composeMiddlewares(emptyList(), handler)
        val res = composed(req())

        val body = res.body
        check(body is Body.Strict)
        assertEquals("hi", body.bytes.decodeToString())
    }

    @Test
    fun `should compose multiple middlewares in registration order`() = runTest {
        val trace = mutableListOf<String>()
        val mw1: Middleware = { next ->
            { request ->
                trace.add("m1-pre")
                val res = next(request)
                trace.add("m1-post")
                res
            }
        }
        val mw2: Middleware = { next ->
            { request ->
                trace.add("m2-pre")
                val res = next(request)
                trace.add("m2-post")
                res
            }
        }
        val handler: RawHandler = {
            trace.add("handler")
            Response(HttpStatus.OK)
        }

        val composed = composeMiddlewares(listOf(mw1, mw2), handler)
        composed(req())

        assertEquals(listOf("m1-pre", "m2-pre", "handler", "m2-post", "m1-post"), trace)
    }
}
