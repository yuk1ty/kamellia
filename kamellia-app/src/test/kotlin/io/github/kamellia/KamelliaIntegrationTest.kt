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

class KamelliaIntegrationTest {
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
    fun `should serve plain text when handler returns String under plainText scope`() = runTest {
        val app = Kamellia()

        with(IntoResponses.plainText) {
            app.get("/") { _ -> "hello" }
        }

        val match = app.router().match(req(HttpMethod.GET, "/"))
        assertNotNull(match)
        val res = match.handler(req(HttpMethod.GET, "/"))
        assertEquals(HttpStatus.OK, res.status)
        assertEquals("hello", String((res.body as Body.Strict).bytes))
    }
}
