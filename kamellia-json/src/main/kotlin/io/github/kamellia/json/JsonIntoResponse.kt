package io.github.kamellia.json

import io.github.kamellia.core.EntityEncoder
import io.github.kamellia.core.HttpStatus
import io.github.kamellia.core.IntoResponse
import io.github.kamellia.core.IntoResponses
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import kotlin.reflect.typeOf

inline fun <reified T> serializerOf(): KSerializer<T> {
    @Suppress("UNCHECKED_CAST")
    return serializer(typeOf<T>()) as KSerializer<T>
}

inline fun <reified T> jsonIntoResponse(status: HttpStatus = HttpStatus.OK): IntoResponse<T> =
    IntoResponses.fromEncoder(
        encoder = EntityEncoder.json(serializerOf<T>()),
        status = status,
    )
