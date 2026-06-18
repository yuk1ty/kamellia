package io.github.kamellia.core

/**
 * An HTTP response.
 *
 * This is the concrete representation of a response that is sent back to the client.
 * It can be created directly or produced by calling [IntoResponse.intoResponse].
 *
 * Example:
 * ```kotlin
 * val response = Response(
 *     status = HttpStatus.OK,
 *     headers = mapOf("Content-Type" to "text/plain"),
 *     body = Body.Strict("Hello, World!".toByteArray())
 * )
 * ```
 *
 * @property status The HTTP status code of the response.
 * @property headers The HTTP headers to include in the response.
 * @property body The body of the response.
 */
data class Response(val status: HttpStatus, val headers: Headers = emptyMap(), val body: Body = Body.Empty) :
    IntoResponse {
    override fun intoResponse(): Response = this
}
