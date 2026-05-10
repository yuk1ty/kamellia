package io.github.kamellia.netty

import io.github.kamellia.core.Body
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
    private val errorHandler: (Throwable, Request?) -> Response = defaultErrorHandler,
) : SimpleChannelInboundHandler<FullHttpRequest>(false) {
    companion object {
        private val defaultErrorHandler: (Throwable, Request?) -> Response = { error, _ ->
            when (error) {
                is HttpException -> Response(
                    status = error.status,
                    headers = mapOf("Content-Type" to "text/plain; charset=utf-8"),
                    body = Body.Strict((error.message ?: "HTTP Error").toByteArray()),
                )

                else -> Response(
                    status = HttpStatus.INTERNAL_SERVER_ERROR,
                    headers = mapOf("Content-Type" to "text/plain; charset=utf-8"),
                    body = Body.Strict((error.message ?: "Internal Server Error").toByteArray()),
                )
            }
        }
    }

    override fun channelRead0(ctx: ChannelHandlerContext, msg: FullHttpRequest) {
        ReferenceCountUtil.retain(msg)

        val dispatcher = ctx.executor().asCoroutineDispatcher()

        CoroutineScope(dispatcher).launch {
            var request: Request? = null
            try {
                request = RequestConverter.convert(msg)

                val routeMatch = router.match(request)

                val handler = if (routeMatch != null) {
                    composeMiddlewares(middlewares, routeMatch.handler)
                } else {
                    composeMiddlewares(middlewares) { _: Request ->
                        Response(
                            status = HttpStatus.NOT_FOUND,
                            headers = mapOf("Content-Type" to "text/plain; charset=utf-8"),
                            body = Body.Strict("Route not found: ${request.method} ${request.path}".toByteArray()),
                        )
                    }
                }

                val requestWithParams = if (routeMatch != null) {
                    request.copy(pathParams = routeMatch.pathParams)
                } else {
                    request
                }

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
        when (response.body) {
            is Body.Streamed -> {
                ctx.executor().execute {
                    CoroutineScope(ctx.executor().asCoroutineDispatcher()).launch {
                        ResponseConverter.writeStreamed(ctx, response)
                        ctx.close()
                    }
                }
            }

            else -> {
                val nettyResponse = ResponseConverter.convertFull(response)
                nettyResponse.headers().set(
                    HttpHeaderNames.CONNECTION,
                    HttpHeaderValues.CLOSE,
                )
                ctx.writeAndFlush(nettyResponse).addListener {
                    ctx.close()
                }
            }
        }
    }

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
