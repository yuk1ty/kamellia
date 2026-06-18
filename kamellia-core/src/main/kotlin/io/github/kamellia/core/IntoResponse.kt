package io.github.kamellia.core

/**
 * A type that can be converted into a [Response].
 *
 * Implementing this interface allows an object to define its own conversion
 * logic to a [Response], which can then be returned from handlers.
 */
interface IntoResponse {

    /**
     * Converts this object into a [Response].
     *
     * Example:
     * ```kotlin
     * data class TextResponse(val text: String) : IntoResponse {
     *     override fun intoResponse(): Response =
     *         Response(status = HttpStatus.OK, body = Body.Strict(text.toByteArray()))
     * }
     *
     * val response: Response = TextResponse("Hello, World!").intoResponse()
     * ```
     *
     * Frameworks and handlers can return any [IntoResponse] implementation,
     * leaving the conversion to a [Response] to the framework via this method.
     *
     * @return The [Response] representation of this object.
     */
    fun intoResponse(): Response
}
