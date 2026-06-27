package io.github.kamellia.netty

import io.github.kamellia.core.Body
import io.github.kamellia.core.HttpMethod
import io.netty.buffer.Unpooled
import io.netty.handler.codec.http.DefaultFullHttpRequest
import io.netty.handler.codec.http.HttpVersion
import java.nio.charset.StandardCharsets
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import io.netty.handler.codec.http.HttpMethod as NettyHttpMethod

class RequestConverterTest {
    @Test
    fun `should convert GET request with no body into Request with Body Empty`() {
        val nettyRequest = DefaultFullHttpRequest(
            HttpVersion.HTTP_1_1,
            NettyHttpMethod.GET,
            "/users",
        )
        nettyRequest.headers().set("Host", "localhost")
        nettyRequest.headers().set("Content-Type", "text/plain")

        val request = RequestConverter.convert(nettyRequest)

        assertEquals(HttpMethod.GET, request.method)
        assertEquals("/users", request.path)
        assertEquals("localhost", request.headers["host"])
        assertEquals("text/plain", request.headers["content-type"])
        assertTrue(request.body is Body.Empty)
    }

    @Test
    fun `should convert POST request body bytes into Body Strict`() {
        val content = """{"name":"John"}"""
        val nettyRequest = DefaultFullHttpRequest(
            HttpVersion.HTTP_1_1,
            NettyHttpMethod.POST,
            "/users",
            Unpooled.copiedBuffer(content, StandardCharsets.UTF_8),
        )
        nettyRequest.headers().set("Content-Type", "application/json")

        val request = RequestConverter.convert(nettyRequest)

        assertEquals(HttpMethod.POST, request.method)
        assertEquals("/users", request.path)
        val body = request.body
        check(body is Body.Strict)
        assertEquals(content, body.bytes.toString(StandardCharsets.UTF_8))
    }

    @Test
    fun `should extract query parameters from the request URI`() {
        val nettyRequest = DefaultFullHttpRequest(
            HttpVersion.HTTP_1_1,
            NettyHttpMethod.GET,
            "/search?q=kotlin&page=1",
        )

        val request = RequestConverter.convert(nettyRequest)

        assertEquals("/search", request.path)
        assertEquals(listOf("kotlin"), request.queryParams.list("q"))
        assertEquals(listOf("1"), request.queryParams.list("page"))
    }

    @Test
    fun `should preserve binary body bytes via Body Strict`() {
        val bytes = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        val nettyRequest = DefaultFullHttpRequest(
            HttpVersion.HTTP_1_1,
            NettyHttpMethod.POST,
            "/upload",
            Unpooled.copiedBuffer(bytes),
        )
        nettyRequest.headers().set("Content-Type", "application/octet-stream")

        val request = RequestConverter.convert(nettyRequest)

        val body = request.body
        check(body is Body.Strict)
        assertEquals(4, body.bytes.size)
        assertEquals(0x01.toByte(), body.bytes[0])
    }

    @Test
    fun `should collect repeated query parameter values into a list`() {
        val nettyRequest = DefaultFullHttpRequest(
            HttpVersion.HTTP_1_1,
            NettyHttpMethod.GET,
            "/search?tag=kotlin&tag=java&tag=rust",
        )

        val request = RequestConverter.convert(nettyRequest)

        assertEquals(listOf("kotlin", "java", "rust"), request.queryParams.list("tag"))
    }

    @Test
    fun `should lowercase header names when copying from Netty headers`() {
        val nettyRequest = DefaultFullHttpRequest(
            HttpVersion.HTTP_1_1,
            NettyHttpMethod.GET,
            "/",
        )
        nettyRequest.headers().set("Content-Type", "application/json")
        nettyRequest.headers().set("Authorization", "Bearer token123")

        val request = RequestConverter.convert(nettyRequest)

        assertEquals("application/json", request.headers["content-type"])
        assertEquals("Bearer token123", request.headers["authorization"])
    }
}
