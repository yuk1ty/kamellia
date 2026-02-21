package io.github.kamellia.middleware

import io.github.kamellia.core.HttpMethod
import io.github.kamellia.core.Request
import io.github.kamellia.core.Response

/**
 * Configuration for CORS middleware
 *
 * @property allowedOrigins List of allowed origins (default: ["*"])
 * @property allowedMethods List of allowed HTTP methods
 * @property allowedHeaders List of allowed request headers
 * @property exposedHeaders List of headers exposed to the client
 * @property allowCredentials Whether to allow credentials (cookies, auth headers)
 * @property maxAge Maximum age (in seconds) for preflight cache
 */
data class CorsConfig(
    val allowedOrigins: List<String> = listOf("*"),
    val allowedMethods: List<String> = listOf("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"),
    val allowedHeaders: List<String> = listOf("Content-Type", "Authorization"),
    val exposedHeaders: List<String> = emptyList(),
    val allowCredentials: Boolean = false,
    val maxAge: Int = 3600,
)

/**
 * Creates a CORS middleware that handles Cross-Origin Resource Sharing
 *
 * Handles:
 * - Preflight OPTIONS requests
 * - CORS headers on all responses
 * - Configurable origins, methods, and headers
 *
 * Example:
 * ```kotlin
 * app.use(corsMiddleware())
 * app.use(corsMiddleware(CorsConfig(
 *     allowedOrigins = listOf("https://example.com"),
 *     allowCredentials = true
 * )))
 * ```
 *
 * @param config CORS configuration
 * @return A middleware that handles CORS
 */
fun corsMiddleware(config: CorsConfig = CorsConfig()): Middleware = { next ->
    { req ->
        // Handle preflight OPTIONS requests
        if (req.method == HttpMethod.OPTIONS) {
            Response.ok().copy(
                headers = buildCorsHeaders(config, req),
            )
        } else {
            // Add CORS headers to regular responses
            val response = next(req)
            response.copy(
                headers = response.headers + buildCorsHeaders(config, req),
            )
        }
    }
}

/**
 * Builds CORS headers based on configuration
 *
 * For Access-Control-Allow-Origin:
 * - If allowedOrigins contains "*", returns "*"
 * - Otherwise, checks the request's Origin header against allowedOrigins
 * - Returns the matching origin if found, otherwise returns the first allowed origin
 */
private fun buildCorsHeaders(config: CorsConfig, req: Request): Map<String, String> {
    return buildMap {
        // Determine the origin to return
        val allowedOrigin = when {
            // If wildcard is in the list, use it
            config.allowedOrigins.contains("*") -> "*"
            // Otherwise, check if the request's origin is in the allowed list
            else -> {
                val requestOrigin = req.headers["origin"]
                if (requestOrigin != null && config.allowedOrigins.contains(requestOrigin)) {
                    requestOrigin
                } else {
                    // Fallback to first allowed origin or "*"
                    config.allowedOrigins.firstOrNull() ?: "*"
                }
            }
        }

        put("Access-Control-Allow-Origin", allowedOrigin)
        put("Access-Control-Allow-Methods", config.allowedMethods.joinToString(","))
        put("Access-Control-Allow-Headers", config.allowedHeaders.joinToString(","))

        if (config.exposedHeaders.isNotEmpty()) {
            put("Access-Control-Expose-Headers", config.exposedHeaders.joinToString(","))
        }

        if (config.allowCredentials) {
            put("Access-Control-Allow-Credentials", "true")
        }

        put("Access-Control-Max-Age", config.maxAge.toString())
    }
}
