package io.github.kamellia.result

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.fold
import io.github.kamellia.core.IntoResponse
import io.github.kamellia.core.Response

class ResultIntoResponse<V : IntoResponse, E : IntoResponse>(val r: Result<V, E>) : IntoResponse {
    override fun intoResponse(): Response = r.fold(
        success = { it.intoResponse() },
        failure = { it.intoResponse() },
    )
}
