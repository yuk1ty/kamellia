package io.github.kamellia.core

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class TextTest {
    @Test
    fun `should wrap a String into a 200 plain text Response when Text is invoked`() {
        val r = Text("hello")

        val res = r.intoResponse()

        assertEquals(HttpStatus.OK, res.status)
        assertEquals("text/plain; charset=utf-8", res.headers["Content-Type"])
        val body = res.body
        check(body is Body.Strict)
        assertContentEquals("hello".toByteArray(), body.bytes)
    }
}
