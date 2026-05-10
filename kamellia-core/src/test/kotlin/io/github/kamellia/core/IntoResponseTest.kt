package io.github.kamellia.core

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class IntoResponseTest {
    @Test
    fun `should expose intoResponse method on IntoResponse implementations`() {
        val custom = object : IntoResponse {
            override fun intoResponse(): Response = Response(
                status = HttpStatus.OK,
                body = Body.Strict("x".toByteArray()),
            )
        }

        val res = custom.intoResponse()

        assertEquals(HttpStatus.OK, res.status)
        val body = res.body
        check(body is Body.Strict)
        assertContentEquals("x".toByteArray(), body.bytes)
    }
}
