package io.github.kamellia.json

import io.github.kamellia.core.Body
import io.github.kamellia.core.ContentType
import io.github.kamellia.core.HttpStatus
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals

class JsonIntoResponseTest {
    @Serializable
    data class User(val id: String, val name: String)

    @Test
    fun `should produce IntoResponse that wraps Serializable value as JSON 200 by default`() {
        val responder = jsonIntoResponse<User>()

        val res = responder.intoResponse(User("1", "Alice"))

        assertEquals(HttpStatus.OK, res.status)
        assertEquals(ContentType.APPLICATION_JSON.value, res.headers["Content-Type"])
        assertEquals("""{"id":"1","name":"Alice"}""", String((res.body as Body.Strict).bytes))
    }

    @Test
    fun `should honor custom status when given to jsonIntoResponse`() {
        val responder = jsonIntoResponse<User>(status = HttpStatus.CREATED)

        val res = responder.intoResponse(User("2", "Bob"))

        assertEquals(HttpStatus.CREATED, res.status)
    }

    @Test
    fun `should support nested generic types via typeOf`() {
        val responder = jsonIntoResponse<List<User>>()

        val res = responder.intoResponse(listOf(User("1", "A"), User("2", "B")))

        assertEquals(HttpStatus.OK, res.status)
        val content = String((res.body as Body.Strict).bytes)
        assertEquals("""[{"id":"1","name":"A"},{"id":"2","name":"B"}]""", content)
    }
}
