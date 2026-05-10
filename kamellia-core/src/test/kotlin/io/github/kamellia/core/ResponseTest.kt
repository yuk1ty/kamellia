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
        val bytes = "data".toByteArray()

        val updated = original.copy(body = Body.Strict(bytes))

        val body = updated.body
        check(body is Body.Strict)
        assertContentEquals(bytes, body.bytes)
    }
}
