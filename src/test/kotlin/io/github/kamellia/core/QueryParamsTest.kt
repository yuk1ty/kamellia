package io.github.kamellia.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class QueryParamsTest {
    @Test
    fun testIntConversion() {
        val params = QueryParams.of(mapOf("page" to listOf("1"), "invalid" to listOf("abc")))

        assertEquals(1, params.int("page"))
        assertNull(params.int("invalid"))
        assertNull(params.int("nonexistent"))
    }

    @Test
    fun testLongConversion() {
        val params = QueryParams.of(mapOf("userId" to listOf("9876543210"), "invalid" to listOf("xyz")))

        assertEquals(9876543210L, params.long("userId"))
        assertNull(params.long("invalid"))
        assertNull(params.long("nonexistent"))
    }

    @Test
    fun testStringConversion() {
        val params = QueryParams.of(mapOf("q" to listOf("kotlin"), "empty" to listOf("")))

        assertEquals("kotlin", params.string("q"))
        assertEquals("", params.string("empty"))
        assertNull(params.string("nonexistent"))
    }

    @Test
    fun testBooleanConversion() {
        val params = QueryParams.of(
            mapOf("flag1" to listOf("true"), "flag2" to listOf("false"), "invalid" to listOf("yes")),
        )

        assertEquals(true, params.boolean("flag1"))
        assertEquals(false, params.boolean("flag2"))
        assertNull(params.boolean("invalid"))
        assertNull(params.boolean("nonexistent"))
    }

    @Test
    fun testDoubleConversion() {
        val params = QueryParams.of(mapOf("price" to listOf("123.45"), "invalid" to listOf("abc")))

        assertEquals(123.45, params.double("price"))
        assertNull(params.double("invalid"))
        assertNull(params.double("nonexistent"))
    }

    @Test
    fun testListConversion() {
        val params = QueryParams.of(mapOf("tags" to listOf("kotlin", "java", "rust")))

        assertEquals(listOf("kotlin", "java", "rust"), params.list("tags"))
        assertTrue(params.list("nonexistent").isEmpty())
    }

    @Test
    fun testIntListConversion() {
        val params = QueryParams.of(mapOf("ids" to listOf("1", "2", "3", "invalid")))

        assertEquals(listOf(1, 2, 3), params.intList("ids"))
        assertTrue(params.intList("nonexistent").isEmpty())
    }

    @Test
    fun testLongListConversion() {
        val params = QueryParams.of(mapOf("refs" to listOf("100", "200", "300", "invalid")))

        assertEquals(listOf(100L, 200L, 300L), params.longList("refs"))
        assertTrue(params.longList("nonexistent").isEmpty())
    }

    @Test
    fun testGetWithTypeInference() {
        val params = QueryParams.of(mapOf("page" to listOf("42"), "q" to listOf("test"), "active" to listOf("true")))

        val page: Int? = params.get("page")
        assertEquals(42, page)

        val query: String? = params.get("q")
        assertEquals("test", query)

        val active: Boolean? = params.get("active")
        assertEquals(true, active)

        val missing: Int? = params.get("nonexistent")
        assertNull(missing)
    }

    @Test
    fun testGetWithInvalidType() {
        val params = QueryParams.of(mapOf("value" to listOf("test")))

        val result: Int? = params.get("value")
        assertNull(result)
    }

    @Test
    fun testMultipleValuesFirstIsUsed() {
        val params = QueryParams.of(mapOf("id" to listOf("1", "2", "3")))

        // 単一値取得は最初の値を返す
        assertEquals(1, params.int("id"))
        assertEquals("1", params.string("id"))
    }

    @Test
    fun testEmpty() {
        val params = QueryParams.empty()

        assertNull(params.int("any"))
        assertNull(params.string("any"))
        assertTrue(params.list("any").isEmpty())
        assertTrue(params.intList("any").isEmpty())
    }

    @Test
    fun testEquals() {
        val params1 = QueryParams.of(mapOf("id" to listOf("123")))
        val params2 = QueryParams.of(mapOf("id" to listOf("123")))
        val params3 = QueryParams.of(mapOf("id" to listOf("456")))

        assertEquals(params1, params2)
        assertNotEquals(params1, params3)
    }

    @Test
    fun testHashCode() {
        val params1 = QueryParams.of(mapOf("id" to listOf("123")))
        val params2 = QueryParams.of(mapOf("id" to listOf("123")))

        assertEquals(params1.hashCode(), params2.hashCode())
    }
}
