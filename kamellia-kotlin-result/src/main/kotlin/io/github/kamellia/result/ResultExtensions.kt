package io.github.kamellia.result

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapError
import io.github.kamellia.core.Body
import io.github.kamellia.core.HttpStatus
import io.github.kamellia.core.IntoResponse
import io.github.kamellia.core.Response
import io.github.kamellia.serialization.json
import io.github.kamellia.serialization.jsonBytes

inline fun <reified V, reified E> Result<V, E>.asJsonIntoResponse(
    errStatus: HttpStatus = HttpStatus.BAD_REQUEST,
): IntoResponse {
    val mapped: Result<IntoResponse, IntoResponse> = this
        .map { json(it) }
        .mapError { err ->
            Response(
                status = errStatus,
                headers = mapOf("Content-Type" to "application/json; charset=utf-8"),
                body = Body.Strict(jsonBytes(err)),
            )
        }
    return ResultIntoResponse(mapped)
}

inline fun <reified V : IntoResponse, reified E : IntoResponse> Result<V, E>.asIntoResponse(): IntoResponse =
    ResultIntoResponse(this)

data object Unit : IntoResponse {
    override fun intoResponse(): Response = Response(status = HttpStatus.NO_CONTENT)
}
