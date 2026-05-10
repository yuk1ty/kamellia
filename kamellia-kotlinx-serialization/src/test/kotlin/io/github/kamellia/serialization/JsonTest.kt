package io.github.kamellia.serialization

import io.github.kamellia.core.Body
import io.github.kamellia.core.HttpStatus
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals

class JsonTest {
    @Serializable
    data class User(val id: String, val name: String)

    @Test
    fun `should wrap a Serializable value as JSON 200 IntoResponse by default`() {
        val r = json(User("1", "Alice"))

        val res = r.intoResponse()

        assertEquals(HttpStatus.OK, res.status)
        assertEquals("application/json; charset=utf-8", res.headers["Content-Type"])
        val body = res.body
        check(body is Body.Strict)
        assertEquals("""{"id":"1","name":"Alice"}""", body.bytes.decodeToString())
    }

    @Test
    fun `should support nested generic types via typeOf when json is invoked`() {
        val r = json(listOf(User("1", "A"), User("2", "B")))

        val res = r.intoResponse()

        val body = res.body
        check(body is Body.Strict)
        assertEquals("""[{"id":"1","name":"A"},{"id":"2","name":"B"}]""", body.bytes.decodeToString())
    }

    @Test
    fun `should expose jsonBytes helper for direct Response building`() {
        val bytes = jsonBytes(User("1", "Alice"))

        assertEquals("""{"id":"1","name":"Alice"}""", bytes.decodeToString())
    }
}
