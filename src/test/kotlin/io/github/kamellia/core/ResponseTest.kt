package io.github.kamellia.core

import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ResponseTest {
    @Serializable
    data class TestData(val message: String, val count: Int)

    @Test
    fun testOkWithBody() {
        val response = Response.ok("Hello")
        assertEquals(HttpStatus.OK, response.status)
        assertEquals("text/plain; charset=utf-8", response.headers["Content-Type"])
        assertTrue(response.body is Body.Text)
        assertEquals("Hello", (response.body as Body.Text).content)
    }

    @Test
    fun testOkWithoutBody() {
        val response = Response.ok()
        assertEquals(HttpStatus.OK, response.status)
        assertTrue(response.body is Body.Empty)
    }

    @Test
    fun testJsonResponse() {
        val data = TestData(message = "test", count = 42)
        val response = Response.json(data)

        assertEquals(HttpStatus.OK, response.status)
        assertEquals("application/json; charset=utf-8", response.headers["Content-Type"])
        assertTrue(response.body is Body.Text)

        val bodyContent = (response.body as Body.Text).content
        assertTrue(bodyContent.contains("\"message\":\"test\""))
        assertTrue(bodyContent.contains("\"count\":42"))
    }

    @Test
    fun testNotFound() {
        val response = Response.notFound()
        assertEquals(HttpStatus.NOT_FOUND, response.status)
        assertEquals("Not Found", (response.body as Body.Text).content)
    }

    @Test
    fun testNotFoundWithCustomMessage() {
        val response = Response.notFound("Resource not found")
        assertEquals(HttpStatus.NOT_FOUND, response.status)
        assertEquals("Resource not found", (response.body as Body.Text).content)
    }

    @Test
    fun testInternalServerError() {
        val response = Response.internalServerError()
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.status)
        assertEquals("Internal Server Error", (response.body as Body.Text).content)
    }

    @Test
    fun testBadRequest() {
        val response = Response.badRequest("Invalid input")
        assertEquals(HttpStatus.BAD_REQUEST, response.status)
        assertEquals("Invalid input", (response.body as Body.Text).content)
    }

    @Test
    fun testCustomResponse() {
        val response =
            Response(
                status = HttpStatus.CREATED,
                headers = mapOf("Location" to "/users/123"),
                body = Body.Text("Created"),
            )

        assertEquals(HttpStatus.CREATED, response.status)
        assertEquals("/users/123", response.headers["Location"])
        assertEquals("Created", (response.body as Body.Text).content)
    }
}
