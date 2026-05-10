package io.github.kamellia.routing

import io.github.kamellia.core.PathParams
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class PathPatternMatcherTest {
    @Test
    fun `should return empty PathParams when matching a static path`() {
        val matcher = PathPatternMatcher("/users")
        val result = matcher.match("/users")
        assertNotNull(result)
        assertEquals(PathParams.empty(), result)
    }

    @Test
    fun `should return null when static path does not match`() {
        val matcher = PathPatternMatcher("/users")
        assertNull(matcher.match("/posts"))
    }

    @Test
    fun `should extract a single named parameter from the path`() {
        val matcher = PathPatternMatcher("/users/:id")
        val result = matcher.match("/users/123")
        assertNotNull(result)
        assertEquals("123", result.string("id"))
    }

    @Test
    fun `should extract multiple named parameters from the path`() {
        val matcher = PathPatternMatcher("/users/:userId/posts/:postId")
        val result = matcher.match("/users/456/posts/789")
        assertNotNull(result)
        assertEquals("456", result.string("userId"))
        assertEquals("789", result.string("postId"))
    }

    @Test
    fun `should accept parameter values that contain hyphen and underscore characters`() {
        val matcher = PathPatternMatcher("/users/:id")
        val result = matcher.match("/users/abc-123_xyz")
        assertNotNull(result)
        assertEquals("abc-123_xyz", result.string("id"))
    }

    @Test
    fun `should return null when path has extra segments beyond the pattern`() {
        val matcher = PathPatternMatcher("/users/:id")
        assertNull(matcher.match("/users/123/posts"))
    }

    @Test
    fun `should return null when path has missing segments compared to the pattern`() {
        val matcher = PathPatternMatcher("/users/:id/posts")
        assertNull(matcher.match("/users/123"))
    }

    @Test
    fun `should return empty PathParams when matching the root path`() {
        val matcher = PathPatternMatcher("/")
        val result = matcher.match("/")
        assertNotNull(result)
        assertEquals(PathParams.empty(), result)
    }

    @Test
    fun `should report matches via the matches predicate`() {
        val matcher = PathPatternMatcher("/users/:id")
        assertEquals(true, matcher.matches("/users/123"))
        assertEquals(false, matcher.matches("/posts/123"))
    }

    @Test
    fun `should support type-safe parameter access on PathParams`() {
        val matcher = PathPatternMatcher("/users/:id/age/:age")
        val result = matcher.match("/users/123/age/25")
        assertNotNull(result)
        assertEquals(123, result.int("id"))
        assertEquals(25, result.int("age"))
        assertNull(result.int("nonexistent"))
    }
}
