package io.github.kamellia.core

object IntoResponses {
    val identity: IntoResponse<Response> = IntoResponse { it }

    val plainText: IntoResponse<String> = IntoResponse { value ->
        Response(
            status = HttpStatus.OK,
            headers = mapOf("Content-Type" to ContentType.TEXT_PLAIN_UTF8.value),
            body = Body.Strict(value.toByteArray()),
        )
    }

    val noContent: IntoResponse<Unit> = IntoResponse {
        Response(status = HttpStatus.NO_CONTENT)
    }

    fun <T> fromEncoder(encoder: EntityEncoder<T>, status: HttpStatus = HttpStatus.OK): IntoResponse<T> =
        IntoResponse { value ->
            Response(
                status = status,
                headers = mapOf("Content-Type" to encoder.contentType.value),
                body = encoder.encode(value),
            )
        }
}
