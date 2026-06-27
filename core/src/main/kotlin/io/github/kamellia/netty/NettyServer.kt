package io.github.kamellia.netty

import io.github.kamellia.core.ErrorHandler
import io.github.kamellia.middleware.Middleware
import io.github.kamellia.routing.Router
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpServerCodec
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler

private val logger = KotlinLogging.logger {}

private const val SO_BACKLOG_SIZE = 128
private const val MAX_CONTENT_LENGTH = 1024 * 1024

class NettyServer(
    private val router: Router,
    private val port: Int,
    private val middlewares: List<Middleware> = emptyList(),
    private val errorHandler: ErrorHandler = KamelliaHandler.defaultErrorHandler,
) {
    private val bossGroup = NioEventLoopGroup(1)
    private val workerGroup = NioEventLoopGroup()

    fun start() {
        logger.info { "Initializing Netty server on port $port" }

        try {
            val bootstrap = ServerBootstrap()
            bootstrap
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel::class.java)
                .option(ChannelOption.SO_BACKLOG, SO_BACKLOG_SIZE)
                .handler(LoggingHandler(LogLevel.INFO))
                .childHandler(
                    object : ChannelInitializer<SocketChannel>() {
                        override fun initChannel(ch: SocketChannel) {
                            val pipeline = ch.pipeline()
                            pipeline.addLast(HttpServerCodec())
                            pipeline.addLast(HttpObjectAggregator(MAX_CONTENT_LENGTH))
                            pipeline.addLast(KamelliaHandler(router, middlewares, errorHandler))
                        }
                    },
                ).childOption(ChannelOption.SO_KEEPALIVE, true)

            val channelFuture = bootstrap.bind(port).sync()
            logger.info { "Server started successfully on port $port" }

            channelFuture.channel().closeFuture().sync()
        } catch (e: Exception) {
            logger.error(e) { "Failed to start server on port $port" }
            throw e
        } finally {
            shutdown()
        }
    }

    fun shutdown() {
        logger.info { "Shutting down Netty server" }
        workerGroup.shutdownGracefully()
        bossGroup.shutdownGracefully()
        logger.info { "Server shutdown complete" }
    }
}
