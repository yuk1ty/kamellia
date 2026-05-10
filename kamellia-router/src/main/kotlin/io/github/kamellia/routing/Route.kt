package io.github.kamellia.routing

import io.github.kamellia.core.HttpMethod

data class Route(
    val method: HttpMethod,
    val pattern: String,
    val handler: RawHandler,
    val matcher: PathPatternMatcher = PathPatternMatcher(pattern),
)
