package examples.middleware

import io.github.kamellia.Kamellia
import io.github.kamellia.dsl.get
import io.github.kamellia.middleware.CorsConfig
import io.github.kamellia.middleware.Middleware
import io.github.kamellia.middleware.corsMiddleware
import io.github.kamellia.middleware.loggingMiddleware
import io.github.kamellia.serialization.json
import java.util.UUID

private const val PORT = 3002

private const val REQUEST_ID_CONTEXT_KEY = "requestId"

private const val REQUEST_ID_LENGTH = 8

private val requestIdMiddleware: Middleware = { next ->
    { req ->
        val requestId = UUID.randomUUID().toString().take(REQUEST_ID_LENGTH)
        req.context.set(REQUEST_ID_CONTEXT_KEY, requestId)
        next(req)
    }
}

fun main() {
    val app = Kamellia()

    app.use(loggingMiddleware())
    app.use(
        corsMiddleware(
            CorsConfig(
                allowedOrigins = listOf("http://localhost:3000"),
                allowCredentials = true,
            ),
        ),
    )
    app.use(requestIdMiddleware)

    app.get("/") { req ->
        val requestId = req.context.get<String>(REQUEST_ID_CONTEXT_KEY)
        json(
            mapOf(
                "message" to "Hello with middleware",
                "requestId" to requestId,
            ),
        )
    }

    app.start(PORT)
}
