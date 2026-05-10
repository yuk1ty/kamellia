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
import kotlin.test.assertTrue

class CorsMiddlewareTest {
    private fun req(
        method: HttpMethod = HttpMethod.GET,
        path: String = "/",
        headers: Map<String, String> = emptyMap(),
    ): Request = Request(
        method = method,
        path = path,
        headers = headers,
        queryParams = QueryParams.empty(),
        pathParams = PathParams.empty(),
        body = Body.Empty,
        context = Context(),
    )

    @Test
    fun `should respond OK with CORS headers when method is OPTIONS`() = runTest {
        val cors = corsMiddleware()
        val handler: RawHandler = { Response(HttpStatus.OK) }
        val middleware = cors(handler)

        val response = middleware(req(method = HttpMethod.OPTIONS, path = "/api/test"))

        assertEquals(HttpStatus.OK, response.status)
        assertTrue(response.headers.containsKey("Access-Control-Allow-Origin"))
        assertTrue(response.headers.containsKey("Access-Control-Allow-Methods"))
        assertTrue(response.headers.containsKey("Access-Control-Allow-Headers"))
    }

    @Test
    fun `should add CORS headers to non-preflight responses`() = runTest {
        val cors = corsMiddleware()
        val handler: RawHandler = { Response(HttpStatus.OK, body = Body.Strict("Hello".toByteArray())) }
        val middleware = cors(handler)

        val response = middleware(req(method = HttpMethod.GET, path = "/api/test"))

        assertEquals(HttpStatus.OK, response.status)
        val body = response.body
        check(body is Body.Strict)
        assertEquals("Hello", body.bytes.decodeToString())
        assertTrue(response.headers.containsKey("Access-Control-Allow-Origin"))
        assertTrue(response.headers.containsKey("Access-Control-Allow-Methods"))
    }

    @Test
    fun `should respect custom configuration values`() = runTest {
        val config = CorsConfig(
            allowedOrigins = listOf("https://example.com"),
            allowedMethods = listOf("GET", "POST"),
            allowedHeaders = listOf("X-Custom-Header"),
            maxAge = 7200,
        )
        val handler: RawHandler = { Response(HttpStatus.OK) }
        val middleware = corsMiddleware(config)(handler)

        val response = middleware(req())

        assertEquals("https://example.com", response.headers["Access-Control-Allow-Origin"])
        assertEquals("GET,POST", response.headers["Access-Control-Allow-Methods"])
        assertEquals("X-Custom-Header", response.headers["Access-Control-Allow-Headers"])
        assertEquals("7200", response.headers["Access-Control-Max-Age"])
    }

    @Test
    fun `should emit allow-credentials header when credentials are enabled`() = runTest {
        val handler: RawHandler = { Response(HttpStatus.OK) }
        val middleware = corsMiddleware(CorsConfig(allowCredentials = true))(handler)

        val response = middleware(req())

        assertEquals("true", response.headers["Access-Control-Allow-Credentials"])
    }

    @Test
    fun `should omit allow-credentials header when credentials are disabled`() = runTest {
        val handler: RawHandler = { Response(HttpStatus.OK) }
        val middleware = corsMiddleware(CorsConfig(allowCredentials = false))(handler)

        val response = middleware(req())

        assertTrue(!response.headers.containsKey("Access-Control-Allow-Credentials"))
    }

    @Test
    fun `should reflect the request Origin when it is present in the allowed list`() = runTest {
        val config = CorsConfig(
            allowedOrigins = listOf("https://example.com", "https://api.example.com"),
        )
        val handler: RawHandler = { Response(HttpStatus.OK) }
        val middleware = corsMiddleware(config)(handler)

        val response = middleware(req(headers = mapOf("origin" to "https://api.example.com")))

        assertEquals("https://api.example.com", response.headers["Access-Control-Allow-Origin"])
    }

    @Test
    fun `should fall back to the first allowed origin when request Origin does not match`() = runTest {
        val config = CorsConfig(
            allowedOrigins = listOf("https://example.com", "https://api.example.com"),
        )
        val handler: RawHandler = { Response(HttpStatus.OK) }
        val middleware = corsMiddleware(config)(handler)

        val response = middleware(req())

        assertEquals("https://example.com", response.headers["Access-Control-Allow-Origin"])
    }

    @Test
    fun `should expose configured response headers`() = runTest {
        val config = CorsConfig(exposedHeaders = listOf("X-Total-Count", "X-Page-Number"))
        val handler: RawHandler = { Response(HttpStatus.OK) }
        val middleware = corsMiddleware(config)(handler)

        val response = middleware(req())

        assertEquals("X-Total-Count,X-Page-Number", response.headers["Access-Control-Expose-Headers"])
    }

    @Test
    fun `should omit Expose-Headers when no headers are exposed`() = runTest {
        val handler: RawHandler = { Response(HttpStatus.OK) }
        val middleware = corsMiddleware(CorsConfig(exposedHeaders = emptyList()))(handler)

        val response = middleware(req())

        assertTrue(!response.headers.containsKey("Access-Control-Expose-Headers"))
    }

    @Test
    fun `should apply default configuration values when no config is supplied`() = runTest {
        val handler: RawHandler = { Response(HttpStatus.OK) }
        val middleware = corsMiddleware()(handler)

        val response = middleware(req())

        assertEquals("*", response.headers["Access-Control-Allow-Origin"])
        assertEquals(
            "GET,POST,PUT,DELETE,PATCH,OPTIONS",
            response.headers["Access-Control-Allow-Methods"],
        )
        assertEquals("Content-Type,Authorization", response.headers["Access-Control-Allow-Headers"])
        assertEquals("3600", response.headers["Access-Control-Max-Age"])
    }
}
