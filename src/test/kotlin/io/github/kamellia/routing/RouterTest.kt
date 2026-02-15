package io.github.kamellia.routing

import io.github.kamellia.core.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class RouterTest {
    @Test
    fun testAddRoute() {
        val router = Router()
        router.addRoute(HttpMethod.GET, "/users") { Response.ok("users") }

        val routes = router.getAllRoutes()
        assertEquals(1, routes.size)
        assertEquals(HttpMethod.GET, routes[0].method)
        assertEquals("/users", routes[0].pattern)
    }

    @Test
    fun testMatchStaticRoute() = runTest {
        val router = Router()
        router.addRoute(HttpMethod.GET, "/users") { Response.ok("users") }

        val request =
            Request(
                method = HttpMethod.GET,
                path = "/users",
                headers = emptyMap(),
                queryParams = emptyMap(),
                pathParams = emptyMap(),
                body = Body.Empty,
                context = Context(),
            )

        val match = router.match(request)
        assertNotNull(match)
        assertEquals(0, match.pathParams.size)
    }

    @Test
    fun testMatchWithPathParams() = runTest {
        val router = Router()
        router.addRoute(HttpMethod.GET, "/users/{id}") { Response.ok("user") }

        val request =
            Request(
                method = HttpMethod.GET,
                path = "/users/123",
                headers = emptyMap(),
                queryParams = emptyMap(),
                pathParams = emptyMap(),
                body = Body.Empty,
                context = Context(),
            )

        val match = router.match(request)
        assertNotNull(match)
        assertEquals(1, match.pathParams.size)
        assertEquals("123", match.pathParams["id"])
    }

    @Test
    fun testNoMatchWrongMethod() {
        val router = Router()
        router.addRoute(HttpMethod.GET, "/users") { Response.ok("users") }

        val request =
            Request(
                method = HttpMethod.POST,
                path = "/users",
                headers = emptyMap(),
                queryParams = emptyMap(),
                pathParams = emptyMap(),
                body = Body.Empty,
                context = Context(),
            )

        val match = router.match(request)
        assertNull(match)
    }

    @Test
    fun testNoMatchWrongPath() {
        val router = Router()
        router.addRoute(HttpMethod.GET, "/users") { Response.ok("users") }

        val request =
            Request(
                method = HttpMethod.GET,
                path = "/posts",
                headers = emptyMap(),
                queryParams = emptyMap(),
                pathParams = emptyMap(),
                body = Body.Empty,
                context = Context(),
            )

        val match = router.match(request)
        assertNull(match)
    }

    @Test
    fun testMultipleRoutes() = runTest {
        val router = Router()
        router.addRoute(HttpMethod.GET, "/users") { Response.ok("users") }
        router.addRoute(HttpMethod.GET, "/posts") { Response.ok("posts") }
        router.addRoute(HttpMethod.POST, "/users") { Response.ok("create user") }

        val request1 =
            Request(
                method = HttpMethod.GET,
                path = "/users",
                headers = emptyMap(),
                queryParams = emptyMap(),
                pathParams = emptyMap(),
                body = Body.Empty,
                context = Context(),
            )
        assertNotNull(router.match(request1))

        val request2 =
            Request(
                method = HttpMethod.GET,
                path = "/posts",
                headers = emptyMap(),
                queryParams = emptyMap(),
                pathParams = emptyMap(),
                body = Body.Empty,
                context = Context(),
            )
        assertNotNull(router.match(request2))

        val request3 =
            Request(
                method = HttpMethod.POST,
                path = "/users",
                headers = emptyMap(),
                queryParams = emptyMap(),
                pathParams = emptyMap(),
                body = Body.Empty,
                context = Context(),
            )
        assertNotNull(router.match(request3))
    }

    @Test
    fun testFirstMatchWins() = runTest {
        val router = Router()
        router.addRoute(HttpMethod.GET, "/users/{id}") { Response.ok("specific") }
        router.addRoute(HttpMethod.GET, "/users/{name}") { Response.ok("general") }

        val request =
            Request(
                method = HttpMethod.GET,
                path = "/users/123",
                headers = emptyMap(),
                queryParams = emptyMap(),
                pathParams = emptyMap(),
                body = Body.Empty,
                context = Context(),
            )

        val match = router.match(request)
        assertNotNull(match)
        val response = match.handler(request.copy(pathParams = match.pathParams))
        assertEquals("specific", (response.body as Body.Text).content)
    }
}
