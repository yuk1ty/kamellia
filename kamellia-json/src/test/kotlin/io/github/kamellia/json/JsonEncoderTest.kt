package io.github.kamellia.json

import io.github.kamellia.core.Body
import io.github.kamellia.core.ContentType
import io.github.kamellia.core.EntityEncoder
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals

class JsonEncoderTest {
    @Serializable
    data class User(val id: String, val name: String)

    @Test
    fun `should serialize a Serializable value into a JSON Body Strict`() {
        val encoder = EntityEncoder.json(User.serializer())

        val body = encoder.encode(User("1", "Alice"))

        assertEquals(ContentType.APPLICATION_JSON, encoder.contentType)
        assertEquals("""{"id":"1","name":"Alice"}""", String((body as Body.Strict).bytes))
    }
}
