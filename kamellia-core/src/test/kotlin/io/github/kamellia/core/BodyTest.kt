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

        val length = body.length

        assertEquals(0L, length)
    }

    @Test
    fun `should hold raw bytes and report their size when body is Strict`() {
        val bytes = "hello".toByteArray()
        val body: Body = Body.Strict(bytes)

        val length = body.length
        val held = (body as Body.Strict).bytes

        assertEquals(5L, length)
        assertContentEquals(bytes, held)
    }

    @Test
    fun `should carry a flow source and optional length when body is Streamed`() = runTest {
        val flow = flowOf("a".toByteArray(), "b".toByteArray())
        val body = Body.Streamed(flow, length = null)

        val length = body.length
        val collected = body.source.toList()

        assertNull(length)
        assertEquals(2, collected.size)
        assertContentEquals("a".toByteArray(), collected[0])
        assertContentEquals("b".toByteArray(), collected[1])
    }
}
