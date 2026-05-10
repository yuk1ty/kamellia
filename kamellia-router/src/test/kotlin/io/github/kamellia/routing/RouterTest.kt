package io.github.kamellia.routing

import io.github.kamellia.core.Body
import io.github.kamellia.core.Context
import io.github.kamellia.core.HttpMethod
import io.github.kamellia.core.HttpStatus
import io.github.kamellia.core.PathParams
import io.github.kamellia.core.QueryParams
import io.github.kamellia.core.Request
import io.github.kamellia.core.Response
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class RouterTest {
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
    fun `should expose registered routes via getAllRoutes`() {
        val router = Router()
        router.addRoute(HttpMethod.GET, "/users") { Response(HttpStatus.OK) }

        val routes = router.getAllRoutes()
        assertEquals(1, routes.size)
        assertEquals(HttpMethod.GET, routes[0].method)
        assertEquals("/users", routes[0].pattern)
    }

    @Test
    fun `should return matching RouteMatch with raw handler when path matches`() = runTest {
        val router = Router()
        router.addRoute(HttpMethod.GET, "/users/:id") {
            Response(
                status = HttpStatus.OK,
                body = Body.Strict(it.pathParams.string("id").orEmpty().toByteArray()),
            )
        }

        val match = router.match(req(HttpMethod.GET, "/users/123"))
        assertNotNull(match)
        assertEquals("123", match.pathParams.string("id"))

        val response = match.handler(req(HttpMethod.GET, "/users/123").copy(pathParams = match.pathParams))
        val body = response.body
        check(body is Body.Strict)
        assertEquals("123", body.bytes.decodeToString())
    }

    @Test
    fun `should return null when no route matches`() {
        val router = Router()
        router.addRoute(HttpMethod.GET, "/users") { Response(HttpStatus.OK) }

        assertNull(router.match(req(HttpMethod.POST, "/users")))
        assertNull(router.match(req(HttpMethod.GET, "/posts")))
    }

    @Test
    fun `should pick the first registered route when multiple patterns match`() = runTest {
        val router = Router()
        router.addRoute(HttpMethod.GET, "/users/:id") {
            Response(HttpStatus.OK, body = Body.Strict("specific".toByteArray()))
        }
        router.addRoute(HttpMethod.GET, "/users/:name") {
            Response(HttpStatus.OK, body = Body.Strict("general".toByteArray()))
        }

        val match = router.match(req(HttpMethod.GET, "/users/123"))
        assertNotNull(match)
        val response = match.handler(req(HttpMethod.GET, "/users/123").copy(pathParams = match.pathParams))
        val body = response.body
        check(body is Body.Strict)
        assertEquals("specific", body.bytes.decodeToString())
    }
}
