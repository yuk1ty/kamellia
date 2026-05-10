package io.github.kamellia.result

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.fold
import io.github.kamellia.core.IntoResponse

fun <V, E> resultIntoResponse(ok: IntoResponse<V>, err: IntoResponse<E>): IntoResponse<Result<V, E>> =
    IntoResponse { r ->
        r.fold(
            success = { ok.intoResponse(it) },
            failure = { err.intoResponse(it) },
        )
    }
