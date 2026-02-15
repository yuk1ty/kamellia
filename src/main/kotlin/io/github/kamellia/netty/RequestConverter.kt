package io.github.kamellia.netty

import io.github.kamellia.core.*
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.QueryStringDecoder
import java.nio.charset.StandardCharsets

object RequestConverter {
    fun convert(nettyRequest: FullHttpRequest): Request {
        val decoder = QueryStringDecoder(nettyRequest.uri())

        // Extract headers
        val headers = mutableMapOf<String, String>()
        nettyRequest.headers().forEach { entry ->
            headers[entry.key.lowercase()] = entry.value
        }

        // Extract query parameters
        val queryParams = decoder.parameters().mapValues { it.value }

        // Extract body
        val body =
            if (nettyRequest.content().readableBytes() > 0) {
                val bytes = ByteArray(nettyRequest.content().readableBytes())
                nettyRequest.content().getBytes(0, bytes)

                // Try to decode as text if content-type suggests it
                val contentType = headers["content-type"] ?: ""
                if (contentType.contains("text") ||
                    contentType.contains("json") ||
                    contentType.contains("xml") ||
                    contentType.contains("form")
                ) {
                    Body.Text(String(bytes, StandardCharsets.UTF_8))
                } else {
                    Body.Binary(bytes)
                }
            } else {
                Body.Empty
            }

        return Request(
            method = HttpMethod.fromString(nettyRequest.method().name()),
            path = decoder.path(),
            headers = headers,
            queryParams = QueryParams.of(queryParams),
            pathParams = PathParams.empty(), // Will be populated by router
            body = body,
            context = Context(),
        )
    }
}
