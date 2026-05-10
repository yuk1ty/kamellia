package io.github.kamellia.netty

import io.github.kamellia.core.Body
import io.github.kamellia.core.ErrorHandler
import io.github.kamellia.core.HttpStatus
import io.github.kamellia.core.Request
import io.github.kamellia.core.Response
import io.github.kamellia.error.HttpException
import io.github.kamellia.middleware.Middleware
import io.github.kamellia.middleware.composeMiddlewares
import io.github.kamellia.routing.Router
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import io.netty.util.ReferenceCountUtil
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.Executor
import kotlin.coroutines.CoroutineContext

class KamelliaHandler(
    private val router: Router,
    private val middlewares: List<Middleware> = emptyList(),
    private val errorHandler: ErrorHandler = defaultErrorHandler,
) : SimpleChannelInboundHandler<FullHttpRequest>(false) {
    companion object {
        val defaultErrorHandler: ErrorHandler = { error, _ ->
            when (error) {
                is HttpException ->
                    Response(
                        status = error.status,
                        headers = mapOf("Content-Type" to "text/plain; charset=utf-8"),
                        body = error.message?.toByteArray()?.let { Body.Strict(it) } ?: Body.Empty,
                    )

                else ->
                    Response(
                        status = HttpStatus.INTERNAL_SERVER_ERROR,
                        headers = mapOf("Content-Type" to "text/plain; charset=utf-8"),
                        body = error.message?.toByteArray()?.let { Body.Strict(it) } ?: Body.Empty,
                    )
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    override fun channelRead0(ctx: ChannelHandlerContext, msg: FullHttpRequest) {
        ReferenceCountUtil.retain(msg)

        val dispatcher = ctx.executor().asCoroutineDispatcher()

        CoroutineScope(dispatcher).launch {
            try {
                val request = RequestConverter.convert(msg)

                val routeMatch = router.match(request)
                val handler = if (routeMatch != null) {
                    composeMiddlewares(middlewares, routeMatch.handler)
                } else {
                    composeMiddlewares(middlewares) { _: Request ->
                        Response(
                            status = HttpStatus.NOT_FOUND,
                            headers = mapOf("Content-Type" to "text/plain; charset=utf-8"),
                            body = Body.Strict(
                                "Route not found: ${request.method} ${request.path}".toByteArray(),
                            ),
                        )
                    }
                }

                val requestWithParams = routeMatch?.let { request.copy(pathParams = it.pathParams) } ?: request

                val response = try {
                    handler(requestWithParams)
                } catch (e: Exception) {
                    errorHandler(e, request)
                }

                sendResponse(ctx, response)
            } finally {
                ReferenceCountUtil.release(msg)
            }
        }
    }

    private fun sendResponse(ctx: ChannelHandlerContext, response: Response) {
        val nettyResponse = ResponseConverter.convertFull(response)
        nettyResponse.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE)
        ctx.writeAndFlush(nettyResponse).addListener {
            ctx.close()
        }
    }

    @Suppress("TooGenericExceptionCaught")
    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        cause.printStackTrace()
        ctx.close()
    }
}

private fun Executor.asCoroutineDispatcher(): CoroutineDispatcher = object : CoroutineDispatcher() {
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        this@asCoroutineDispatcher.execute(block)
    }
}
