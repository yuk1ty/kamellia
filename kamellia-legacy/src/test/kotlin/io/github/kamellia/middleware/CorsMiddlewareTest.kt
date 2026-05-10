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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CorsMiddlewareTest {
    @Test
    fun testPreflightRequest() = runTest {
        val cors = corsMiddleware()
        val handler: Handler = { Response.ok() }
        val middleware = cors(handler)

        val request =
            Request(
                method = HttpMethod.OPTIONS,
                path = "/api/test",
                headers = emptyMap(),
                queryParams = QueryParams(emptyMap()),
                pathParams = PathParams(emptyMap()),
                body = Body.Empty,
                context = Context(),
            )

        val response = middleware(request)

        assertEquals(HttpStatus.OK, response.status)
        assertTrue(response.headers.containsKey("Access-Control-Allow-Origin"))
        assertTrue(response.headers.containsKey("Access-Control-Allow-Methods"))
        assertTrue(response.headers.containsKey("Access-Control-Allow-Headers"))
    }

    @Test
    fun testRegularRequestGetsCorsHeaders() = runTest {
        val cors = corsMiddleware()
        val handler: Handler = { Response.ok("Hello") }
        val middleware = cors(handler)

        val request = Request(
            method = HttpMethod.GET,
            path = "/api/test",
            headers = emptyMap(),
            queryParams = QueryParams.empty(),
            pathParams = PathParams.empty(),
            body = Body.Empty,
            context = Context(),
        )
        val response = middleware(request)

        assertEquals(HttpStatus.OK, response.status)
        assertEquals(Body.Text("Hello"), response.body)
        assertTrue(response.headers.containsKey("Access-Control-Allow-Origin"))
        assertTrue(response.headers.containsKey("Access-Control-Allow-Methods"))
    }

    @Test
    fun testCustomConfigurationRespected() = runTest {
        val config =
            CorsConfig(
                allowedOrigins = listOf("https://example.com"),
                allowedMethods = listOf("GET", "POST"),
                allowedHeaders = listOf("X-Custom-Header"),
                maxAge = 7200,
            )

        val cors = corsMiddleware(config)
        val handler: Handler = { Response.ok() }
        val middleware = cors(handler)

        val request = Request(
            method = HttpMethod.GET,
            path = "/",
            headers = emptyMap(),
            queryParams = QueryParams.empty(),
            pathParams = PathParams.empty(),
            body = Body.Empty,
            context = Context(),
        )
        val response = middleware(request)

        assertEquals("https://example.com", response.headers["Access-Control-Allow-Origin"])
        assertEquals("GET,POST", response.headers["Access-Control-Allow-Methods"])
        assertEquals("X-Custom-Header", response.headers["Access-Control-Allow-Headers"])
        assertEquals("7200", response.headers["Access-Control-Max-Age"])
    }

    @Test
    fun testAllowCredentialsFlag() = runTest {
        val config = CorsConfig(allowCredentials = true)
        val cors = corsMiddleware(config)
        val handler: Handler = { Response.ok() }
        val middleware = cors(handler)

        val request = Request(
            method = HttpMethod.GET,
            path = "/",
            headers = emptyMap(),
            queryParams = QueryParams.empty(),
            pathParams = PathParams.empty(),
            body = Body.Empty,
            context = Context(),
        )
        val response = middleware(request)

        assertEquals("true", response.headers["Access-Control-Allow-Credentials"])
    }

    @Test
    fun testAllowCredentialsFlagDisabled() = runTest {
        val config = CorsConfig(allowCredentials = false)
        val cors = corsMiddleware(config)
        val handler: Handler = { Response.ok() }
        val middleware = cors(handler)

        val request = Request(
            method = HttpMethod.GET,
            path = "/",
            headers = emptyMap(),
            queryParams = QueryParams.empty(),
            pathParams = PathParams.empty(),
            body = Body.Empty,
            context = Context(),
        )
        val response = middleware(request)

        assertTrue(!response.headers.containsKey("Access-Control-Allow-Credentials"))
    }

    @Test
    fun testMultipleOriginsWithMatchingOrigin() = runTest {
        val config =
            CorsConfig(
                allowedOrigins = listOf("https://example.com", "https://api.example.com"),
            )

        val cors = corsMiddleware(config)
        val handler: Handler = { Response.ok() }
        val middleware = cors(handler)

        // Request with Origin header matching one of the allowed origins
        val request = Request(
            method = HttpMethod.GET,
            path = "/",
            headers = mapOf("origin" to "https://api.example.com"),
            queryParams = QueryParams.empty(),
            pathParams = PathParams.empty(),
            body = Body.Empty,
            context = Context(),
        )
        val response = middleware(request)

        // Should return the matching origin
        assertEquals(
            "https://api.example.com",
            response.headers["Access-Control-Allow-Origin"],
        )
    }

    @Test
    fun testMultipleOriginsWithoutMatchingOrigin() = runTest {
        val config =
            CorsConfig(
                allowedOrigins = listOf("https://example.com", "https://api.example.com"),
            )

        val cors = corsMiddleware(config)
        val handler: Handler = { Response.ok() }
        val middleware = cors(handler)

        // Request without Origin header or with non-matching origin
        val request = Request(
            method = HttpMethod.GET,
            path = "/",
            headers = emptyMap(),
            queryParams = QueryParams.empty(),
            pathParams = PathParams.empty(),
            body = Body.Empty,
            context = Context(),
        )
        val response = middleware(request)

        // Should return the first allowed origin as fallback
        assertEquals(
            "https://example.com",
            response.headers["Access-Control-Allow-Origin"],
        )
    }

    @Test
    fun testExposedHeaders() = runTest {
        val config =
            CorsConfig(
                exposedHeaders = listOf("X-Total-Count", "X-Page-Number"),
            )

        val cors = corsMiddleware(config)
        val handler: Handler = { Response.ok() }
        val middleware = cors(handler)

        val request = Request(
            method = HttpMethod.GET,
            path = "/",
            headers = emptyMap(),
            queryParams = QueryParams.empty(),
            pathParams = PathParams.empty(),
            body = Body.Empty,
            context = Context(),
        )
        val response = middleware(request)

        assertEquals(
            "X-Total-Count,X-Page-Number",
            response.headers["Access-Control-Expose-Headers"],
        )
    }

    @Test
    fun testExposedHeadersEmpty() = runTest {
        val config = CorsConfig(exposedHeaders = emptyList())
        val cors = corsMiddleware(config)
        val handler: Handler = { Response.ok() }
        val middleware = cors(handler)

        val request = Request(
            method = HttpMethod.GET,
            path = "/",
            headers = emptyMap(),
            queryParams = QueryParams.empty(),
            pathParams = PathParams.empty(),
            body = Body.Empty,
            context = Context(),
        )
        val response = middleware(request)

        assertTrue(!response.headers.containsKey("Access-Control-Expose-Headers"))
    }

    @Test
    fun testDefaultConfiguration() = runTest {
        val cors = corsMiddleware()
        val handler: Handler = { Response.ok() }
        val middleware = cors(handler)

        val request = Request(
            method = HttpMethod.GET,
            path = "/",
            headers = emptyMap(),
            queryParams = QueryParams.empty(),
            pathParams = PathParams.empty(),
            body = Body.Empty,
            context = Context(),
        )
        val response = middleware(request)

        assertEquals("*", response.headers["Access-Control-Allow-Origin"])
        assertEquals(
            "GET,POST,PUT,DELETE,PATCH,OPTIONS",
            response.headers["Access-Control-Allow-Methods"],
        )
        assertEquals("Content-Type,Authorization", response.headers["Access-Control-Allow-Headers"])
        assertEquals("3600", response.headers["Access-Control-Max-Age"])
    }
}
