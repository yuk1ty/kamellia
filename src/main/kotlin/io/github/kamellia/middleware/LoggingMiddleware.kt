package io.github.kamellia.middleware

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import java.util.UUID
import kotlin.time.measureTimedValue

private val logger = KotlinLogging.logger {}

private const val REQUEST_ID_LENGTH = 8

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
        val requestId = UUID.randomUUID().toString().substring(0, REQUEST_ID_LENGTH)

        val (response, duration) =
            withLoggingContext("requestId" to requestId) {
                logger.info { "[${req.method}] ${req.path}" }
                if (config.logRequestBody) {
                    logger.debug { "Request Body: ${req.body}" }
                }
                measureTimedValue { next(req) }
            }

        withLoggingContext("requestId" to requestId) {
            logger.info {
                "Response: ${response.status.code} ${response.status} (${duration.inWholeMilliseconds}ms)"
            }
            if (config.logResponseBody) {
                logger.debug { "Response Body: ${response.body}" }
            }
        }

        response
    }
}
