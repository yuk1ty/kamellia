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
    fun `should return matching RouteMatch with raw handler when path matches`() = runTest {
        val router = Router()
        val handler: RawHandler = { Response(status = HttpStatus.OK, body = Body.Strict("hi".toByteArray())) }
        router.addRoute(HttpMethod.GET, "/users/:id", handler)

        val match = router.match(req(HttpMethod.GET, "/users/42"))

        assertNotNull(match)
        assertEquals("42", match.pathParams.string("id"))
        val res = match.handler(req(HttpMethod.GET, "/users/42"))
        assertEquals(HttpStatus.OK, res.status)
    }

    @Test
    fun `should return null when no route matches`() {
        val router = Router()
        router.addRoute(HttpMethod.GET, "/users/:id") { Response(HttpStatus.OK) }

        val match = router.match(req(HttpMethod.POST, "/users/42"))

        assertNull(match)
    }
}
