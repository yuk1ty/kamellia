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
}
