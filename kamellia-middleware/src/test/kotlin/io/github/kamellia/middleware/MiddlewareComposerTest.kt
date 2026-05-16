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

    @Test
    fun `should allow a single middleware to decorate the response with extra headers`() = runTest {
        val middleware: Middleware = { next ->
            { request ->
                val response = next(request)
                response.copy(headers = response.headers + ("X-Custom" to "Value"))
            }
        }
        val handler: RawHandler = { Response(HttpStatus.OK, body = Body.Strict("Hello".toByteArray())) }

        val composed = composeMiddlewares(listOf(middleware), handler)
        val response = composed(req())

        assertEquals("Value", response.headers["X-Custom"])
        val body = response.body
        check(body is Body.Strict)
        assertEquals("Hello", body.bytes.decodeToString())
    }

    @Test
    fun `should make request-side mutations visible to the wrapped handler`() = runTest {
        val middleware: Middleware = { next ->
            { request ->
                request.context.set("customKey", "customValue")
                next(request)
            }
        }
        val handler: RawHandler = { request ->
            val value = request.context.get<String>("customKey")
            Response(HttpStatus.OK, body = Body.Strict("Value: $value".toByteArray()))
        }

        val composed = composeMiddlewares(listOf(middleware), handler)
        val response = composed(req())

        val body = response.body
        check(body is Body.Strict)
        assertEquals("Value: customValue", body.bytes.decodeToString())
    }

    @Test
    fun `should let each middleware contribute its own response headers`() = runTest {
        val m1: Middleware = { next ->
            { request ->
                val response = next(request)
                response.copy(headers = response.headers + ("X-M1" to "Added"))
            }
        }
        val m2: Middleware = { next ->
            { request ->
                val response = next(request)
                response.copy(headers = response.headers + ("X-M2" to "Added"))
            }
        }
        val handler: RawHandler = { Response(HttpStatus.OK) }

        val composed = composeMiddlewares(listOf(m1, m2), handler)
        val response = composed(req())

        assertEquals("Added", response.headers["X-M1"])
        assertEquals("Added", response.headers["X-M2"])
    }

    @Test
    fun `should let middlewares cooperatively read and update the request context`() = runTest {
        val m1: Middleware = { next ->
            { request ->
                request.context.set("counter", 1)
                next(request)
            }
        }
        val m2: Middleware = { next ->
            { request ->
                val counter = request.context.get<Int>("counter") ?: 0
                request.context.set("counter", counter + 1)
                next(request)
            }
        }
        val handler: RawHandler = { request ->
            val counter = request.context.get<Int>("counter") ?: 0
            Response(HttpStatus.OK, body = Body.Strict("Counter: $counter".toByteArray()))
        }

        val composed = composeMiddlewares(listOf(m1, m2), handler)
        val response = composed(req())

        val body = response.body
        check(body is Body.Strict)
        assertEquals("Counter: 2", body.bytes.decodeToString())
    }

    @Test
    fun `should propagate values set by middlewares into suspending handlers`() = runTest {
        val middleware: Middleware = { next ->
            { request ->
                request.context.set("middleware", "executed")
                next(request)
            }
        }
        val handler: RawHandler = { request ->
            kotlinx.coroutines.delay(10)
            val value = request.context.get<String>("middleware")
            Response(HttpStatus.OK, body = Body.Strict("Async: $value".toByteArray()))
        }

        val composed = composeMiddlewares(listOf(middleware), handler)
        val response = composed(req())

        val body = response.body
        check(body is Body.Strict)
        assertEquals("Async: executed", body.bytes.decodeToString())
    }
}
