package io.github.kamellia.core

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertSame

class ResponseTest {
    @Test
    fun `should default body to Body Empty when not provided`() {
        val response = Response(status = HttpStatus.OK)

        assertSame(Body.Empty, response.body)
        assertEquals(HttpStatus.OK, response.status)
        assertEquals(emptyMap(), response.headers)
    }

    @Test
    fun `should retain Body Strict via copy`() {
        val original = Response(status = HttpStatus.OK)
        val bytes = "hi".toByteArray()

        val copied = original.copy(body = Body.Strict(bytes))

        val held = (copied.body as Body.Strict).bytes
        assertContentEquals(bytes, held)
    }
}
