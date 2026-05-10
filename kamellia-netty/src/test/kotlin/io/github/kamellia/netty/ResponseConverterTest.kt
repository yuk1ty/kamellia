package io.github.kamellia.netty

import io.github.kamellia.core.Body
import io.github.kamellia.core.HttpStatus
import io.github.kamellia.core.Response
import io.netty.channel.embedded.EmbeddedChannel
import io.netty.handler.codec.http.DefaultHttpResponse
import io.netty.handler.codec.http.HttpContent
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import io.netty.handler.codec.http.LastHttpContent
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import java.nio.charset.StandardCharsets
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ResponseConverterTest {
    @Test
    fun `should write strict body bytes to FullHttpResponse`() {
        val response = Response(
            status = HttpStatus.OK,
            headers = mapOf("Content-Type" to "text/plain"),
            body = Body.Strict("Hello".toByteArray()),
        )

        val nettyResponse = ResponseConverter.convertFull(response)

        assertEquals(200, nettyResponse.status().code())
        assertEquals("text/plain", nettyResponse.headers().get("Content-Type"))
        assertEquals(5, nettyResponse.headers().getInt(HttpHeaderNames.CONTENT_LENGTH))
        assertEquals("Hello", nettyResponse.content().toString(StandardCharsets.UTF_8))
    }

    @Test
    fun `should write empty content for Body Empty`() {
        val response = Response(
            status = HttpStatus.NO_CONTENT,
            headers = emptyMap(),
            body = Body.Empty,
        )

        val nettyResponse = ResponseConverter.convertFull(response)

        assertEquals(204, nettyResponse.status().code())
        assertEquals(0, nettyResponse.content().readableBytes())
    }

    @Test
    fun `should emit chunked response when body is Streamed with no length`() = runTest {
        val flow = flowOf("a".toByteArray(), "bc".toByteArray())
        val response = Response(
            status = HttpStatus.OK,
            headers = mapOf("Content-Type" to "text/plain"),
            body = Body.Streamed(flow, length = null),
        )
        val channel = EmbeddedChannel()

        ResponseConverter.writeStreamed(channel, response)

        val outbound = generateSequence { channel.readOutbound<Any?>() }.toList()
        val head = outbound.firstOrNull() as? DefaultHttpResponse
        assertTrue(head != null)
        assertEquals(200, head.status().code())
        assertEquals(HttpHeaderValues.CHUNKED.toString(), head.headers().get(HttpHeaderNames.TRANSFER_ENCODING))

        val chunks = outbound.drop(1).filterIsInstance<HttpContent>()
        val nonLastChunks = chunks.filter { it !is LastHttpContent }
        val combined = nonLastChunks.joinToString("") { it.content().toString(StandardCharsets.UTF_8) }
        assertEquals("abc", combined)

        val last = chunks.last()
        assertTrue(last is LastHttpContent)

        channel.finishAndReleaseAll()
    }
}
