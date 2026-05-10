package io.github.kamellia.middleware

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
