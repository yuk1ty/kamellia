package io.github.kamellia.middleware

import io.github.kamellia.core.Response
import kotlin.system.measureTimeMillis

/**
 * Configuration for logging middleware
 *
 * @property logRequestBody Whether to log request body content
 * @property logResponseBody Whether to log response body content
 */
data class LoggingConfig(
    val logRequestBody: Boolean = false,
    val logResponseBody: Boolean = false,
)

/**
 * Creates a logging middleware that logs request and response information
 *
 * Logs:
 * - Request method and path
 * - Response status and duration
 * - Optionally: request and response bodies
 *
 * Example:
 * ```kotlin
 * app.use(loggingMiddleware())
 * app.use(loggingMiddleware(LoggingConfig(logRequestBody = true)))
 * ```
 *
 * @param config Logging configuration
 * @return A middleware that logs requests and responses
 */
fun loggingMiddleware(config: LoggingConfig = LoggingConfig()): Middleware = { next ->
    { req ->
        var response: Response? = null
        val duration =
            measureTimeMillis {
                println("[${req.method}] ${req.path}")
                if (config.logRequestBody) {
                    println("Request Body: ${req.body}")
                }
                response = next(req)
            }

        println("Response: ${response!!.status.code} ${response!!.status} (${duration}ms)")
        if (config.logResponseBody) {
            println("Response Body: ${response!!.body}")
        }

        response!!
    }
}
