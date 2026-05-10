package io.github.kamellia.middleware

data class CorsConfig(
    val allowedOrigins: List<String> = listOf("*"),
    val allowedMethods: List<String> = listOf("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"),
    val allowedHeaders: List<String> = listOf("Content-Type", "Authorization"),
    val exposedHeaders: List<String> = emptyList(),
    val allowCredentials: Boolean = false,
    val maxAge: Int = 3600,
)
