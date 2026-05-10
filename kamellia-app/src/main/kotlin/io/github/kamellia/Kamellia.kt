package io.github.kamellia

import io.github.kamellia.middleware.Middleware
import io.github.kamellia.netty.NettyServer
import io.github.kamellia.routing.Router

class Kamellia {
    @PublishedApi
    internal val router: Router = Router()

    @PublishedApi
    internal val middlewares: MutableList<Middleware> = mutableListOf()

    fun use(middleware: Middleware): Kamellia {
        middlewares.add(middleware)
        return this
    }

    fun router(): Router = router

    fun middlewares(): List<Middleware> = middlewares.toList()

    fun start(port: Int = 3000) {
        NettyServer(router, port, middlewares).start()
    }
}
