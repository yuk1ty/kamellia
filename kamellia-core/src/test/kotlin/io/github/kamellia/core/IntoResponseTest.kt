package io.github.kamellia.core

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class IntoResponseTest {
    @Test
    fun `should convert a value into a Response when intoResponse is invoked`() {
        val encoder = IntoResponse<String> { value ->
            Response(
                status = HttpStatus.OK,
                body = Body.Strict(value.toByteArray()),
            )
        }

        val res = encoder.intoResponse("hi")

        assertEquals(HttpStatus.OK, res.status)
        val held = (res.body as Body.Strict).bytes
        assertContentEquals("hi".toByteArray(), held)
    }
}
