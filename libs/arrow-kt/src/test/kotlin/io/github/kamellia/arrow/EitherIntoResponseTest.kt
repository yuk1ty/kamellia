package io.github.kamellia.arrow

import arrow.core.Either
import io.github.kamellia.core.Body
import io.github.kamellia.core.HttpStatus
import io.github.kamellia.core.Response
import io.github.kamellia.serialization.json
import io.github.kamellia.serialization.jsonBytes
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals

class EitherIntoResponseTest {
    @Serializable
    data class User(val id: String, val name: String)

    @Serializable
    data class AppError(val code: String, val message: String)

    @Test
    fun `should produce right-Response when Either is Right`() {
        val e: Either<Response, Response> = Either.Right(json(User("1", "Alice")).intoResponse())

        val res = EitherIntoResponse(e).intoResponse()

        assertEquals(HttpStatus.OK, res.status)
        assertEquals(
            """{"id":"1","name":"Alice"}""",
            (res.body as Body.Strict).bytes.decodeToString(),
        )
    }

    @Test
    fun `should produce left-Response when Either is Left`() {
        val errResponse = Response(
            status = HttpStatus.BAD_REQUEST,
            headers = mapOf("Content-Type" to "application/json; charset=utf-8"),
            body = Body.Strict(jsonBytes(AppError("E1", "boom"))),
        )
        val e: Either<Response, Response> = Either.Left(errResponse)

        val res = EitherIntoResponse(e).intoResponse()

        assertEquals(HttpStatus.BAD_REQUEST, res.status)
        assertEquals(
            """{"code":"E1","message":"boom"}""",
            (res.body as Body.Strict).bytes.decodeToString(),
        )
    }
}
