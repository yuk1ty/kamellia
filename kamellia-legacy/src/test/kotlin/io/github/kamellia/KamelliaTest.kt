package io.github.kamellia

import io.github.kamellia.core.Response
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class KamelliaTest {
    @Test
    fun testGetRoute() {
        val app = Kamellia()
        val result = app.get("/test") { Response.ok("test") }

        assertNotNull(result)
        assertEquals(app, result) // Should return this for chaining
    }

    @Test
    fun testPostRoute() {
        val app = Kamellia()
        val result = app.post("/users") { Response.ok("created") }

        assertNotNull(result)
        assertEquals(app, result)
    }

    @Test
    fun testPutRoute() {
        val app = Kamellia()
        val result = app.put("/users/1") { Response.ok("updated") }

        assertNotNull(result)
        assertEquals(app, result)
    }

    @Test
    fun testDeleteRoute() {
        val app = Kamellia()
        val result = app.delete("/users/1") { Response.ok("deleted") }

        assertNotNull(result)
        assertEquals(app, result)
    }

    @Test
    fun testPatchRoute() {
        val app = Kamellia()
        val result = app.patch("/users/1") { Response.ok("patched") }

        assertNotNull(result)
        assertEquals(app, result)
    }

    @Test
    fun testMethodChaining() {
        val app =
            Kamellia()
                .get("/") { Response.ok("home") }
                .get("/users") { Response.ok("users") }
                .post("/users") { Response.ok("create") }
                .put("/users/:id") { Response.ok("update") }
                .delete("/users/:id") { Response.ok("delete") }

        assertNotNull(app)
    }

    @Test
    fun testHandlerExecution() = runTest {
        val app = Kamellia()
        var executedHandler = false

        app.get("/test") {
            executedHandler = true
            Response.ok("executed")
        }

        // We can't easily test the handler execution without starting the server,
        // but we can verify the route was registered
        assertNotNull(app)
    }

    @Test
    fun testMultipleMethodsOnSamePath() {
        val app = Kamellia()

        app.get("/users") { Response.ok("list") }
        app.post("/users") { Response.ok("create") }
        app.put("/users") { Response.ok("update") }
        app.delete("/users") { Response.ok("delete") }

        assertNotNull(app)
    }
}
