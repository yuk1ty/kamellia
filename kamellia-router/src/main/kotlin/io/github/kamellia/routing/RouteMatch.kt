package io.github.kamellia.routing

import io.github.kamellia.core.PathParams

data class RouteMatch(val pathParams: PathParams, val handler: RawHandler)
