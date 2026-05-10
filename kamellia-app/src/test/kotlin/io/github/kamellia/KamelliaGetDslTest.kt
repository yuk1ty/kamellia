package io.github.kamellia

import io.github.kamellia.core.Body
import io.github.kamellia.core.Context
import io.github.kamellia.core.HttpMethod
import io.github.kamellia.core.HttpStatus
import io.github.kamellia.core.IntoResponses
import io.github.kamellia.core.PathParams
import io.github.kamellia.core.QueryParams
import io.github.kamellia.core.Request
import io.github.kamellia.dsl.get
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class KamelliaGetDslTest {
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
    fun `should register GET route that delegates to the IntoResponse encoder`() = runTest {
        val app = Kamellia()

        with(IntoResponses.plainText) {
            app.get("/hello") { "world" }
        }

        val match = app.router().match(req("/hello"))
        assertNotNull(match)
        val res = match.handler(req("/hello"))
        assertEquals(HttpStatus.OK, res.status)
        assertEquals("world", String((res.body as Body.Strict).bytes))
    }

    @Test
    fun `should resolve different IntoResponse instances per registration scope`() = runTest {
        val app = Kamellia()

        with(IntoResponses.plainText) {
            app.get("/a") { "AAA" }
        }
        with(IntoResponses.identity) {
            app.get("/b") { _ ->
                io.github.kamellia.core.Response(
                    status = HttpStatus.CREATED,
                    body = Body.Strict("BBB".toByteArray()),
                )
            }
        }

        val a = app.router().match(req("/a"))!!.handler(req("/a"))
        val b = app.router().match(req("/b"))!!.handler(req("/b"))

        assertEquals(HttpStatus.OK, a.status)
        assertEquals("AAA", String((a.body as Body.Strict).bytes))
        assertEquals(HttpStatus.CREATED, b.status)
        assertEquals("BBB", String((b.body as Body.Strict).bytes))
    }
}
