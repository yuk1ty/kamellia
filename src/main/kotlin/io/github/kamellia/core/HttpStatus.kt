package io.github.kamellia.core

enum class HttpStatus(val code: Int, val reasonPhrase: String) {
    OK(200, "OK"),
    CREATED(201, "Created"),
    NO_CONTENT(204, "No Content"),
    BAD_REQUEST(400, "Bad Request"),
    UNAUTHORIZED(401, "Unauthorized"),
    FORBIDDEN(403, "Forbidden"),
    NOT_FOUND(404, "Not Found"),
    INTERNAL_SERVER_ERROR(500, "Internal Server Error"),
    ;

    companion object {
        fun fromCode(code: Int): HttpStatus {
            return entries.find { it.code == code }
                ?: INTERNAL_SERVER_ERROR
        }
    }
}
