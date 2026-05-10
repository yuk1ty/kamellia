package io.github.kamellia.middleware

import io.github.kamellia.routing.RawHandler

typealias Middleware = (RawHandler) -> RawHandler
