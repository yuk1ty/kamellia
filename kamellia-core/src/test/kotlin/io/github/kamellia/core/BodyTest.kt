package io.github.kamellia.core

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BodyTest {
    @Test
    fun `should expose zero length when body is Empty`() {
        val body: Body = Body.Empty

        assertEquals(0L, body.length)
    }

    @Test
    fun `should hold raw bytes and report their size when body is Strict`() {
        val bytes = "hello".toByteArray()
        val body: Body = Body.Strict(bytes)

        assertEquals(5L, body.length)
        assertContentEquals(bytes, (body as Body.Strict).bytes)
    }

    @Test
    fun `should carry a flow source and optional length when body is Streamed`() = runTest {
        val first = "a".toByteArray()
        val second = "b".toByteArray()
        val body = Body.Streamed(flowOf(first, second), length = null)

        assertNull(body.length)
        val collected = body.source.toList()
        assertEquals(2, collected.size)
        assertContentEquals(first, collected[0])
        assertContentEquals(second, collected[1])
    }
}
