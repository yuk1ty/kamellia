package io.github.kamellia

import io.github.kamellia.core.Body
import io.github.kamellia.core.Context
import io.github.kamellia.core.HttpMethod
import io.github.kamellia.core.HttpStatus
import io.github.kamellia.core.PathParams
import io.github.kamellia.core.QueryParams
import io.github.kamellia.core.Request
import io.github.kamellia.core.Text
import io.github.kamellia.dsl.delete
import io.github.kamellia.dsl.patch
import io.github.kamellia.dsl.post
import io.github.kamellia.dsl.put
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class KamelliaHttpMethodsDslTest {
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
    fun `should register POST route that calls intoResponse on the returned value`() = runTest {
        val app = Kamellia()
        app.post("/items") { _ -> Text("created") }

        val match = app.router().match(req(HttpMethod.POST, "/items"))
        assertNotNull(match)
        val res = match.handler(req(HttpMethod.POST, "/items"))

        assertEquals(HttpStatus.OK, res.status)
        check(res.body is Body.Strict)
        assertEquals("created", (res.body as Body.Strict).bytes.decodeToString())
    }

    @Test
    fun `should register PUT route that calls intoResponse on the returned value`() = runTest {
        val app = Kamellia()
        app.put("/items/1") { _ -> Text("updated") }

        val match = app.router().match(req(HttpMethod.PUT, "/items/1"))
        assertNotNull(match)
        val res = match.handler(req(HttpMethod.PUT, "/items/1"))
        assertEquals("updated", (res.body as Body.Strict).bytes.decodeToString())
    }

    @Test
    fun `should register DELETE route that calls intoResponse on the returned value`() = runTest {
        val app = Kamellia()
        app.delete("/items/1") { _ -> Text("deleted") }

        val match = app.router().match(req(HttpMethod.DELETE, "/items/1"))
        assertNotNull(match)
        val res = match.handler(req(HttpMethod.DELETE, "/items/1"))
        assertEquals("deleted", (res.body as Body.Strict).bytes.decodeToString())
    }

    @Test
    fun `should register PATCH route that calls intoResponse on the returned value`() = runTest {
        val app = Kamellia()
        app.patch("/items/1") { _ -> Text("patched") }

        val match = app.router().match(req(HttpMethod.PATCH, "/items/1"))
        assertNotNull(match)
        val res = match.handler(req(HttpMethod.PATCH, "/items/1"))
        assertEquals("patched", (res.body as Body.Strict).bytes.decodeToString())
    }
}
