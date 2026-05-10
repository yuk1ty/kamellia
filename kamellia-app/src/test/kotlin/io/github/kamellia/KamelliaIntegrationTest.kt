package io.github.kamellia

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import io.github.kamellia.core.Body
import io.github.kamellia.core.Context
import io.github.kamellia.core.HttpMethod
import io.github.kamellia.core.HttpStatus
import io.github.kamellia.core.PathParams
import io.github.kamellia.core.QueryParams
import io.github.kamellia.core.Request
import io.github.kamellia.core.Text
import io.github.kamellia.dsl.get
import io.github.kamellia.result.asJsonIntoResponse
import io.github.kamellia.serialization.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class KamelliaIntegrationTest {
    @Serializable
    data class User(val id: String, val name: String)

    @Serializable
    data class AppError(val code: String, val message: String)

    private fun req(path: String): Request = Request(
        method = HttpMethod.GET,
        path = path,
        headers = emptyMap(),
        queryParams = QueryParams.empty(),
        pathParams = PathParams.empty(),
        body = Body.Empty,
        context = Context(),
    )

    @Test
    fun `should serve plain text when handler returns Text`() = runTest {
        val app = Kamellia()
        app.get("/") { _ -> Text("hello") }

        val match = app.router().match(req("/"))
        assertNotNull(match)
        val res = match.handler(req("/"))

        assertEquals(HttpStatus.OK, res.status)
        assertEquals("hello", (res.body as Body.Strict).bytes.decodeToString())
    }

    @Test
    fun `should serve JSON when handler returns json wrapper of a Serializable value`() = runTest {
        val app = Kamellia()
        app.get("/users/:id") { _ -> json(User("1", "Alice")) }

        val match = app.router().match(req("/users/1"))
        assertNotNull(match)
        val res = match.handler(req("/users/1"))

        assertEquals(HttpStatus.OK, res.status)
        assertEquals("application/json; charset=utf-8", res.headers["Content-Type"])
        assertEquals(
            """{"id":"1","name":"Alice"}""",
            (res.body as Body.Strict).bytes.decodeToString(),
        )
    }

    @Test
    fun `should serve 400 with JSON error body when handler returns ResultIntoResponse of Err`() = runTest {
        val app = Kamellia()
        app.get("/safe") { _ ->
            val r: Result<User, AppError> = Err(AppError("E1", "boom"))
            r.asJsonIntoResponse()
        }

        val match = app.router().match(req("/safe"))
        assertNotNull(match)
        val res = match.handler(req("/safe"))

        assertEquals(HttpStatus.BAD_REQUEST, res.status)
        assertEquals(
            """{"code":"E1","message":"boom"}""",
            (res.body as Body.Strict).bytes.decodeToString(),
        )
    }

    @Test
    fun `should serve 200 with JSON body when handler returns ResultIntoResponse of Ok`() = runTest {
        val app = Kamellia()
        app.get("/ok") { _ ->
            val r: Result<User, AppError> = Ok(User("1", "Alice"))
            r.asJsonIntoResponse()
        }

        val match = app.router().match(req("/ok"))
        assertNotNull(match)
        val res = match.handler(req("/ok"))

        assertEquals(HttpStatus.OK, res.status)
        assertEquals(
            """{"id":"1","name":"Alice"}""",
            (res.body as Body.Strict).bytes.decodeToString(),
        )
    }
}
