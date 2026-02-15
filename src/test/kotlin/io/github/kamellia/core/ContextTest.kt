package io.github.kamellia.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ContextTest {
    @Test
    fun testSetAndGet() {
        val context = Context()
        context.set("key1", "value1")
        context.set("key2", 42)

        assertEquals("value1", context.get<String>("key1"))
        assertEquals(42, context.get<Int>("key2"))
    }

    @Test
    fun testGetNonExistentKey() {
        val context = Context()
        assertNull(context.get<String>("nonexistent"))
    }

    @Test
    fun testOverwrite() {
        val context = Context()
        context.set("key", "value1")
        context.set("key", "value2")

        assertEquals("value2", context.get<String>("key"))
    }

    @Test
    fun testRemove() {
        val context = Context()
        context.set("key", "value")
        context.remove("key")

        assertNull(context.get<String>("key"))
    }

    @Test
    fun testClear() {
        val context = Context()
        context.set("key1", "value1")
        context.set("key2", "value2")
        context.clear()

        assertNull(context.get<String>("key1"))
        assertNull(context.get<String>("key2"))
    }

    @Test
    fun testDifferentTypes() {
        val context = Context()
        context.set("string", "text")
        context.set("int", 123)
        context.set("list", listOf("a", "b", "c"))

        assertEquals("text", context.get<String>("string"))
        assertEquals(123, context.get<Int>("int"))
        assertEquals(listOf("a", "b", "c"), context.get<List<String>>("list"))
    }
}
