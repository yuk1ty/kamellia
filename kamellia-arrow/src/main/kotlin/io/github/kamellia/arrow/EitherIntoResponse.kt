package io.github.kamellia.arrow

import arrow.core.Either
import io.github.kamellia.core.IntoResponse

fun <E, V> eitherIntoResponse(ok: IntoResponse<V>, err: IntoResponse<E>): IntoResponse<Either<E, V>> =
    IntoResponse { either ->
        either.fold(
            ifLeft = { err.intoResponse(it) },
            ifRight = { ok.intoResponse(it) },
        )
    }
