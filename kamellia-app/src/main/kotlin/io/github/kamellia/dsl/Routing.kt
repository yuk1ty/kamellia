package io.github.kamellia.dsl

import io.github.kamellia.Kamellia
import io.github.kamellia.core.Handler
import io.github.kamellia.core.HttpMethod
import io.github.kamellia.core.IntoResponse
import io.github.kamellia.routing.RawHandler

context(encoder: IntoResponse<T>)
inline fun <T> Kamellia.get(path: String, crossinline handler: Handler<T>): Kamellia {
    val raw: RawHandler = { req -> encoder.intoResponse(handler(req)) }
    router.addRoute(HttpMethod.GET, path, raw)
    return this
}

context(encoder: IntoResponse<T>)
inline fun <T> Kamellia.post(path: String, crossinline handler: Handler<T>): Kamellia {
    val raw: RawHandler = { req -> encoder.intoResponse(handler(req)) }
    router.addRoute(HttpMethod.POST, path, raw)
    return this
}

context(encoder: IntoResponse<T>)
inline fun <T> Kamellia.put(path: String, crossinline handler: Handler<T>): Kamellia {
    val raw: RawHandler = { req -> encoder.intoResponse(handler(req)) }
    router.addRoute(HttpMethod.PUT, path, raw)
    return this
}

context(encoder: IntoResponse<T>)
inline fun <T> Kamellia.delete(path: String, crossinline handler: Handler<T>): Kamellia {
    val raw: RawHandler = { req -> encoder.intoResponse(handler(req)) }
    router.addRoute(HttpMethod.DELETE, path, raw)
    return this
}

context(encoder: IntoResponse<T>)
inline fun <T> Kamellia.patch(path: String, crossinline handler: Handler<T>): Kamellia {
    val raw: RawHandler = { req -> encoder.intoResponse(handler(req)) }
    router.addRoute(HttpMethod.PATCH, path, raw)
    return this
}
