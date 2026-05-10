package io.github.kamellia.netty

import io.github.kamellia.core.Body
import io.github.kamellia.core.Response
import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.handler.codec.http.DefaultFullHttpResponse
import io.netty.handler.codec.http.DefaultHttpContent
import io.netty.handler.codec.http.DefaultHttpResponse
import io.netty.handler.codec.http.FullHttpResponse
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.HttpVersion
import io.netty.handler.codec.http.LastHttpContent
import kotlinx.coroutines.flow.collect

object ResponseConverter {
    fun convertFull(response: Response): FullHttpResponse {
        val content = when (val body = response.body) {
            is Body.Strict -> Unpooled.copiedBuffer(body.bytes)
            Body.Empty -> Unpooled.EMPTY_BUFFER
            is Body.Streamed -> error("Streamed body must be written via writeStreamed, not convertFull")
        }

        val nettyResponse = DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1,
            HttpResponseStatus.valueOf(response.status.code),
            content,
        )

        response.headers.forEach { (name, value) ->
            nettyResponse.headers().set(name, value)
        }

        nettyResponse.headers().setInt(
            HttpHeaderNames.CONTENT_LENGTH,
            content.readableBytes(),
        )

        return nettyResponse
    }

    suspend fun writeStreamed(channel: Channel, response: Response) {
        val body = response.body
        require(body is Body.Streamed) { "writeStreamed expects Body.Streamed" }

        val head = DefaultHttpResponse(
            HttpVersion.HTTP_1_1,
            HttpResponseStatus.valueOf(response.status.code),
        )
        response.headers.forEach { (name, value) ->
            head.headers().set(name, value)
        }
        if (body.length != null) {
            head.headers().set(HttpHeaderNames.CONTENT_LENGTH, body.length)
        } else {
            head.headers().set(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED)
        }
        channel.writeAndFlush(head)

        body.source.collect { chunk ->
            channel.writeAndFlush(DefaultHttpContent(Unpooled.copiedBuffer(chunk)))
        }

        channel.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT)
    }
}
