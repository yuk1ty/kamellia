package io.github.kamellia.middleware

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.github.kamellia.core.Body
import io.github.kamellia.core.Context
import io.github.kamellia.core.HttpMethod
import io.github.kamellia.core.HttpStatus
import io.github.kamellia.core.PathParams
import io.github.kamellia.core.QueryParams
import io.github.kamellia.core.Request
import io.github.kamellia.core.Response
import io.github.kamellia.routing.RawHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.slf4j.LoggerFactory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LoggingMiddlewareTest {
    private lateinit var appender: ListAppender<ILoggingEvent>
    private lateinit var rootLogger: Logger

    @BeforeTest
    fun setUp() {
        rootLogger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
        rootLogger.level = Level.DEBUG
        appender = ListAppender<ILoggingEvent>().also {
            it.context = rootLogger.loggerContext
            it.start()
        }
        rootLogger.addAppender(appender)
    }

    @AfterTest
    fun tearDown() {
        rootLogger.detachAppender(appender)
        appender.stop()
    }

    private fun req(method: HttpMethod = HttpMethod.GET, path: String = "/", body: Body = Body.Empty): Request =
        Request(
            method = method,
            path = path,
            headers = emptyMap(),
            queryParams = QueryParams.empty(),
            pathParams = PathParams.empty(),
            body = body,
            context = Context(),
        )

    private fun messages(): List<String> = appender.list.map { it.formattedMessage }

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

    @Test
    fun `should log the request method and path`() = runTest {
        val handler: RawHandler = { Response(HttpStatus.OK) }
        val middleware = loggingMiddleware()(handler)

        middleware(req(HttpMethod.GET, "/test"))

        assertTrue(messages().any { it.contains("[GET] /test") }, "Expected request line in logs: ${messages()}")
    }

    @Test
    fun `should log the response status`() = runTest {
        val handler: RawHandler = { Response(HttpStatus.CREATED) }
        val middleware = loggingMiddleware()(handler)

        middleware(req(HttpMethod.POST, "/api/users"))

        assertTrue(
            messages().any { it.contains("Response: 201 CREATED") },
            "Expected response status line in logs: ${messages()}",
        )
    }

    @Test
    fun `should log the elapsed time of the handler in milliseconds`() = runTest {
        val handler: RawHandler = {
            delay(10)
            Response(HttpStatus.OK)
        }
        val middleware = loggingMiddleware()(handler)

        middleware(req())

        assertTrue(messages().any { it.contains("ms)") }, "Expected duration to appear in logs: ${messages()}")
    }

    @Test
    fun `should log the request body when logRequestBody is enabled`() = runTest {
        val handler: RawHandler = { Response(HttpStatus.OK) }
        val middleware = loggingMiddleware(LoggingConfig(logRequestBody = true))(handler)

        middleware(req(HttpMethod.POST, "/api/users", body = Body.Strict("test body".toByteArray())))

        assertTrue(messages().any { it.contains("Request Body:") }, "Expected request body log: ${messages()}")
    }

    @Test
    fun `should log the response body when logResponseBody is enabled`() = runTest {
        val handler: RawHandler = { Response(HttpStatus.OK, body = Body.Strict("Hello World".toByteArray())) }
        val middleware = loggingMiddleware(LoggingConfig(logResponseBody = true))(handler)

        middleware(req())

        assertTrue(messages().any { it.contains("Response Body:") }, "Expected response body log: ${messages()}")
    }

    @Test
    fun `should not alter the response body or status produced by the handler`() = runTest {
        val handler: RawHandler = { req ->
            Response(HttpStatus.OK, body = Body.Strict("Echo: ${req.path}".toByteArray()))
        }
        val middleware = loggingMiddleware()(handler)

        val response = middleware(req(HttpMethod.GET, "/test"))

        assertEquals(HttpStatus.OK, response.status)
        val body = response.body
        check(body is Body.Strict)
        assertEquals("Echo: /test", body.bytes.decodeToString())
    }

    @Test
    fun `should work with suspend handlers that perform asynchronous work`() = runTest {
        val handler: RawHandler = {
            delay(10)
            Response(HttpStatus.OK, body = Body.Strict("Async result".toByteArray()))
        }
        val middleware = loggingMiddleware()(handler)

        val response = middleware(req(HttpMethod.GET, "/async"))

        val body = response.body
        check(body is Body.Strict)
        assertEquals("Async result", body.bytes.decodeToString())
        assertTrue(messages().any { it.contains("[GET] /async") })
        assertTrue(messages().any { it.contains("Response: 200 OK") })
    }
}
