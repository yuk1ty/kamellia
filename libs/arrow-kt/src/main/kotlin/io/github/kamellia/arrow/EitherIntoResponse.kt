package io.github.kamellia.arrow

import arrow.core.Either
import io.github.kamellia.core.IntoResponse
import io.github.kamellia.core.Response

class EitherIntoResponse<L : IntoResponse, R : IntoResponse>(val e: Either<L, R>) : IntoResponse {
    override fun intoResponse(): Response = e.fold(
        ifLeft = { it.intoResponse() },
        ifRight = { it.intoResponse() },
    )
}
