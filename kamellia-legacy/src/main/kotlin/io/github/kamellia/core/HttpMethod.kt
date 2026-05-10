package io.github.kamellia.core

enum class HttpMethod {
    GET,
    POST,
    PUT,
    DELETE,
    PATCH,
    HEAD,
    OPTIONS,
    ;

    companion object {
        fun fromString(method: String): HttpMethod {
            return valueOf(method.uppercase())
        }
    }
}
