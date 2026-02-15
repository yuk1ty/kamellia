package io.github.kamellia.error

import io.github.kamellia.core.HttpStatus

class HttpException(
    val status: HttpStatus,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
