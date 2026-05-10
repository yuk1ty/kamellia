package io.github.kamellia.netty

import io.github.kamellia.core.Body
import io.github.kamellia.core.Context
import io.github.kamellia.core.HttpMethod
import io.github.kamellia.core.PathParams
import io.github.kamellia.core.QueryParams
import io.github.kamellia.core.Request
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.QueryStringDecoder

object RequestConverter {
    fun convert(nettyRequest: FullHttpRequest): Request {
        val decoder = QueryStringDecoder(nettyRequest.uri())

        val headers = mutableMapOf<String, String>()
        nettyRequest.headers().forEach { entry ->
            headers[entry.key.lowercase()] = entry.value
        }

        val queryParams = decoder.parameters().mapValues { it.value }

        val body = if (nettyRequest.content().readableBytes() > 0) {
            val bytes = ByteArray(nettyRequest.content().readableBytes())
            nettyRequest.content().getBytes(0, bytes)
            Body.Strict(bytes)
        } else {
            Body.Empty
        }

        return Request(
            method = HttpMethod.fromString(nettyRequest.method().name()),
            path = decoder.path(),
            headers = headers,
            queryParams = QueryParams.of(queryParams),
            pathParams = PathParams.empty(),
            body = body,
            context = Context(),
        )
    }
}
