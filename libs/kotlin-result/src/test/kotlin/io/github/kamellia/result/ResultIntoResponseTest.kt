package io.github.kamellia.result

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import io.github.kamellia.core.Body
import io.github.kamellia.core.HttpStatus
import io.github.kamellia.core.Response
import io.github.kamellia.serialization.json
import io.github.kamellia.serialization.jsonBytes
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals

class ResultIntoResponseTest {
    @Serializable
    data class User(val id: String, val name: String)

    @Serializable
    data class AppError(val code: String, val message: String)

    @Test
    fun `should produce ok-Response when Result is Ok`() {
        val r: Result<Response, Response> = Ok(json(User("1", "Alice")).intoResponse())
        val wrapped = ResultIntoResponse(r)

        val res = wrapped.intoResponse()

        assertEquals(HttpStatus.OK, res.status)
        val body = res.body
        check(body is Body.Strict)
        assertEquals("""{"id":"1","name":"Alice"}""", body.bytes.decodeToString())
    }

    @Test
    fun `should produce err-Response when Result is Err`() {
        val errResponse = Response(
            status = HttpStatus.BAD_REQUEST,
            headers = mapOf("Content-Type" to "application/json; charset=utf-8"),
            body = Body.Strict(jsonBytes(AppError("E1", "boom"))),
        )
        val r: Result<Response, Response> = Err(errResponse)
        val wrapped = ResultIntoResponse(r)

        val res = wrapped.intoResponse()

        assertEquals(HttpStatus.BAD_REQUEST, res.status)
        val body = res.body
        check(body is Body.Strict)
        assertEquals("""{"code":"E1","message":"boom"}""", body.bytes.decodeToString())
    }
}
