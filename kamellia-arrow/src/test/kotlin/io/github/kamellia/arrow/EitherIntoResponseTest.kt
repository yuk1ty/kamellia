package io.github.kamellia.arrow

import arrow.core.Either
import io.github.kamellia.core.Body
import io.github.kamellia.core.HttpStatus
import io.github.kamellia.core.IntoResponse
import io.github.kamellia.core.Response
import kotlin.test.Test
import kotlin.test.assertEquals

class EitherIntoResponseTest {
    private val okEncoder: IntoResponse<String> = IntoResponse { value ->
        Response(HttpStatus.OK, body = Body.Strict("ok:$value".toByteArray()))
    }
    private val errEncoder: IntoResponse<String> = IntoResponse { value ->
        Response(HttpStatus.BAD_REQUEST, body = Body.Strict("err:$value".toByteArray()))
    }

    @Test
    fun `should produce right-Response when Either is Right`() {
        val responder = eitherIntoResponse(okEncoder, errEncoder)
        val e: Either<String, String> = Either.Right("Alice")

        val res = responder.intoResponse(e)

        assertEquals(HttpStatus.OK, res.status)
        assertEquals("ok:Alice", String((res.body as Body.Strict).bytes))
    }

    @Test
    fun `should produce left-Response when Either is Left`() {
        val responder = eitherIntoResponse(okEncoder, errEncoder)
        val e: Either<String, String> = Either.Left("invalid")

        val res = responder.intoResponse(e)

        assertEquals(HttpStatus.BAD_REQUEST, res.status)
        assertEquals("err:invalid", String((res.body as Body.Strict).bytes))
    }
}
