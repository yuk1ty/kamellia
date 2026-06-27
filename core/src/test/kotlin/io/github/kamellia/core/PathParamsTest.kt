package io.github.kamellia.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class PathParamsTest {
    @Test
    fun testIntConversion() {
        val params = PathParams.of(mapOf("id" to "123", "invalid" to "abc"))

        assertEquals(123, params.int("id"))
        assertNull(params.int("invalid"))
        assertNull(params.int("nonexistent"))
    }

    @Test
    fun testLongConversion() {
        val params = PathParams.of(mapOf("userId" to "9876543210", "invalid" to "xyz"))

        assertEquals(9876543210L, params.long("userId"))
        assertNull(params.long("invalid"))
        assertNull(params.long("nonexistent"))
    }

    @Test
    fun testStringConversion() {
        val params = PathParams.of(mapOf("name" to "john", "empty" to ""))

        assertEquals("john", params.string("name"))
        assertEquals("", params.string("empty"))
        assertNull(params.string("nonexistent"))
    }

    @Test
    fun testBooleanConversion() {
        val params = PathParams.of(mapOf("flag1" to "true", "flag2" to "false", "invalid" to "yes"))

        assertEquals(true, params.boolean("flag1"))
        assertEquals(false, params.boolean("flag2"))
        assertNull(params.boolean("invalid"))
        assertNull(params.boolean("nonexistent"))
    }

    @Test
    fun testDoubleConversion() {
        val params = PathParams.of(mapOf("price" to "123.45", "invalid" to "abc"))

        assertEquals(123.45, params.double("price"))
        assertNull(params.double("invalid"))
        assertNull(params.double("nonexistent"))
    }

    @Test
    fun testGetWithTypeInference() {
        val params = PathParams.of(mapOf("id" to "42", "name" to "test", "active" to "true"))

        val id: Int? = params.get("id")
        assertEquals(42, id)

        val name: String? = params.get("name")
        assertEquals("test", name)

        val active: Boolean? = params.get("active")
        assertEquals(true, active)

        val missing: Int? = params.get("nonexistent")
        assertNull(missing)
    }

    @Test
    fun testGetWithInvalidType() {
        val params = PathParams.of(mapOf("value" to "test"))

        val result: Int? = params.get("value")
        assertNull(result)
    }

    @Test
    fun testEmpty() {
        val params = PathParams.empty()

        assertNull(params.int("any"))
        assertNull(params.string("any"))
        assertNull(params.long("any"))
    }

    @Test
    fun testEquals() {
        val params1 = PathParams.of(mapOf("id" to "123"))
        val params2 = PathParams.of(mapOf("id" to "123"))
        val params3 = PathParams.of(mapOf("id" to "456"))

        assertEquals(params1, params2)
        assertNotEquals(params1, params3)
    }

    @Test
    fun testHashCode() {
        val params1 = PathParams.of(mapOf("id" to "123"))
        val params2 = PathParams.of(mapOf("id" to "123"))

        assertEquals(params1.hashCode(), params2.hashCode())
    }
}
