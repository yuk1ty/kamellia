package io.github.kamellia

import io.github.kamellia.core.Body
import io.github.kamellia.core.Context
import io.github.kamellia.core.HttpMethod
import io.github.kamellia.core.HttpStatus
import io.github.kamellia.core.PathParams
import io.github.kamellia.core.QueryParams
import io.github.kamellia.core.Request
import io.github.kamellia.core.Response
import io.github.kamellia.dsl.delete
import io.github.kamellia.dsl.get
import io.github.kamellia.dsl.post
import io.github.kamellia.dsl.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame

class KamelliaRoutingDslChainingTest {
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
    fun `should return the same Kamellia instance from each DSL call to support method chaining`() {
        val app = Kamellia()

        val afterGet = app.get("/") { _ -> Response(HttpStatus.OK) }
        val afterPost = afterGet.post("/users") { _ -> Response(HttpStatus.OK) }
        val afterPut = afterPost.put("/users/:id") { _ -> Response(HttpStatus.OK) }
        val afterDelete = afterPut.delete("/users/:id") { _ -> Response(HttpStatus.OK) }

        assertSame(app, afterGet)
        assertSame(app, afterPost)
        assertSame(app, afterPut)
        assertSame(app, afterDelete)
        assertEquals(4, app.router().getAllRoutes().size)
    }

    @Test
    fun `should register independent routes for the same path under different HTTP methods`() {
        val app = Kamellia()
            .get("/users") { _ -> Response(HttpStatus.OK) }
            .post("/users") { _ -> Response(HttpStatus.OK) }
            .put("/users") { _ -> Response(HttpStatus.OK) }
            .delete("/users") { _ -> Response(HttpStatus.OK) }

        val routes = app.router().getAllRoutes()
        assertEquals(4, routes.size)
        assertEquals(
            setOf(HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE),
            routes.map { it.method }.toSet(),
        )

        assertNotNull(app.router().match(req(HttpMethod.GET, "/users")))
        assertNotNull(app.router().match(req(HttpMethod.POST, "/users")))
        assertNotNull(app.router().match(req(HttpMethod.PUT, "/users")))
        assertNotNull(app.router().match(req(HttpMethod.DELETE, "/users")))
    }
}
