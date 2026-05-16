package io.github.kamellia.netty

import io.github.kamellia.core.Body
import io.github.kamellia.core.HttpStatus
import io.github.kamellia.core.Response
import io.netty.handler.codec.http.HttpHeaderNames
import kotlin.test.Test
import kotlin.test.assertEquals

class ResponseConverterTest {
    @Test
    fun `should write strict body bytes to FullHttpResponse`() {
        val response = Response(
            status = HttpStatus.OK,
            headers = mapOf("Content-Type" to "text/plain"),
            body = Body.Strict("hello".toByteArray()),
        )

        val nettyResponse = ResponseConverter.convertFull(response)

        assertEquals(200, nettyResponse.status().code())
        assertEquals("text/plain", nettyResponse.headers().get("Content-Type"))
        val readable = nettyResponse.content().readableBytes()
        val bytes = ByteArray(readable)
        nettyResponse.content().getBytes(0, bytes)
        assertEquals("hello", String(bytes))
        assertEquals(readable.toString(), nettyResponse.headers().get(HttpHeaderNames.CONTENT_LENGTH))
    }

    @Test
    fun `should write empty content for Body Empty`() {
        val response = Response(status = HttpStatus.NO_CONTENT)

        val nettyResponse = ResponseConverter.convertFull(response)

        assertEquals(204, nettyResponse.status().code())
        assertEquals(0, nettyResponse.content().readableBytes())
        assertEquals("0", nettyResponse.headers().get(HttpHeaderNames.CONTENT_LENGTH))
    }

    @Test
    fun `should set Content-Length header to the body byte size`() {
        val response = Response(
            status = HttpStatus.OK,
            body = Body.Strict("Test".toByteArray()),
        )

        val nettyResponse = ResponseConverter.convertFull(response)

        assertEquals(true, nettyResponse.headers().contains(HttpHeaderNames.CONTENT_LENGTH))
        assertEquals(4, nettyResponse.headers().getInt(HttpHeaderNames.CONTENT_LENGTH))
    }

    @Test
    fun `should propagate multiple response headers to the Netty response`() {
        val response = Response(
            status = HttpStatus.OK,
            headers = mapOf(
                "Content-Type" to "application/json",
                "X-Custom-Header" to "custom-value",
            ),
            body = Body.Strict("{}".toByteArray()),
        )

        val nettyResponse = ResponseConverter.convertFull(response)

        assertEquals("application/json", nettyResponse.headers().get("Content-Type"))
        assertEquals("custom-value", nettyResponse.headers().get("X-Custom-Header"))
    }

    @Test
    fun `should map HttpStatus to the matching Netty status code`() {
        val testCases = listOf(
            HttpStatus.OK to 200,
            HttpStatus.CREATED to 201,
            HttpStatus.BAD_REQUEST to 400,
            HttpStatus.NOT_FOUND to 404,
            HttpStatus.INTERNAL_SERVER_ERROR to 500,
        )

        testCases.forEach { (status, expectedCode) ->
            val response = Response(status = status)
            val nettyResponse = ResponseConverter.convertFull(response)
            assertEquals(expectedCode, nettyResponse.status().code())
        }
    }
}
