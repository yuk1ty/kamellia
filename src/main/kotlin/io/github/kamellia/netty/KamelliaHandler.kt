package io.github.kamellia.netty

import io.github.kamellia.core.Body
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
                is HttpException ->
                    Response(
                        status = error.status,
                        body = Body.Text(error.message ?: "HTTP Error"),
                    )

                else ->
                    Response.internalServerError(
                        error.message ?: "Internal Server Error",
                    )
            }
        }
    }

    override fun channelRead0(ctx: ChannelHandlerContext, msg: FullHttpRequest) {
        // Retain message for async processing
        ReferenceCountUtil.retain(msg)

        // Convert Netty executor to CoroutineDispatcher
        val dispatcher = ctx.executor().asCoroutineDispatcher()

        // Launch coroutine on Netty event loop
        CoroutineScope(dispatcher).launch {
            var request: Request? = null
            try {
                // Convert Netty request to Kamellia request
                request = RequestConverter.convert(msg)

                // Find matching route
                val routeMatch = router.match(request)

                // Compose middlewares with handler
                val handler =
                    if (routeMatch != null) {
                        // Compose middlewares with route handler
                        composeMiddlewares(middlewares, routeMatch.handler)
                    } else {
                        // Compose middlewares with 404 handler
                        composeMiddlewares(middlewares) { _: Request ->
                            Response.notFound("Route not found: ${request.method} ${request.path}")
                        }
                    }

                // Prepare request with path params
                val requestWithParams =
                    if (routeMatch != null) {
                        request.copy(pathParams = routeMatch.pathParams)
                    } else {
                        request
                    }

                // Execute composed handler
                val response = handler(requestWithParams)

                // Convert and send response
                sendResponse(ctx, response)
            } catch (e: Exception) {
                // Handle errors
                val errorResponse = errorHandler(e, request)
                sendResponse(ctx, errorResponse)
            } finally {
                // Release message
                ReferenceCountUtil.release(msg)
            }
        }
    }

    private fun sendResponse(ctx: ChannelHandlerContext, response: Response) {
        val nettyResponse = ResponseConverter.convert(response)

        // Phase 1: Always close connection (no keep-alive)
        val keepAlive = false

        if (keepAlive) {
            nettyResponse.headers().set(
                HttpHeaderNames.CONNECTION,
                HttpHeaderValues.KEEP_ALIVE,
            )
            ctx.writeAndFlush(nettyResponse)
        } else {
            nettyResponse.headers().set(
                HttpHeaderNames.CONNECTION,
                HttpHeaderValues.CLOSE,
            )
            ctx.writeAndFlush(nettyResponse).addListener {
                ctx.close()
            }
        }
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        cause.printStackTrace()
        ctx.close()
    }
}

// Extension to convert Netty Executor to CoroutineDispatcher
private fun Executor.asCoroutineDispatcher(): CoroutineDispatcher {
    return object : CoroutineDispatcher() {
        override fun dispatch(context: CoroutineContext, block: Runnable) {
            this@asCoroutineDispatcher.execute(block)
        }
    }
}
