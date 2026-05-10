package io.github.kamellia.middleware

import io.github.kamellia.core.Handler

/**
 * Composes multiple middlewares into a single handler.
 *
 * Middlewares are applied in the order they appear in the list (left to right),
 * meaning the first middleware in the list will be the outermost layer of the chain.
 *
 * Execution order example:
 * ```
 * If middlewares = [M1, M2, M3]:
 * Request → M1 → M2 → M3 → Handler → M3 → M2 → M1 → Response
 * ```
 *
 * @param middlewares The list of middlewares to compose
 * @param handler The final handler to execute after all middlewares
 * @return A composed handler that applies all middlewares in sequence
 */
fun composeMiddlewares(middlewares: List<Middleware>, handler: Handler): Handler {
    return middlewares.foldRight(handler) { middleware, next ->
        middleware(next)
    }
}
