package io.github.kamellia.netty

import io.github.kamellia.core.Body
import io.github.kamellia.core.Response
import io.netty.buffer.Unpooled
import io.netty.handler.codec.http.*
import java.nio.charset.StandardCharsets

object ResponseConverter {
    fun convert(response: Response): FullHttpResponse {
        // Convert body to ByteBuf
        val content =
            when (val body = response.body) {
                is Body.Text -> Unpooled.copiedBuffer(body.content, StandardCharsets.UTF_8)
                is Body.Binary -> Unpooled.copiedBuffer(body.bytes)
                Body.Empty -> Unpooled.EMPTY_BUFFER
            }

        val nettyResponse =
            DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.valueOf(response.status.code),
                content,
            )

        // Set headers
        response.headers.forEach { (name, value) ->
            nettyResponse.headers().set(name, value)
        }

        // Set content-length
        nettyResponse.headers().setInt(
            HttpHeaderNames.CONTENT_LENGTH,
            content.readableBytes(),
        )

        return nettyResponse
    }
}
