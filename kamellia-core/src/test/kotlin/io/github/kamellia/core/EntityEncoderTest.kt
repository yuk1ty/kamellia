package io.github.kamellia.core

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class EntityEncoderTest {
    @Test
    fun `should encode a value into a Body and expose its content type`() {
        val encoder = EntityEncoder<String>(ContentType.TEXT_PLAIN_UTF8) { value ->
            Body.Strict(value.toByteArray())
        }

        val body = encoder.encode("x")

        assertEquals(ContentType.TEXT_PLAIN_UTF8, encoder.contentType)
        assertContentEquals("x".toByteArray(), (body as Body.Strict).bytes)
    }
}
