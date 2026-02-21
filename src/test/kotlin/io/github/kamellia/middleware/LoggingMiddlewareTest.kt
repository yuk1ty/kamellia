package io.github.kamellia.middleware

import io.github.kamellia.core.Body
import io.github.kamellia.core.Context
import io.github.kamellia.core.Handler
import io.github.kamellia.core.HttpMethod
import io.github.kamellia.core.HttpStatus
import io.github.kamellia.core.PathParams
import io.github.kamellia.core.QueryParams
import io.github.kamellia.core.Request
import io.github.kamellia.core.Response
import kotlinx.coroutines.test.runTest
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LoggingMiddlewareTest {
    @Test
    fun testLogsRequestMethodAndPath() = runTest {
        val output =
            captureStdout {
                val middleware = loggingMiddleware()
                val handler: Handler = { Response.ok() }
                val composed = middleware(handler)

                val request =
                    Request(
                        method = HttpMethod.GET,
                        path = "/test",
                        headers = emptyMap(),
                        queryParams = QueryParams.empty(),
                        pathParams = PathParams.empty(),
                        body = Body.Empty,
                        context = Context(),
                    )
                composed(request)
            }

        assertTrue(output.contains("[GET] /test"))
    }

    @Test
    fun testLogsResponseStatus() = runTest {
        val output =
            captureStdout {
                val middleware = loggingMiddleware()
                val handler: Handler = { Response(HttpStatus.CREATED) }
                val composed = middleware(handler)

                val request =
                    Request(
                        method = HttpMethod.POST,
                        path = "/api/users",
                        headers = emptyMap(),
                        queryParams = QueryParams.empty(),
                        pathParams = PathParams.empty(),
                        body = Body.Empty,
                        context = Context(),
                    )
                composed(request)
            }

        assertTrue(output.contains("Response: 201 CREATED"))
    }

    @Test
    fun testLogsDuration() = runTest {
        val output =
            captureStdout {
                val middleware = loggingMiddleware()
                val handler: Handler = {
                    kotlinx.coroutines.delay(10)
                    Response.ok()
                }
                val composed = middleware(handler)

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
                composed(request)
            }

        // Should contain duration in ms
        assertTrue(output.contains("ms)"))
    }

    @Test
    fun testOptionalRequestBodyLogging() = runTest {
        val output =
            captureStdout {
                val middleware = loggingMiddleware(LoggingConfig(logRequestBody = true))
                val handler: Handler = { Response.ok() }
                val composed = middleware(handler)

                val request =
                    Request(
                        method = HttpMethod.POST,
                        path = "/api/users",
                        headers = emptyMap(),
                        queryParams = QueryParams.empty(),
                        pathParams = PathParams.empty(),
                        body = Body.Text("test body"),
                        context = Context(),
                    )
                composed(request)
            }

        assertTrue(output.contains("Request Body:"))
        assertTrue(output.contains("test body"))
    }

    @Test
    fun testOptionalResponseBodyLogging() = runTest {
        val output =
            captureStdout {
                val middleware = loggingMiddleware(LoggingConfig(logResponseBody = true))
                val handler: Handler = { Response.ok("Hello World") }
                val composed = middleware(handler)

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
                composed(request)
            }

        assertTrue(output.contains("Response Body:"))
        assertTrue(output.contains("Hello World"))
    }

    @Test
    fun testDoesNotAffectRequestResponse() = runTest {
        val middleware = loggingMiddleware()
        val handler: Handler = { req ->
            Response.ok("Echo: ${req.path}")
        }
        val composed = middleware(handler)

        val request =
            Request(
                method = HttpMethod.GET,
                path = "/test",
                headers = emptyMap(),
                queryParams = QueryParams.empty(),
                pathParams = PathParams.empty(),
                body = Body.Empty,
                context = Context(),
            )

        val originalOut = System.out
        val outputStream = ByteArrayOutputStream()
        System.setOut(PrintStream(outputStream))

        val response =
            try {
                composed(request)
            } finally {
                System.setOut(originalOut)
            }

        assertEquals(Body.Text("Echo: /test"), response.body)
        assertEquals(HttpStatus.OK, response.status)
    }

    @Test
    fun testWorksWithAsyncHandlers() = runTest {
        val middleware = loggingMiddleware()
        val handler: Handler = {
            kotlinx.coroutines.delay(10)
            Response.ok("Async result")
        }
        val composed = middleware(handler)

        val request =
            Request(
                method = HttpMethod.GET,
                path = "/async",
                headers = emptyMap(),
                queryParams = QueryParams.empty(),
                pathParams = PathParams.empty(),
                body = Body.Empty,
                context = Context(),
            )

        val originalOut = System.out
        val outputStream = ByteArrayOutputStream()
        System.setOut(PrintStream(outputStream))

        val response =
            try {
                composed(request)
            } finally {
                System.setOut(originalOut)
            }

        val output = outputStream.toString()
        assertEquals(Body.Text("Async result"), response.body)
        assertTrue(output.contains("[GET] /async"))
        assertTrue(output.contains("Response: 200 OK"))
    }

    /**
     * Captures stdout during block execution and returns the output as a String
     */
    private suspend fun captureStdout(block: suspend () -> Unit): String {
        val originalOut = System.out
        val outputStream = ByteArrayOutputStream()
        System.setOut(PrintStream(outputStream))

        return try {
            block()
            outputStream.toString()
        } finally {
            System.setOut(originalOut)
        }
    }
}
