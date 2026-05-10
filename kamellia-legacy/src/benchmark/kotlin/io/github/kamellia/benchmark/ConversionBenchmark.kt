package io.github.kamellia.benchmark

import io.github.kamellia.core.Body
import io.github.kamellia.core.HttpStatus
import io.github.kamellia.core.Response
import io.github.kamellia.netty.RequestConverter
import io.github.kamellia.netty.ResponseConverter
import io.netty.buffer.Unpooled
import io.netty.handler.codec.http.DefaultFullHttpRequest
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.HttpVersion
import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.Blackhole
import kotlinx.benchmark.Setup
import kotlinx.benchmark.TearDown
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.State

/**
 * Benchmarks for Netty conversion operations.
 *
 * Measures:
 * - Netty HttpRequest to Kamellia Request conversion
 * - Kamellia Response to Netty HttpResponse conversion
 * - Header parsing overhead
 * - Different body sizes
 */
@State(Scope.Benchmark)
class ConversionBenchmark {
    private lateinit var simpleNettyRequest: FullHttpRequest
    private lateinit var nettyRequestWithHeaders: FullHttpRequest
    private lateinit var nettyRequestWithBody: FullHttpRequest
    private lateinit var simpleResponse: Response
    private lateinit var responseWithHeaders: Response
    private lateinit var responseWithBody: Response

    @Setup
    fun setup() {
        simpleNettyRequest = DefaultFullHttpRequest(
            HttpVersion.HTTP_1_1,
            HttpMethod.GET,
            "/api/users",
        )

        nettyRequestWithHeaders = DefaultFullHttpRequest(
            HttpVersion.HTTP_1_1,
            HttpMethod.GET,
            "/api/users",
        ).apply {
            headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json")
            headers().set(HttpHeaderNames.ACCEPT, "application/json")
            headers().set(HttpHeaderNames.AUTHORIZATION, "Bearer token123")
            headers().set(HttpHeaderNames.USER_AGENT, "Benchmark/1.0")
        }

        val bodyContent = """{"name":"test","email":"test@example.com"}"""
        nettyRequestWithBody = DefaultFullHttpRequest(
            HttpVersion.HTTP_1_1,
            HttpMethod.POST,
            "/api/users",
            Unpooled.copiedBuffer(bodyContent, Charsets.UTF_8),
        ).apply {
            headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json")
            headers().set(HttpHeaderNames.CONTENT_LENGTH, bodyContent.length)
        }

        simpleResponse = Response(
            status = HttpStatus.OK,
            body = Body.Text("ok"),
        )

        responseWithHeaders = Response(
            status = HttpStatus.OK,
            headers = mapOf(
                "Content-Type" to "application/json",
                "Cache-Control" to "no-cache",
                "X-Custom-Header" to "value",
            ),
            body = Body.Text("ok"),
        )

        responseWithBody = Response(
            status = HttpStatus.OK,
            headers = mapOf("Content-Type" to "application/json"),
            body = Body.Text("""{"data":[1,2,3,4,5],"total":5,"status":"success"}"""),
        )
    }

    @TearDown
    fun teardown() {
        simpleNettyRequest.release()
        nettyRequestWithHeaders.release()
        nettyRequestWithBody.release()
    }

    @Benchmark
    fun benchmarkSimpleRequestConversion(blackhole: Blackhole) {
        val request = RequestConverter.convert(simpleNettyRequest)
        blackhole.consume(request)
    }

    @Benchmark
    fun benchmarkRequestConversionWithHeaders(blackhole: Blackhole) {
        val request = RequestConverter.convert(nettyRequestWithHeaders)
        blackhole.consume(request)
    }

    @Benchmark
    fun benchmarkRequestConversionWithBody(blackhole: Blackhole) {
        val request = RequestConverter.convert(nettyRequestWithBody)
        blackhole.consume(request)
    }

    @Benchmark
    fun benchmarkSimpleResponseConversion(blackhole: Blackhole) {
        val nettyResponse = ResponseConverter.convert(simpleResponse)
        blackhole.consume(nettyResponse)
        nettyResponse.release()
    }

    @Benchmark
    fun benchmarkResponseConversionWithHeaders(blackhole: Blackhole) {
        val nettyResponse = ResponseConverter.convert(responseWithHeaders)
        blackhole.consume(nettyResponse)
        nettyResponse.release()
    }

    @Benchmark
    fun benchmarkResponseConversionWithBody(blackhole: Blackhole) {
        val nettyResponse = ResponseConverter.convert(responseWithBody)
        blackhole.consume(nettyResponse)
        nettyResponse.release()
    }
}
