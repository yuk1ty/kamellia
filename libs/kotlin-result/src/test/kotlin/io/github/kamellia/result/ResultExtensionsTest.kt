package io.github.kamellia.result

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import io.github.kamellia.core.Body
import io.github.kamellia.core.HttpStatus
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals

class ResultExtensionsTest {
    @Serializable
    data class User(val id: String, val name: String)

    @Serializable
    data class AppError(val code: String, val message: String)

    @Test
    fun `should map Result to JSON ok and JSON err with default 400`() {
        val ok: Result<User, AppError> = Ok(User("1", "Alice"))
        val err: Result<User, AppError> = Err(AppError("E1", "boom"))

        val okRes = ok.asJsonIntoResponse().intoResponse()
        val errRes = err.asJsonIntoResponse().intoResponse()

        assertEquals(HttpStatus.OK, okRes.status)
        assertEquals(
            """{"id":"1","name":"Alice"}""",
            (okRes.body as Body.Strict).bytes.decodeToString(),
        )

        assertEquals(HttpStatus.BAD_REQUEST, errRes.status)
        assertEquals(
            """{"code":"E1","message":"boom"}""",
            (errRes.body as Body.Strict).bytes.decodeToString(),
        )
    }
}
