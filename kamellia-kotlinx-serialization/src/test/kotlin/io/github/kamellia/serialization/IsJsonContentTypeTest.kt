package io.github.kamellia.serialization

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IsJsonContentTypeTest {
    @Test
    fun `should accept application slash json as a JSON content type`() {
        assertTrue(isJsonContentType("application/json"))
    }

    @Test
    fun `should accept JSON content type with charset parameter`() {
        assertTrue(isJsonContentType("application/json; charset=utf-8"))
    }

    @Test
    fun `should accept content types with the +json structured suffix`() {
        assertTrue(isJsonContentType("application/vnd.api+json"))
        assertTrue(isJsonContentType("application/ld+json"))
    }

    @Test
    fun `should accept text slash json as a legacy JSON content type`() {
        assertTrue(isJsonContentType("text/json"))
    }

    @Test
    fun `should be case-insensitive when matching the JSON content type`() {
        assertTrue(isJsonContentType("Application/JSON"))
        assertTrue(isJsonContentType("APPLICATION/VND.API+JSON"))
    }

    @Test
    fun `should reject non-JSON content types`() {
        assertFalse(isJsonContentType("text/plain"))
        assertFalse(isJsonContentType("application/xml"))
        assertFalse(isJsonContentType("application/octet-stream"))
    }

    @Test
    fun `should reject a null content type`() {
        assertFalse(isJsonContentType(null))
    }
}
