package io.github.kamellia.middleware

import io.github.kamellia.core.Body
import io.github.kamellia.core.Context
import io.github.kamellia.core.Handler
import io.github.kamellia.core.HttpMethod
import io.github.kamellia.core.PathParams
import io.github.kamellia.core.QueryParams
import io.github.kamellia.core.Request
import io.github.kamellia.core.Response
import io.github.kamellia.routing.Router
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MiddlewareIntegrationTest {
    @Test
    fun testMiddlewareWithRouting() = runTest {
        val executionLog = mutableListOf<String>()

        val middleware1: Middleware = { next ->
            { req ->
                executionLog.add("m1-before")
                val response = next(req)
                executionLog.add("m1-after")
                response
            }
        }

        val middleware2: Middleware = { next ->
            { req ->
                executionLog.add("m2-before")
                val response = next(req)
                executionLog.add("m2-after")
                response
            }
        }

        val handler: Handler = { req ->
            executionLog.add("handler")
            Response.ok("Hello ${req.pathParams.get<String>("name")}")
        }

        val router = Router()
        router.addRoute(HttpMethod.GET, "/users/:name", handler)

        val request =
            Request(
                method = HttpMethod.GET,
                path = "/users/john",
                headers = emptyMap(),
                queryParams = QueryParams(emptyMap()),
                pathParams = PathParams(emptyMap()),
                body = Body.Empty,
                context = Context(),
            )

        val routeMatch = router.match(request)!!
        val composed = composeMiddlewares(listOf(middleware1, middleware2), routeMatch.handler)
        val requestWithParams = request.copy(pathParams = routeMatch.pathParams)
        val response = composed(requestWithParams)

        assertEquals(Body.Text("Hello john"), response.body)
        assertEquals(
            listOf("m1-before", "m2-before", "handler", "m2-after", "m1-after"),
            executionLog,
        )
    }

    @Test
    fun testMultipleMiddlewaresComposeCorrectly() = runTest {
        val m1: Middleware = { next ->
            { req ->
                val response = next(req)
                response.copy(headers = response.headers + ("X-M1" to "added"))
            }
        }

        val m2: Middleware = { next ->
            { req ->
                val response = next(req)
                response.copy(headers = response.headers + ("X-M2" to "added"))
            }
        }

        val m3: Middleware = { next ->
            { req ->
                val response = next(req)
                response.copy(headers = response.headers + ("X-M3" to "added"))
            }
        }

        val handler: Handler = { Response.ok("Test") }

        val composed = composeMiddlewares(listOf(m1, m2, m3), handler)
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

        assertEquals("added", response.headers["X-M1"])
        assertEquals("added", response.headers["X-M2"])
        assertEquals("added", response.headers["X-M3"])
    }

    @Test
    fun testContextSharingBetweenMiddlewares() = runTest {
        val m1: Middleware = { next ->
            { req ->
                req.context.set("userId", "12345")
                next(req)
            }
        }

        val m2: Middleware = { next ->
            { req ->
                val userId = req.context.get<String>("userId")
                req.context.set("userName", "User_$userId")
                next(req)
            }
        }

        val handler: Handler = { req ->
            val userName = req.context.get<String>("userName")
            Response.ok("Welcome $userName")
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

        assertEquals(Body.Text("Welcome User_12345"), response.body)
    }

    @Test
    fun testMiddlewareAndPathParametersWorkTogether() = runTest {
        val loggingMiddleware: Middleware = { next ->
            { req ->
                req.context.set("logged", true)
                next(req)
            }
        }

        val handler: Handler = { req ->
            val id = req.pathParams.get<String>("id")
            val logged = req.context.get<Boolean>("logged")
            Response.ok("ID: $id, Logged: $logged")
        }

        val router = Router()
        router.addRoute(HttpMethod.GET, "/items/:id", handler)

        val request = Request(
            method = HttpMethod.GET,
            path = "/items/42",
            headers = emptyMap(),
            queryParams = QueryParams.empty(),
            pathParams = PathParams.empty(),
            body = Body.Empty,
            context = Context(),
        )
        val routeMatch = router.match(request)!!

        val composed = composeMiddlewares(listOf(loggingMiddleware), routeMatch.handler)
        val requestWithParams = request.copy(pathParams = routeMatch.pathParams)
        val response = composed(requestWithParams)

        assertEquals(Body.Text("ID: 42, Logged: true"), response.body)
    }

    @Test
    fun testErrorHandlingThroughMiddlewareChain() = runTest {
        val errorHandlingMiddleware: Middleware = { next ->
            { req ->
                try {
                    next(req)
                } catch (e: Exception) {
                    Response.internalServerError("Caught: ${e.message}")
                }
            }
        }

        val handler: Handler = {
            throw RuntimeException("Test error")
        }

        val composed = composeMiddlewares(listOf(errorHandlingMiddleware), handler)
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

        assertEquals(Body.Text("Caught: Test error"), response.body)
    }

    @Test
    fun testLoggingAndCorsMiddlewaresTogether() = runTest {
        val cors = corsMiddleware()
        val logging: Middleware = { next ->
            { req ->
                req.context.set("logged", true)
                next(req)
            }
        }

        val handler: Handler = { req ->
            val logged = req.context.get<Boolean>("logged")
            Response.ok("Logged: $logged")
        }

        val composed = composeMiddlewares(listOf(logging, cors), handler)
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

        assertEquals(Body.Text("Logged: true"), response.body)
        assertTrue(response.headers.containsKey("Access-Control-Allow-Origin"))
    }

    @Test
    fun testMiddlewareWith404Route() = runTest {
        val middleware: Middleware = { next ->
            { req ->
                val response = next(req)
                response.copy(headers = response.headers + ("X-Middleware" to "applied"))
            }
        }

        val notFoundHandler: Handler = {
            Response.notFound("Not found")
        }

        val composed = composeMiddlewares(listOf(middleware), notFoundHandler)
        val response = composed(
            Request(
                method = HttpMethod.GET,
                path = "/nonexistent",
                headers = emptyMap(),
                queryParams = QueryParams.empty(),
                pathParams = PathParams.empty(),
                body = Body.Empty,
                context = Context(),
            ),
        )

        assertEquals(Body.Text("Not found"), response.body)
        assertEquals("applied", response.headers["X-Middleware"])
    }
}
