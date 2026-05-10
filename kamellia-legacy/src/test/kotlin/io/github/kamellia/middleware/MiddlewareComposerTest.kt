package io.github.kamellia.middleware

import io.github.kamellia.core.Body
import io.github.kamellia.core.Context
import io.github.kamellia.core.Handler
import io.github.kamellia.core.HttpMethod
import io.github.kamellia.core.PathParams
import io.github.kamellia.core.QueryParams
import io.github.kamellia.core.Request
import io.github.kamellia.core.Response
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MiddlewareComposerTest {
    @Test
    fun testEmptyMiddlewareList() = runTest {
        val handler: Handler = { Response.ok("Hello") }
        val composed = composeMiddlewares(emptyList(), handler)

        val request = Request(
            method = HttpMethod.GET,
            path = "/",
            headers = emptyMap(),
            queryParams = QueryParams.empty(),
            pathParams = PathParams.empty(),
            body = Body.Empty,
            context = Context(),
        )
        val response = composed(request)

        assertEquals(Response.ok("Hello"), response)
    }

    @Test
    fun testSingleMiddleware() = runTest {
        val middleware: Middleware = { next ->
            { req ->
                val response = next(req)
                response.copy(
                    headers = response.headers + ("X-Custom" to "Value"),
                )
            }
        }

        val handler: Handler = { Response.ok("Hello") }
        val composed = composeMiddlewares(listOf(middleware), handler)

        val request = Request(
            method = HttpMethod.GET,
            path = "/",
            headers = emptyMap(),
            queryParams = QueryParams.empty(),
            pathParams = PathParams.empty(),
            body = Body.Empty,
            context = Context(),
        )
        val response = composed(request)

        assertEquals("Value", response.headers["X-Custom"])
        assertEquals(Body.Text("Hello"), response.body)
    }

    @Test
    fun testMiddlewareExecutionOrder() = runTest {
        val executionOrder = mutableListOf<String>()

        val m1: Middleware = { next ->
            { req ->
                executionOrder.add("m1-before")
                val response = next(req)
                executionOrder.add("m1-after")
                response
            }
        }

        val m2: Middleware = { next ->
            { req ->
                executionOrder.add("m2-before")
                val response = next(req)
                executionOrder.add("m2-after")
                response
            }
        }

        val m3: Middleware = { next ->
            { req ->
                executionOrder.add("m3-before")
                val response = next(req)
                executionOrder.add("m3-after")
                response
            }
        }

        val handler: Handler = {
            executionOrder.add("handler")
            Response.ok()
        }

        val composed = composeMiddlewares(listOf(m1, m2, m3), handler)
        composed(
            Request(
                method = HttpMethod.GET,
                path = "/",
                headers = emptyMap(),
                queryParams = QueryParams.empty(),
                pathParams = PathParams.empty(),
                body = Body.Empty,
                context = Context(),
            ),
        )

        assertEquals(
            listOf("m1-before", "m2-before", "m3-before", "handler", "m3-after", "m2-after", "m1-after"),
            executionOrder,
        )
    }

    @Test
    fun testMiddlewareCanModifyRequest() = runTest {
        val middleware: Middleware = { next ->
            { req ->
                // Add custom header to request context
                req.context.set("customKey", "customValue")
                next(req)
            }
        }

        val handler: Handler = { req ->
            val value = req.context.get<String>("customKey")
            Response.ok("Value: $value")
        }

        val composed = composeMiddlewares(listOf(middleware), handler)
        val response = composed(
            Request(
                method = HttpMethod.GET,
                path = "/",
                headers = emptyMap(),
                queryParams = QueryParams.empty(),
                pathParams = PathParams.empty(),
                body = Body.Empty,
                context = Context(),
            ),
        )

        assertEquals(Body.Text("Value: customValue"), response.body)
    }

    @Test
    fun testMiddlewareCanModifyResponse() = runTest {
        val m1: Middleware = { next ->
            { req ->
                val response = next(req)
                response.copy(
                    headers = response.headers + ("X-M1" to "Added"),
                )
            }
        }

        val m2: Middleware = { next ->
            { req ->
                val response = next(req)
                response.copy(
                    headers = response.headers + ("X-M2" to "Added"),
                )
            }
        }

        val handler: Handler = { Response.ok("Hello") }

        val composed = composeMiddlewares(listOf(m1, m2), handler)
        val response = composed(
            Request(
                method = HttpMethod.GET,
                path = "/",
                headers = emptyMap(),
                queryParams = QueryParams.empty(),
                pathParams = PathParams.empty(),
                body = Body.Empty,
                context = Context(),
            ),
        )

        assertEquals("Added", response.headers["X-M1"])
        assertEquals("Added", response.headers["X-M2"])
    }

    @Test
    fun testMiddlewareCanAccessAndModifyContext() = runTest {
        val m1: Middleware = { next ->
            { req ->
                req.context.set("counter", 1)
                next(req)
            }
        }

        val m2: Middleware = { next ->
            { req ->
                val counter = req.context.get<Int>("counter") ?: 0
                req.context.set("counter", counter + 1)
                next(req)
            }
        }

        val handler: Handler = { req ->
            val counter = req.context.get<Int>("counter") ?: 0
            Response.ok("Counter: $counter")
        }

        val composed = composeMiddlewares(listOf(m1, m2), handler)
        val response = composed(
            Request(
                method = HttpMethod.GET,
                path = "/",
                headers = emptyMap(),
                queryParams = QueryParams.empty(),
                pathParams = PathParams.empty(),
                body = Body.Empty,
                context = Context(),
            ),
        )

        assertEquals(Body.Text("Counter: 2"), response.body)
    }

    @Test
    fun testMiddlewareChainWithSuspendHandlers() = runTest {
        val middleware: Middleware = { next ->
            { req ->
                req.context.set("middleware", "executed")
                next(req)
            }
        }

        val handler: Handler = { req ->
            // Simulate async operation
            kotlinx.coroutines.delay(10)
            val value = req.context.get<String>("middleware")
            Response.ok("Async: $value")
        }

        val composed = composeMiddlewares(listOf(middleware), handler)
        val response = composed(
            Request(
                method = HttpMethod.GET,
                path = "/",
                headers = emptyMap(),
                queryParams = QueryParams.empty(),
                pathParams = PathParams.empty(),
                body = Body.Empty,
                context = Context(),
            ),
        )

        assertEquals(Body.Text("Async: executed"), response.body)
    }
}
