package io.github.kamellia.json

import io.github.kamellia.core.Body
import io.github.kamellia.core.ContentType
import io.github.kamellia.core.EntityEncoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

internal val jsonInstance = Json {
    prettyPrint = false
    ignoreUnknownKeys = true
}

fun <T> EntityEncoder.Companion.json(serializer: KSerializer<T>): EntityEncoder<T> =
    EntityEncoder(ContentType.APPLICATION_JSON) { value ->
        Body.Strict(jsonInstance.encodeToString(serializer, value).toByteArray())
    }
