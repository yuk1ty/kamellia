package io.github.kamellia.middleware

import io.github.kamellia.core.HttpMethod
import io.github.kamellia.core.HttpStatus
import io.github.kamellia.core.Request
import io.github.kamellia.core.Response

fun corsMiddleware(config: CorsConfig = CorsConfig()): Middleware = { next ->
    { req ->
        if (req.method == HttpMethod.OPTIONS) {
            Response(
                status = HttpStatus.OK,
                headers = buildCorsHeaders(config, req),
            )
        } else {
            val response = next(req)
            response.copy(
                headers = response.headers + buildCorsHeaders(config, req),
            )
        }
    }
}

private fun buildCorsHeaders(config: CorsConfig, req: Request): Map<String, String> = buildMap {
    val allowedOrigin = when {
        config.allowedOrigins.contains("*") -> "*"

        else -> {
            val requestOrigin = req.headers["origin"]
            if (requestOrigin != null && config.allowedOrigins.contains(requestOrigin)) {
                requestOrigin
            } else {
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
