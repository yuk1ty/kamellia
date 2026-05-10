package io.github.kamellia

import kotlin.test.Test
import kotlin.test.assertEquals

class KamelliaConstructorTest {
    @Test
    fun `should keep Kamellia constructible with no routes`() {
        val app = Kamellia()

        assertEquals(0, app.router().getAllRoutes().size)
        assertEquals(0, app.middlewares().size)
    }
}
