package examples

import io.github.kamellia.Kamellia
import io.github.kamellia.core.Response
import io.github.kamellia.core.text
import io.github.kamellia.middleware.CorsConfig
import io.github.kamellia.middleware.LoggingConfig
import io.github.kamellia.middleware.corsMiddleware
import io.github.kamellia.middleware.loggingMiddleware
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.Serializable
import java.util.UUID

private val logger = KotlinLogging.logger {}

private const val DEFAULT_PORT = 3001

@Serializable
data class ApiResponse(val message: String, val requestId: String? = null)

@Serializable
data class UserData(val username: String, val email: String)

suspend fun main() {
    val app = Kamellia()

    logger.info { "Setting up middleware pipeline..." }

    // 1. Error handling middleware (first - catches all errors)
    app.use { next ->
        { req ->
            try {
                next(req)
            } catch (e: IllegalArgumentException) {
                logger.error(e) { "Validation error: ${e.message}" }
                Response.badRequest("Invalid input: ${e.message}")
            } catch (e: Exception) {
                logger.error(e) { "Unexpected error: ${e.message}" }
                Response.internalServerError("Internal server error")
            }
        }
    }

    // 2. Request ID middleware - tracks requests across the application
    app.use { next ->
        { req ->
            val requestId = req.headers["X-Request-ID"] ?: UUID.randomUUID().toString().substring(0, 8)
            req.context.set("requestId", requestId)

            val response = next(req)
            response.copy(
                headers = response.headers + ("X-Request-ID" to requestId),
            )
        }
    }

    // 3. Logging middleware - logs all requests with detailed information
    app.use(
        loggingMiddleware(
            LoggingConfig(
                logRequestBody = true,
                logResponseBody = false,
            ),
        ),
    )

    // 4. CORS middleware - enables cross-origin requests
    app.use(
        corsMiddleware(
            CorsConfig(
                allowedOrigins = listOf("http://localhost:3000", "https://example.com"),
                allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS"),
                allowedHeaders = listOf("Content-Type", "Authorization", "X-Request-ID"),
                exposedHeaders = listOf("X-Request-ID", "X-Response-Time"),
                allowCredentials = true,
                maxAge = 3600,
            ),
        ),
    )

    // 5. Response time middleware - adds performance metrics
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

    // 6. Simple authentication middleware
    app.use { next ->
        { req ->
            val authHeader = req.headers["Authorization"]
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                val token = authHeader.substring(7)
                // In a real app, validate the token here
                if (token.isNotEmpty()) {
                    req.context.set("authenticated", true)
                    req.context.set("userId", "user-${token.take(4)}")
                }
            }
            next(req)
        }
    }

    // 7. Custom header middleware - adds server identification
    app.use { next ->
        { req ->
            val response = next(req)
            response.copy(
                headers = response.headers + ("X-Powered-By" to "Kamellia"),
            )
        }
    }

    // Routes
    app.get("/") { req ->
        val requestId = req.context.get<String>("requestId")
        Response.json(
            ApiResponse(
                message = "Middleware example server running!",
                requestId = requestId,
            ),
        )
    }

    // Public endpoint - no authentication required
    app.get("/api/public") { req ->
        val requestId = req.context.get<String>("requestId")
        Response.json(
            ApiResponse(
                message = "This is a public endpoint accessible to everyone",
                requestId = requestId,
            ),
        )
    }

    // Protected endpoint - requires authentication
    app.get("/api/protected") { req ->
        val authenticated = req.context.get<Boolean>("authenticated") ?: false
        val requestId = req.context.get<String>("requestId")

        if (!authenticated) {
            return@get Response.unauthorized("Authentication required. Please provide a Bearer token.")
        }

        val userId = req.context.get<String>("userId")
        Response.json(
            ApiResponse(
                message = "Welcome authenticated user: $userId",
                requestId = requestId,
            ),
        )
    }

    // Endpoint that demonstrates error handling
    app.get("/api/error") { req ->
        val type = req.queryParams.string("type") ?: "none"
        when (type) {
            "validation" -> throw IllegalArgumentException("Invalid parameter provided")
            "server" -> throw RuntimeException("Simulated server error")
            else -> Response.json(
                ApiResponse(
                    message = "No error. Try ?type=validation or ?type=server",
                    requestId = req.context.get<String>("requestId"),
                ),
            )
        }
    }

    // POST endpoint demonstrating request body handling
    app.post("/api/users") { req ->
        val body = req.text()
        val requestId = req.context.get<String>("requestId")

        logger.debug { "Creating user with data: $body" }

        Response.json(
            ApiResponse(
                message = "User created successfully: $body",
                requestId = requestId,
            ),
        )
    }

    // Endpoint showing conditional middleware (admin-only)
    app.get("/api/admin/settings") { req ->
        val authenticated = req.context.get<Boolean>("authenticated") ?: false
        val requestId = req.context.get<String>("requestId")

        if (!authenticated) {
            return@get Response.unauthorized("Authentication required")
        }

        // In a real app, check if user is admin
        Response.json(
            ApiResponse(
                message = "Admin settings accessed",
                requestId = requestId,
            ),
        )
    }

    logger.info { "Starting Kamellia server with middleware on port $DEFAULT_PORT..." }
    logger.info { "Middleware stack: Error Handling → Request ID → Logging → CORS → Timing → Auth → Custom Headers" }
    logger.info { "Try these endpoints:" }
    logger.info { "  GET  http://localhost:$DEFAULT_PORT/" }
    logger.info { "  GET  http://localhost:$DEFAULT_PORT/api/public" }
    logger.info { "  GET  http://localhost:$DEFAULT_PORT/api/protected (requires Authorization header)" }
    logger.info { "  GET  http://localhost:$DEFAULT_PORT/api/error?type=validation" }
    logger.info { "  POST http://localhost:$DEFAULT_PORT/api/users" }

    app.start(DEFAULT_PORT)
}
