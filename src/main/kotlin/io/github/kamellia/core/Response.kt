package io.github.kamellia.core

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

val jsonInstance =
    Json {
        prettyPrint = false
        ignoreUnknownKeys = true
    }

data class Response(
    val status: HttpStatus,
    val headers: Headers = emptyMap(),
    val body: Body = Body.Empty,
) {
    companion object {
        fun ok(body: String = ""): Response {
            return Response(
                status = HttpStatus.OK,
                headers = mapOf("Content-Type" to "text/plain; charset=utf-8"),
                body = if (body.isEmpty()) Body.Empty else Body.Text(body),
            )
        }

        inline fun <reified T> json(data: T): Response {
            val jsonString = jsonInstance.encodeToString(serializer(), data)
            return Response(
                status = HttpStatus.OK,
                headers = mapOf("Content-Type" to "application/json; charset=utf-8"),
                body = Body.Text(jsonString),
            )
        }

        fun notFound(message: String = "Not Found"): Response {
            return Response(
                status = HttpStatus.NOT_FOUND,
                headers = mapOf("Content-Type" to "text/plain; charset=utf-8"),
                body = Body.Text(message),
            )
        }

        fun internalServerError(message: String = "Internal Server Error"): Response {
            return Response(
                status = HttpStatus.INTERNAL_SERVER_ERROR,
                headers = mapOf("Content-Type" to "text/plain; charset=utf-8"),
                body = Body.Text(message),
            )
        }

        fun badRequest(message: String = "Bad Request"): Response {
            return Response(
                status = HttpStatus.BAD_REQUEST,
                headers = mapOf("Content-Type" to "text/plain; charset=utf-8"),
                body = Body.Text(message),
            )
        }
    }
}
