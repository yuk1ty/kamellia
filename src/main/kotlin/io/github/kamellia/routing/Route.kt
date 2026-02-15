package io.github.kamellia.routing

import io.github.kamellia.core.Handler
import io.github.kamellia.core.HttpMethod

data class Route(
    val method: HttpMethod,
    val pattern: String,
    val handler: Handler,
    val matcher: PathPatternMatcher = PathPatternMatcher(pattern),
)
