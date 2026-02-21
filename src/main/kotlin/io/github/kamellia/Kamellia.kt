package io.github.kamellia

import io.github.kamellia.core.Handler
import io.github.kamellia.core.HttpMethod
import io.github.kamellia.middleware.Middleware
import io.github.kamellia.netty.NettyServer
import io.github.kamellia.routing.Router

class Kamellia {
    private val router = Router()
    private val middlewares = mutableListOf<Middleware>()

    /**
     * Register a GET route
     */
    fun get(path: String, handler: Handler): Kamellia {
        router.addRoute(HttpMethod.GET, path, handler)
        return this
    }

    /**
     * Register a POST route
     */
    fun post(path: String, handler: Handler): Kamellia {
        router.addRoute(HttpMethod.POST, path, handler)
        return this
    }

    /**
     * Register a PUT route
     */
    fun put(path: String, handler: Handler): Kamellia {
        router.addRoute(HttpMethod.PUT, path, handler)
        return this
    }

    /**
     * Register a DELETE route
     */
    fun delete(path: String, handler: Handler): Kamellia {
        router.addRoute(HttpMethod.DELETE, path, handler)
        return this
    }

    /**
     * Register a PATCH route
     */
    fun patch(path: String, handler: Handler): Kamellia {
        router.addRoute(HttpMethod.PATCH, path, handler)
        return this
    }

    /**
     * Register a middleware
     *
     * Middlewares are executed in the order they are registered.
     * Each middleware can intercept and modify requests and responses.
     *
     * Example:
     * ```kotlin
     * app.use(loggingMiddleware())
     * app.use(corsMiddleware())
     * ```
     */
    fun use(middleware: Middleware): Kamellia {
        middlewares.add(middleware)
        return this
    }

    /**
     * Start the server on the specified port
     */
    suspend fun start(port: Int = 3000) {
        val server = NettyServer(router, port, middlewares)
        server.start()
    }
}
