package io.github.kamellia.serialization

import io.github.kamellia.core.Body
import io.github.kamellia.core.HttpStatus
import io.github.kamellia.core.IntoResponse
import io.github.kamellia.core.Response
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.reflect.typeOf

@PublishedApi
internal val jsonInstance: Json = Json {
    prettyPrint = false
    ignoreUnknownKeys = true
}

inline fun <reified T> serializerOf(): KSerializer<T> {
    @Suppress("UNCHECKED_CAST")
    return serializer(typeOf<T>()) as KSerializer<T>
}

inline fun <reified T> jsonBytes(value: T): ByteArray {
    val ser = serializerOf<T>()
    return jsonInstance.encodeToString(ser, value).toByteArray()
}

inline fun <reified T> json(value: T): IntoResponse {
    val bytes = jsonBytes(value)
    return JsonResponse(bytes)
}

@JvmInline
value class JsonResponse(@PublishedApi internal val bytes: ByteArray) : IntoResponse {
    override fun intoResponse(): Response = Response(
        status = HttpStatus.OK,
        headers = mapOf("Content-Type" to "application/json; charset=utf-8"),
        body = Body.Strict(bytes),
    )
}
