package io.github.kamellia.routing

import io.github.kamellia.core.PathParams
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class PathPatternMatcherTest {
    @Test
    fun testStaticPath() {
        val matcher = PathPatternMatcher("/users")
        val result = matcher.match("/users")
        assertNotNull(result)
        assertEquals(PathParams.empty(), result)
    }

    @Test
    fun testStaticPathNoMatch() {
        val matcher = PathPatternMatcher("/users")
        val result = matcher.match("/posts")
        assertNull(result)
    }

    @Test
    fun testSingleParameter() {
        val matcher = PathPatternMatcher("/users/{id}")
        val result = matcher.match("/users/123")
        assertNotNull(result)
        assertEquals("123", result.string("id"))
    }

    @Test
    fun testMultipleParameters() {
        val matcher = PathPatternMatcher("/users/{userId}/posts/{postId}")
        val result = matcher.match("/users/456/posts/789")
        assertNotNull(result)
        assertEquals("456", result.string("userId"))
        assertEquals("789", result.string("postId"))
    }

    @Test
    fun testParameterWithSpecialCharacters() {
        val matcher = PathPatternMatcher("/users/{id}")
        val result = matcher.match("/users/abc-123_xyz")
        assertNotNull(result)
        assertEquals("abc-123_xyz", result.string("id"))
    }

    @Test
    fun testNoMatchWithExtraSegments() {
        val matcher = PathPatternMatcher("/users/{id}")
        val result = matcher.match("/users/123/posts")
        assertNull(result)
    }

    @Test
    fun testNoMatchWithMissingSegments() {
        val matcher = PathPatternMatcher("/users/{id}/posts")
        val result = matcher.match("/users/123")
        assertNull(result)
    }

    @Test
    fun testRootPath() {
        val matcher = PathPatternMatcher("/")
        val result = matcher.match("/")
        assertNotNull(result)
        assertEquals(PathParams.empty(), result)
    }

    @Test
    fun testMatches() {
        val matcher = PathPatternMatcher("/users/{id}")
        assertEquals(true, matcher.matches("/users/123"))
        assertEquals(false, matcher.matches("/posts/123"))
    }

    @Test
    fun testTypeSafeParameterAccess() {
        val matcher = PathPatternMatcher("/users/{id}/age/{age}")
        val result = matcher.match("/users/123/age/25")
        assertNotNull(result)
        assertEquals(123, result.int("id"))
        assertEquals(25, result.int("age"))
        assertNull(result.int("nonexistent"))
    }
}
