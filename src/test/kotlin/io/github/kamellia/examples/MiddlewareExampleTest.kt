package io.github.kamellia.examples

import io.github.kamellia.Kamellia
import io.github.kamellia.core.Response
import io.github.kamellia.middleware.CorsConfig
import io.github.kamellia.middleware.LoggingConfig
import io.github.kamellia.middleware.corsMiddleware
import io.github.kamellia.middleware.loggingMiddleware
import kotlin.test.Test

/**
 * Examples demonstrating middleware usage in Kamellia
 *
 * These examples show common patterns and best practices for using middlewares.
 * Note: These tests demonstrate API usage but don't actually start the server.
 */
class MiddlewareExampleTest {
    @Test
    fun exampleBasicLoggingMiddleware() {
        val app = Kamellia()

        // Add logging middleware (logs all requests)
        app.use(loggingMiddleware())

        // Add routes
        app.get("/api/users") { req ->
            Response.ok("Users list")
        }

        // When started, this will log:
        // [GET] /api/users
        // Response: 200 OK (Xms)
    }

    @Test
    fun exampleLoggingWithConfiguration() {
        val app = Kamellia()

        // Add logging middleware with custom configuration
        app.use(
            loggingMiddleware(
                LoggingConfig(
                    logRequestBody = true,
                    logResponseBody = true,
                ),
            ),
        )

        app.post("/api/users") { req ->
            Response.ok("User created")
        }

        // Logs request and response bodies in addition to basic info
    }

    @Test
    fun exampleBasicCorsMiddleware() {
        val app = Kamellia()

        // Add CORS middleware with default settings
        app.use(corsMiddleware())

        app.get("/api/data") { req ->
            Response.ok("Data")
        }

        // All responses will include CORS headers:
        // Access-Control-Allow-Origin: *
        // Access-Control-Allow-Methods: GET,POST,PUT,DELETE,PATCH,OPTIONS
        // etc.
    }

    @Test
    fun exampleCorsWithCustomConfiguration() {
        val app = Kamellia()

        // Add CORS middleware with custom configuration
        app.use(
            corsMiddleware(
                CorsConfig(
                    allowedOrigins = listOf("https://example.com", "https://app.example.com"),
                    allowedMethods = listOf("GET", "POST", "PUT", "DELETE"),
                    allowedHeaders = listOf("Content-Type", "Authorization", "X-API-Key"),
                    exposedHeaders = listOf("X-Total-Count", "X-Page-Number"),
                    allowCredentials = true,
                    maxAge = 7200,
                ),
            ),
        )

        app.get("/api/users") { req ->
            Response.ok("Users")
        }
    }

    @Test
    fun exampleMultipleMiddlewares() {
        val app = Kamellia()

        // Middlewares are executed in the order they are registered
        // Request → Logging → CORS → Handler → CORS → Logging → Response

        // 1. Logging middleware (outermost - executes first and last)
        app.use(loggingMiddleware())

        // 2. CORS middleware
        app.use(corsMiddleware())

        // 3. Routes
        app.get("/api/users") { req ->
            Response.ok("Users list")
        }

        app.post("/api/users") { req ->
            Response.ok("User created")
        }
    }

    @Test
    fun exampleCustomMiddleware() {
        val app = Kamellia()

        // Custom middleware to add a custom header
        app.use { next ->
            { req ->
                val response = next(req)
                response.copy(
                    headers = response.headers + ("X-Custom-Header" to "MyValue"),
                )
            }
        }

        // Custom middleware for authentication
        app.use { next ->
            { req ->
                val token = req.headers["Authorization"]
                if (token != null) {
                    // Store user info in context
                    req.context.set("authenticated", true)
                    req.context.set("userId", "12345")
                }
                next(req)
            }
        }

        // Routes can access context set by middlewares
        app.get("/api/profile") { req ->
            val authenticated = req.context.get<Boolean>("authenticated") ?: false
            if (authenticated) {
                val userId = req.context.get<String>("userId")
                Response.ok("Profile for user: $userId")
            } else {
                Response.unauthorized("Not authenticated")
            }
        }
    }

    @Test
    fun exampleErrorHandlingMiddleware() {
        val app = Kamellia()

        // Error handling middleware (should be first)
        app.use { next ->
            { req ->
                try {
                    next(req)
                } catch (e: IllegalArgumentException) {
                    Response.badRequest("Invalid input: ${e.message}")
                } catch (e: Exception) {
                    Response.internalServerError("Error: ${e.message}")
                }
            }
        }

        app.get("/api/risky") { req ->
            // Any errors thrown here will be caught by the middleware
            throw IllegalArgumentException("Bad data")
        }
    }

    @Test
    fun exampleTimingMiddleware() {
        val app = Kamellia()

        // Timing middleware that adds response time header
        app.use { next ->
            { req ->
                val startTime = System.currentTimeMillis()
                val response = next(req)
                val duration = System.currentTimeMillis() - startTime

                response.copy(
                    headers = response.headers + ("X-Response-Time" to "${duration}ms"),
                )
            }
        }

        app.get("/api/data") { req ->
            Response.ok("Data")
        }
    }

    @Test
    fun exampleRequestIdMiddleware() {
        val app = Kamellia()

        // Request ID middleware for tracking requests
        app.use { next ->
            { req ->
                val requestId = req.headers["X-Request-ID"] ?: java.util.UUID.randomUUID().toString()
                req.context.set("requestId", requestId)

                val response = next(req)
                response.copy(
                    headers = response.headers + ("X-Request-ID" to requestId),
                )
            }
        }

        app.get("/api/users") { req ->
            val requestId = req.context.get<String>("requestId")
            println("Processing request: $requestId")
            Response.ok("Users")
        }
    }

    @Test
    fun exampleConditionalMiddleware() {
        val app = Kamellia()

        // Middleware that only applies to certain paths
        app.use { next ->
            { req ->
                if (req.path.startsWith("/api/admin")) {
                    // Check admin authentication
                    val isAdmin = req.context.get<Boolean>("isAdmin") ?: false
                    if (!isAdmin) {
                        Response.forbidden("Admin access required")
                    } else {
                        next(req)
                    }
                } else {
                    next(req)
                }
            }
        }

        app.get("/api/users") { req ->
            Response.ok("Public endpoint")
        }

        app.get("/api/admin/settings") { req ->
            Response.ok("Admin settings")
        }
    }

    @Test
    fun exampleCompleteApplication() {
        val app = Kamellia()

        // 1. Error handling (first - catches all errors)
        app.use { next ->
            { req ->
                try {
                    next(req)
                } catch (e: Exception) {
                    println("Error handling request: ${e.message}")
                    Response.internalServerError("Internal server error")
                }
            }
        }

        // 2. Request ID tracking
        app.use { next ->
            { req ->
                val requestId = java.util.UUID.randomUUID().toString()
                req.context.set("requestId", requestId)
                val response = next(req)
                response.copy(headers = response.headers + ("X-Request-ID" to requestId))
            }
        }

        // 3. Logging
        app.use(loggingMiddleware())

        // 4. CORS
        app.use(
            corsMiddleware(
                CorsConfig(
                    allowedOrigins = listOf("https://example.com"),
                    allowCredentials = true,
                ),
            ),
        )

        // 5. Authentication
        app.use { next ->
            { req ->
                val token = req.headers["Authorization"]
                if (token != null && token.startsWith("Bearer ")) {
                    req.context.set("authenticated", true)
                }
                next(req)
            }
        }

        // Routes
        app.get("/api/public") { req ->
            Response.ok("Public data")
        }

        app.get("/api/private") { req ->
            val authenticated = req.context.get<Boolean>("authenticated") ?: false
            if (authenticated) {
                Response.ok("Private data")
            } else {
                Response.unauthorized("Authentication required")
            }
        }

        app.post("/api/users") { req ->
            Response.ok("User created")
        }

        // app.start(8080)
    }
}
