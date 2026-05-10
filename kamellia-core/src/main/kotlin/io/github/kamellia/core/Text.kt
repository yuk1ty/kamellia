package io.github.kamellia.core

@JvmInline
value class Text(val value: String) : IntoResponse {
    override fun intoResponse(): Response = Response(
        status = HttpStatus.OK,
        headers = mapOf("Content-Type" to "text/plain; charset=utf-8"),
        body = Body.Strict(value.toByteArray()),
    )
}
