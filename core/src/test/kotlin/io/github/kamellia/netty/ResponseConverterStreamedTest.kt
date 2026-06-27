package io.github.kamellia.netty

import io.github.kamellia.core.Body
import io.github.kamellia.core.HttpStatus
import io.github.kamellia.core.Response
import io.netty.channel.embedded.EmbeddedChannel
import io.netty.handler.codec.http.DefaultHttpContent
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpResponse
import io.netty.handler.codec.http.LastHttpContent
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ResponseConverterStreamedTest {
    @Test
    fun `should emit chunked response when body is Streamed with no length`() = runTest {
        val channel = EmbeddedChannel()
        val response = Response(
            status = HttpStatus.OK,
            body = Body.Streamed(flowOf("a".toByteArray(), "bc".toByteArray()), length = null),
        )

        ResponseConverter.writeStreamed(channel, response)

        val head = channel.readOutbound<HttpResponse>()
        assertEquals(200, head.status().code())
        assertEquals("chunked", head.headers().get(HttpHeaderNames.TRANSFER_ENCODING))

        val first = channel.readOutbound<DefaultHttpContent>()
        val firstBytes = ByteArray(first.content().readableBytes())
        first.content().getBytes(0, firstBytes)
        assertEquals("a", String(firstBytes))

        val second = channel.readOutbound<DefaultHttpContent>()
        val secondBytes = ByteArray(second.content().readableBytes())
        second.content().getBytes(0, secondBytes)
        assertEquals("bc", String(secondBytes))

        val last = channel.readOutbound<Any>()
        assertTrue(last is LastHttpContent)
    }
}
