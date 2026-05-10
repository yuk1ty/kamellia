package io.github.kamellia.netty

import io.github.kamellia.core.Body
import io.github.kamellia.core.HttpStatus
import io.github.kamellia.error.HttpException
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultErrorHandlerTest {
    @Test
    fun `should map HttpException to its declared status with the message in the body`() {
        val response = KamelliaHandler.defaultErrorHandler(
            HttpException(HttpStatus.NOT_FOUND, "user missing"),
            null,
        )

        assertEquals(HttpStatus.NOT_FOUND, response.status)
        assertEquals("text/plain; charset=utf-8", response.headers["Content-Type"])
        val body = response.body
        check(body is Body.Strict)
        assertEquals("user missing", body.bytes.decodeToString())
    }

    @Test
    fun `should map a generic Exception to 500 with the message in the body`() {
        val response = KamelliaHandler.defaultErrorHandler(
            IllegalStateException("kaboom"),
            null,
        )

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.status)
        val body = response.body
        check(body is Body.Strict)
        assertEquals("kaboom", body.bytes.decodeToString())
    }

    @Test
    fun `should fall back to empty body when the exception has no message`() {
        val response = KamelliaHandler.defaultErrorHandler(IllegalStateException(), null)

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.status)
        assertEquals(Body.Empty, response.body)
    }
}
