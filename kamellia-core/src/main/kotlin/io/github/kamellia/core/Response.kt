package io.github.kamellia.core

data class Response(val status: HttpStatus, val headers: Headers = emptyMap(), val body: Body = Body.Empty) :
    IntoResponse {
    override fun intoResponse(): Response = this
}
