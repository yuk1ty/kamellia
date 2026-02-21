package io.github.kamellia.middleware

import io.github.kamellia.core.Handler

/**
 * A middleware is a function that takes a [Handler] and returns a new [Handler].
 *
 * Middlewares enable request/response processing chains for cross-cutting concerns
 * such as logging, CORS, authentication, error handling, etc.
 *
 * Example:
 * ```kotlin
 * val loggingMiddleware: Middleware = { next ->
 *     { request ->
 *         println("Processing: ${request.method} ${request.path}")
 *         val response = next(request)
 *         println("Response: ${response.status}")
 *         response
 *     }
 * }
 * ```
 *
 * Middlewares can be composed together and will execute in registration order:
 * ```kotlin
 * app.use(loggingMiddleware)
 * app.use(corsMiddleware)
 * app.get("/") { Response.ok("Hello") }
 * ```
 *
 * Execution flow: Request → Logging → CORS → Handler → CORS → Logging → Response
 */
typealias Middleware = (Handler) -> Handler
