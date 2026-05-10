package io.github.kamellia.routing

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PathPatternMatcherTest {
    @Test
    fun `should match a static path exactly`() {
        val matcher = PathPatternMatcher("/health")

        assertTrue(matcher.matches("/health"))
        assertFalse(matcher.matches("/healthz"))
    }

    @Test
    fun `should extract a single path parameter`() {
        val matcher = PathPatternMatcher("/users/:id")

        val params = matcher.match("/users/42")

        assertNotNull(params)
        assertEquals("42", params.string("id"))
    }

    @Test
    fun `should extract multiple path parameters`() {
        val matcher = PathPatternMatcher("/users/:userId/posts/:postId")

        val params = matcher.match("/users/u1/posts/p2")

        assertNotNull(params)
        assertEquals("u1", params.string("userId"))
        assertEquals("p2", params.string("postId"))
    }

    @Test
    fun `should not match when segment count differs`() {
        val matcher = PathPatternMatcher("/users/:id")

        assertNull(matcher.match("/users/42/extra"))
    }
}
