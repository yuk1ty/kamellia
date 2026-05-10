package io.github.kamellia.benchmark

import io.github.kamellia.core.Body
import io.github.kamellia.core.Context
import io.github.kamellia.core.HttpMethod
import io.github.kamellia.core.HttpStatus
import io.github.kamellia.core.PathParams
import io.github.kamellia.core.QueryParams
import io.github.kamellia.core.Request
import io.github.kamellia.core.Response
import io.github.kamellia.routing.Router
import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.Blackhole
import kotlinx.benchmark.Setup
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.State

/**
 * Benchmarks for Router performance.
 *
 * Measures routing overhead for different path patterns:
 * - Simple static routes
 * - Parameterized routes with single parameter
 * - Complex routes with multiple parameters
 * - Method discrimination
 */
@State(Scope.Benchmark)
class RouterBenchmark {
    private lateinit var router: Router
    private lateinit var simpleRequest: Request
    private lateinit var paramRequest: Request
    private lateinit var complexParamRequest: Request
    private lateinit var postRequest: Request

    @Setup
    fun setup() {
        router = Router()

        router.addRoute(HttpMethod.GET, "/api/users") { _ ->
            Response(status = HttpStatus.OK, body = Body.Text("users"))
        }

        router.addRoute(HttpMethod.GET, "/api/users/:id") { req ->
            val id = req.pathParams.string("id")
            Response(status = HttpStatus.OK, body = Body.Text("user $id"))
        }

        router.addRoute(HttpMethod.GET, "/api/users/:userId/posts/:postId") { req ->
            val userId = req.pathParams.string("userId")
            val postId = req.pathParams.string("postId")
            Response(status = HttpStatus.OK, body = Body.Text("user $userId post $postId"))
        }

        router.addRoute(HttpMethod.POST, "/api/users") { _ ->
            Response(status = HttpStatus.CREATED, body = Body.Text("created"))
        }

        router.addRoute(HttpMethod.PUT, "/api/users/:id") { req ->
            val id = req.pathParams.string("id")
            Response(status = HttpStatus.OK, body = Body.Text("updated $id"))
        }

        simpleRequest = createRequest(HttpMethod.GET, "/api/users")
        paramRequest = createRequest(HttpMethod.GET, "/api/users/123")
        complexParamRequest = createRequest(HttpMethod.GET, "/api/users/123/posts/456")
        postRequest = createRequest(HttpMethod.POST, "/api/users")
    }

    private fun createRequest(method: HttpMethod, path: String): Request = Request(
        method = method,
        path = path,
        headers = emptyMap(),
        queryParams = QueryParams.empty(),
        pathParams = PathParams.empty(),
        body = Body.Empty,
        context = Context(),
    )

    @Benchmark
    fun benchmarkSimpleRouteMatch(blackhole: Blackhole) {
        val match = router.match(simpleRequest)
        blackhole.consume(match)
    }

    @Benchmark
    fun benchmarkParameterizedRouteMatch(blackhole: Blackhole) {
        val match = router.match(paramRequest)
        blackhole.consume(match)
    }

    @Benchmark
    fun benchmarkComplexParameterizedRouteMatch(blackhole: Blackhole) {
        val match = router.match(complexParamRequest)
        blackhole.consume(match)
    }

    @Benchmark
    fun benchmarkMethodDiscrimination(blackhole: Blackhole) {
        val match = router.match(postRequest)
        blackhole.consume(match)
    }
}
