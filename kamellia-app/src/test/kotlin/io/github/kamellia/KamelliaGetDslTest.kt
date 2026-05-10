package io.github.kamellia

import io.github.kamellia.core.Body
import io.github.kamellia.core.Context
import io.github.kamellia.core.HttpMethod
import io.github.kamellia.core.HttpStatus
import io.github.kamellia.core.PathParams
import io.github.kamellia.core.QueryParams
import io.github.kamellia.core.Request
import io.github.kamellia.core.Response
import io.github.kamellia.core.Text
import io.github.kamellia.dsl.get
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class KamelliaGetDslTest {
    private fun req(method: HttpMethod, path: String): Request = Request(
        method = method,
        path = path,
        headers = emptyMap(),
        queryParams = QueryParams.empty(),
        pathParams = PathParams.empty(),
        body = Body.Empty,
        context = Context(),
    )

    @Test
    fun `should register GET route that calls intoResponse on the returned value`() = runTest {
        val app = Kamellia()
        app.get("/hello") { _ -> Text("world") }

        val match = app.router().match(req(HttpMethod.GET, "/hello"))
        assertNotNull(match)
        val res = match.handler(req(HttpMethod.GET, "/hello"))

        assertEquals(HttpStatus.OK, res.status)
        val body = res.body
        check(body is Body.Strict)
        assertEquals("world", body.bytes.decodeToString())
    }

    @Test
    fun `should accept Response itself as a handler return value`() = runTest {
        val app = Kamellia()
        app.get("/raw") { _ -> Response(HttpStatus.NO_CONTENT) }

        val match = app.router().match(req(HttpMethod.GET, "/raw"))
        assertNotNull(match)
        val res = match.handler(req(HttpMethod.GET, "/raw"))

        assertEquals(HttpStatus.NO_CONTENT, res.status)
    }

    @Test
    fun `should accept Response with custom status when handler returns a Response directly`() = runTest {
        val app = Kamellia()
        app.get("/created") { _ ->
            Response(HttpStatus.CREATED, body = Body.Strict("done".toByteArray()))
        }

        val match = app.router().match(req(HttpMethod.GET, "/created"))
        assertNotNull(match)
        val res = match.handler(req(HttpMethod.GET, "/created"))

        assertEquals(HttpStatus.CREATED, res.status)
        val body = res.body
        check(body is Body.Strict)
        assertEquals("done", body.bytes.decodeToString())
    }
}
