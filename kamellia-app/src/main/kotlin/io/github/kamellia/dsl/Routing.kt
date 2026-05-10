package io.github.kamellia.dsl

import io.github.kamellia.Kamellia
import io.github.kamellia.core.HttpMethod
import io.github.kamellia.core.IntoResponse
import io.github.kamellia.core.Request
import io.github.kamellia.routing.RawHandler

inline fun <T : IntoResponse> Kamellia.get(path: String, crossinline handler: suspend (Request) -> T): Kamellia {
    val raw: RawHandler = { req -> handler(req).intoResponse() }
    router.addRoute(HttpMethod.GET, path, raw)
    return this
}

inline fun <T : IntoResponse> Kamellia.post(path: String, crossinline handler: suspend (Request) -> T): Kamellia {
    val raw: RawHandler = { req -> handler(req).intoResponse() }
    router.addRoute(HttpMethod.POST, path, raw)
    return this
}

inline fun <T : IntoResponse> Kamellia.put(path: String, crossinline handler: suspend (Request) -> T): Kamellia {
    val raw: RawHandler = { req -> handler(req).intoResponse() }
    router.addRoute(HttpMethod.PUT, path, raw)
    return this
}

inline fun <T : IntoResponse> Kamellia.delete(path: String, crossinline handler: suspend (Request) -> T): Kamellia {
    val raw: RawHandler = { req -> handler(req).intoResponse() }
    router.addRoute(HttpMethod.DELETE, path, raw)
    return this
}

inline fun <T : IntoResponse> Kamellia.patch(path: String, crossinline handler: suspend (Request) -> T): Kamellia {
    val raw: RawHandler = { req -> handler(req).intoResponse() }
    router.addRoute(HttpMethod.PATCH, path, raw)
    return this
}
