package io.github.kamellia

import io.github.kamellia.core.Body
import io.github.kamellia.core.ErrorHandler
import io.github.kamellia.core.HttpStatus
import io.github.kamellia.core.Response
import kotlin.test.Test
import kotlin.test.assertEquals

class KamelliaStartErrorHandlerTest {
    @Test
    fun `should accept a custom ErrorHandler that maps a domain exception to a Response`() {
        val custom: ErrorHandler = { error, _ ->
            Response(
                status = HttpStatus.UNAUTHORIZED,
                body = Body.Strict("auth failed: ${error.message}".toByteArray()),
            )
        }

        val response = custom(IllegalStateException("token expired"), null)

        assertEquals(HttpStatus.UNAUTHORIZED, response.status)
        assertEquals("auth failed: token expired", (response.body as Body.Strict).bytes.decodeToString())
    }
}
