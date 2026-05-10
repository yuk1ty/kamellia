package io.github.kamellia.core

import kotlin.test.Test
import kotlin.test.assertSame

class ResponseIntoResponseTest {
    @Test
    fun `should return self when intoResponse is invoked on a Response`() {
        val res = Response(
            status = HttpStatus.OK,
            body = Body.Strict("x".toByteArray()),
        )

        val converted: Response = res.intoResponse()

        assertSame(res, converted)
    }
}
