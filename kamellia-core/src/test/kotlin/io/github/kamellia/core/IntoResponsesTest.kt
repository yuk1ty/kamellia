package io.github.kamellia.core

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertSame

class IntoResponsesTest {
    @Test
    fun `should produce a 200 plain text Response when plainText encoder is applied`() {
        val res = IntoResponses.plainText.intoResponse("hello")

        assertEquals(HttpStatus.OK, res.status)
        assertEquals(ContentType.TEXT_PLAIN_UTF8.value, res.headers["Content-Type"])
        assertContentEquals("hello".toByteArray(), (res.body as Body.Strict).bytes)
    }

    @Test
    fun `should pass-through a Response unchanged when identity encoder is applied`() {
        val original = Response(
            status = HttpStatus.CREATED,
            headers = mapOf("X-Foo" to "bar"),
            body = Body.Strict("done".toByteArray()),
        )

        val res = IntoResponses.identity.intoResponse(original)

        assertSame(original, res)
    }

    @Test
    fun `should set Content-Type from EntityEncoder when fromEncoder is applied`() {
        val encoder = EntityEncoder<String>(ContentType.APPLICATION_OCTET_STREAM) { value ->
            Body.Strict(value.toByteArray())
        }

        val res = IntoResponses.fromEncoder(encoder, status = HttpStatus.CREATED).intoResponse("x")

        assertEquals(HttpStatus.CREATED, res.status)
        assertEquals(ContentType.APPLICATION_OCTET_STREAM.value, res.headers["Content-Type"])
    }
}
