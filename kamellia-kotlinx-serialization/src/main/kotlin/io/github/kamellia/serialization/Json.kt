package io.github.kamellia.serialization

import io.github.kamellia.core.Body
import io.github.kamellia.core.Headers
import io.github.kamellia.core.HttpStatus
import io.github.kamellia.core.IntoResponse
import io.github.kamellia.core.Request
import io.github.kamellia.core.Response
import kotlinx.coroutines.flow.collect
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import java.io.ByteArrayOutputStream
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

@PublishedApi
internal suspend fun readJsonBytes(body: Body): ByteArray = when (body) {
    is Body.Strict -> body.bytes

    Body.Empty -> throw SerializationException("Cannot decode JSON from an empty request body")

    is Body.Streamed -> {
        val sink = ByteArrayOutputStream()
        body.source.collect { chunk -> sink.write(chunk) }
        if (sink.size() == 0) {
            throw SerializationException("Cannot decode JSON from an empty streamed body")
        }
        sink.toByteArray()
    }
}

suspend inline fun <reified T> Request.json(strict: Boolean = true): T {
    val contentType = headers.findIgnoreCase("Content-Type")
    if (strict && !isJsonContentType(contentType)) {
        throw SerializationException(
            "Content-Type must be a JSON media type, but got: $contentType",
        )
    }
    val bytes = readJsonBytes(this.body)
    val ser = serializerOf<T>()
    return jsonInstance.decodeFromString(ser, bytes.decodeToString())
}

@PublishedApi
internal fun Headers.findIgnoreCase(name: String): String? {
    val lower = name.lowercase()
    for ((key, value) in this) {
        if (key.lowercase() == lower) return value
    }
    return null
}

@PublishedApi
internal fun isJsonContentType(contentType: String?): Boolean {
    if (contentType == null) return false
    val mediaType = contentType.substringBefore(';').trim().lowercase()
    return mediaType == "application/json" ||
        mediaType == "text/json" ||
        (mediaType.startsWith("application/") && mediaType.endsWith("+json"))
}
