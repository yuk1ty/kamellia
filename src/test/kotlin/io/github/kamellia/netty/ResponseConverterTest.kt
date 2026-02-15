package io.github.kamellia.netty

import io.github.kamellia.core.Body
import io.github.kamellia.core.HttpStatus
import io.github.kamellia.core.Response
import io.netty.handler.codec.http.HttpHeaderNames
import java.nio.charset.StandardCharsets
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ResponseConverterTest {
    @Test
    fun testConvertTextResponse() {
        val response =
            Response(
                status = HttpStatus.OK,
                headers = mapOf("Content-Type" to "text/plain"),
                body = Body.Text("Hello, World!"),
            )

        val nettyResponse = ResponseConverter.convert(response)

        assertEquals(200, nettyResponse.status().code())
        assertEquals("text/plain", nettyResponse.headers().get("Content-Type"))
        val content = nettyResponse.content().toString(StandardCharsets.UTF_8)
        assertEquals("Hello, World!", content)
    }

    @Test
    fun testConvertEmptyResponse() {
        val response =
            Response(
                status = HttpStatus.NO_CONTENT,
                headers = emptyMap(),
                body = Body.Empty,
            )

        val nettyResponse = ResponseConverter.convert(response)

        assertEquals(204, nettyResponse.status().code())
        assertEquals(0, nettyResponse.content().readableBytes())
    }

    @Test
    fun testConvertBinaryResponse() {
        val bytes = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        val response =
            Response(
                status = HttpStatus.OK,
                headers = mapOf("Content-Type" to "application/octet-stream"),
                body = Body.Binary(bytes),
            )

        val nettyResponse = ResponseConverter.convert(response)

        assertEquals(200, nettyResponse.status().code())
        assertEquals(4, nettyResponse.content().readableBytes())
    }

    @Test
    fun testContentLengthIsSet() {
        val response =
            Response(
                status = HttpStatus.OK,
                headers = emptyMap(),
                body = Body.Text("Test"),
            )

        val nettyResponse = ResponseConverter.convert(response)

        assertTrue(nettyResponse.headers().contains(HttpHeaderNames.CONTENT_LENGTH))
        val contentLength = nettyResponse.headers().getInt(HttpHeaderNames.CONTENT_LENGTH)
        assertEquals(4, contentLength)
    }

    @Test
    fun testMultipleHeaders() {
        val response =
            Response(
                status = HttpStatus.OK,
                headers =
                mapOf(
                    "Content-Type" to "application/json",
                    "X-Custom-Header" to "custom-value",
                ),
                body = Body.Text("{}"),
            )

        val nettyResponse = ResponseConverter.convert(response)

        assertEquals("application/json", nettyResponse.headers().get("Content-Type"))
        assertEquals("custom-value", nettyResponse.headers().get("X-Custom-Header"))
    }

    @Test
    fun testDifferentStatusCodes() {
        val testCases =
            listOf(
                HttpStatus.OK to 200,
                HttpStatus.CREATED to 201,
                HttpStatus.BAD_REQUEST to 400,
                HttpStatus.NOT_FOUND to 404,
                HttpStatus.INTERNAL_SERVER_ERROR to 500,
            )

        testCases.forEach { (status, expectedCode) ->
            val response =
                Response(
                    status = status,
                    headers = emptyMap(),
                    body = Body.Empty,
                )

            val nettyResponse = ResponseConverter.convert(response)
            assertEquals(expectedCode, nettyResponse.status().code())
        }
    }
}
