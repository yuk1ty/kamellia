package io.github.kamellia.middleware

import io.github.kamellia.routing.RawHandler

fun composeMiddlewares(middlewares: List<Middleware>, handler: RawHandler): RawHandler =
    middlewares.foldRight(handler) { middleware, next -> middleware(next) }
