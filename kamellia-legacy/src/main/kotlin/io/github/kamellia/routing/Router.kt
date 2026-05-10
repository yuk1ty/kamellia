package io.github.kamellia.routing

import io.github.kamellia.core.Handler
import io.github.kamellia.core.HttpMethod
import io.github.kamellia.core.Request

class Router {
    private val routes = mutableListOf<Route>()

    fun addRoute(method: HttpMethod, pattern: String, handler: Handler) {
        routes.add(Route(method, pattern, handler))
    }

    fun match(request: Request): RouteMatch? {
        // Find first matching route
        for (route in routes) {
            if (route.method != request.method) continue

            val pathParams = route.matcher.match(request.path)
            if (pathParams != null) {
                return RouteMatch(pathParams, route.handler)
            }
        }

        return null
    }

    fun getAllRoutes(): List<Route> = routes.toList()
}
