package io.github.kamellia.result

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import io.github.kamellia.core.Body
import io.github.kamellia.core.HttpStatus
import io.github.kamellia.core.IntoResponse
import io.github.kamellia.core.Response
import kotlin.test.Test
import kotlin.test.assertEquals

class ResultIntoResponseTest {
    private val okEncoder: IntoResponse<String> = IntoResponse { value ->
        Response(HttpStatus.OK, body = Body.Strict("ok:$value".toByteArray()))
    }
    private val errEncoder: IntoResponse<String> = IntoResponse { value ->
        Response(HttpStatus.BAD_REQUEST, body = Body.Strict("err:$value".toByteArray()))
    }

    @Test
    fun `should produce ok-Response when Result is Ok`() {
        val responder = resultIntoResponse(okEncoder, errEncoder)
        val r: Result<String, String> = Ok("Alice")

        val res = responder.intoResponse(r)

        assertEquals(HttpStatus.OK, res.status)
        assertEquals("ok:Alice", String((res.body as Body.Strict).bytes))
    }

    @Test
    fun `should produce err-Response when Result is Err`() {
        val responder = resultIntoResponse(okEncoder, errEncoder)
        val r: Result<String, String> = Err("not_found")

        val res = responder.intoResponse(r)

        assertEquals(HttpStatus.BAD_REQUEST, res.status)
        assertEquals("err:not_found", String((res.body as Body.Strict).bytes))
    }
}
