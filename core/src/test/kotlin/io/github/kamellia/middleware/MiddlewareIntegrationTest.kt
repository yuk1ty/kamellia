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
import io.github.kamellia.routing.Router
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class IntegrationTestException(message: String) : Exception(message)

class MiddlewareIntegrationTest {
    private fun req(method: HttpMethod = HttpMethod.GET, path: String = "/"): Request = Request(
        method = method,
        path = path,
        headers = emptyMap(),
        queryParams = QueryParams.empty(),
        pathParams = PathParams.empty(),
        body = Body.Empty,
        context = Context(),
    )

    @Test
    fun `should run middlewares before and after handler for a matched route`() = runTest {
        val executionLog = mutableListOf<String>()

        val m1: Middleware = { next ->
            { request ->
                executionLog.add("m1-before")
                val response = next(request)
                executionLog.add("m1-after")
                response
            }
        }
        val m2: Middleware = { next ->
            { request ->
                executionLog.add("m2-before")
                val response = next(request)
                executionLog.add("m2-after")
                response
            }
        }
        val handler: RawHandler = { request ->
            executionLog.add("handler")
            Response(
                status = HttpStatus.OK,
                body = Body.Strict("Hello ${request.pathParams.string("name")}".toByteArray()),
            )
        }

        val router = Router()
        router.addRoute(HttpMethod.GET, "/users/:name", handler)
        val routeMatch = router.match(req(HttpMethod.GET, "/users/john"))!!

        val composed = composeMiddlewares(listOf(m1, m2), routeMatch.handler)
        val response = composed(req(HttpMethod.GET, "/users/john").copy(pathParams = routeMatch.pathParams))

        val body = response.body
        check(body is Body.Strict)
        assertEquals("Hello john", body.bytes.decodeToString())
        assertEquals(
            listOf("m1-before", "m2-before", "handler", "m2-after", "m1-after"),
            executionLog,
        )
    }

    @Test
    fun `should let each of three composed middlewares contribute its own response header`() = runTest {
        val m1: Middleware = { next ->
            { request ->
                val response = next(request)
                response.copy(headers = response.headers + ("X-M1" to "added"))
            }
        }
        val m2: Middleware = { next ->
            { request ->
                val response = next(request)
                response.copy(headers = response.headers + ("X-M2" to "added"))
            }
        }
        val m3: Middleware = { next ->
            { request ->
                val response = next(request)
                response.copy(headers = response.headers + ("X-M3" to "added"))
            }
        }
        val handler: RawHandler = { Response(HttpStatus.OK, body = Body.Strict("Test".toByteArray())) }

        val composed = composeMiddlewares(listOf(m1, m2, m3), handler)
        val response = composed(req())

        assertEquals("added", response.headers["X-M1"])
        assertEquals("added", response.headers["X-M2"])
        assertEquals("added", response.headers["X-M3"])
    }

    @Test
    fun `should share context state across middlewares and visible to the handler`() = runTest {
        val m1: Middleware = { next ->
            { request ->
                request.context.set("userId", "12345")
                next(request)
            }
        }
        val m2: Middleware = { next ->
            { request ->
                val userId = request.context.get<String>("userId")
                request.context.set("userName", "User_$userId")
                next(request)
            }
        }
        val handler: RawHandler = { request ->
            val userName = request.context.get<String>("userName")
            Response(HttpStatus.OK, body = Body.Strict("Welcome $userName".toByteArray()))
        }

        val composed = composeMiddlewares(listOf(m1, m2), handler)
        val response = composed(req())

        val body = response.body
        check(body is Body.Strict)
        assertEquals("Welcome User_12345", body.bytes.decodeToString())
    }

    @Test
    fun `should expose path parameters and context values together to the handler`() = runTest {
        val loggingMiddleware: Middleware = { next ->
            { request ->
                request.context.set("logged", true)
                next(request)
            }
        }
        val handler: RawHandler = { request ->
            val id = request.pathParams.string("id")
            val logged = request.context.get<Boolean>("logged")
            Response(HttpStatus.OK, body = Body.Strict("ID: $id, Logged: $logged".toByteArray()))
        }

        val router = Router()
        router.addRoute(HttpMethod.GET, "/items/:id", handler)
        val routeMatch = router.match(req(HttpMethod.GET, "/items/42"))!!

        val composed = composeMiddlewares(listOf(loggingMiddleware), routeMatch.handler)
        val response = composed(req(HttpMethod.GET, "/items/42").copy(pathParams = routeMatch.pathParams))

        val body = response.body
        check(body is Body.Strict)
        assertEquals("ID: 42, Logged: true", body.bytes.decodeToString())
    }

    @Test
    fun `should allow middleware to catch exceptions thrown by handlers and convert them to a response`() = runTest {
        val errorHandlingMiddleware: Middleware = { next ->
            { request ->
                try {
                    next(request)
                } catch (e: IntegrationTestException) {
                    Response(
                        status = HttpStatus.INTERNAL_SERVER_ERROR,
                        body = Body.Strict("Caught: ${e.message}".toByteArray()),
                    )
                }
            }
        }
        val handler: RawHandler = { throw IntegrationTestException("Test error") }

        val composed = composeMiddlewares(listOf(errorHandlingMiddleware), handler)
        val response = composed(req())

        val body = response.body
        check(body is Body.Strict)
        assertEquals("Caught: Test error", body.bytes.decodeToString())
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.status)
    }

    @Test
    fun `should combine a context-setting middleware with the CORS middleware seamlessly`() = runTest {
        val cors = corsMiddleware()
        val logging: Middleware = { next ->
            { request ->
                request.context.set("logged", true)
                next(request)
            }
        }
        val handler: RawHandler = { request ->
            val logged = request.context.get<Boolean>("logged")
            Response(HttpStatus.OK, body = Body.Strict("Logged: $logged".toByteArray()))
        }

        val composed = composeMiddlewares(listOf(logging, cors), handler)
        val response = composed(req())

        val body = response.body
        check(body is Body.Strict)
        assertEquals("Logged: true", body.bytes.decodeToString())
        assertTrue(response.headers.containsKey("Access-Control-Allow-Origin"))
    }

    @Test
    fun `should still apply middleware decorations when the handler returns a 404 response`() = runTest {
        val middleware: Middleware = { next ->
            { request ->
                val response = next(request)
                response.copy(headers = response.headers + ("X-Middleware" to "applied"))
            }
        }
        val notFoundHandler: RawHandler = {
            Response(status = HttpStatus.NOT_FOUND, body = Body.Strict("Not found".toByteArray()))
        }

        val composed = composeMiddlewares(listOf(middleware), notFoundHandler)
        val response = composed(req(HttpMethod.GET, "/nonexistent"))

        val body = response.body
        check(body is Body.Strict)
        assertEquals("Not found", body.bytes.decodeToString())
        assertEquals("applied", response.headers["X-Middleware"])
    }
}
