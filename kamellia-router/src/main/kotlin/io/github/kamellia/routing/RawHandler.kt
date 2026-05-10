package io.github.kamellia.routing

import io.github.kamellia.core.Request
import io.github.kamellia.core.Response

typealias RawHandler = suspend (Request) -> Response
