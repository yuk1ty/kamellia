package io.github.kamellia.benchmark

import io.github.kamellia.core.Body
import io.github.kamellia.core.Context
import io.github.kamellia.core.Handler
import io.github.kamellia.core.HttpMethod
import io.github.kamellia.core.HttpStatus
import io.github.kamellia.core.PathParams
import io.github.kamellia.core.QueryParams
import io.github.kamellia.core.Request
import io.github.kamellia.core.Response
import io.github.kamellia.middleware.Middleware
import io.github.kamellia.middleware.composeMiddlewares
import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.Blackhole
import kotlinx.benchmark.Setup
import kotlinx.coroutines.runBlocking
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.State

private const val MIDDLEWARE_COUNT_5 = 5
private const val MIDDLEWARE_COUNT_10 = 10

/**
 * Benchmarks for Middleware composition and execution.
 *
 * Measures overhead of:
 * - Single middleware execution
 * - Middleware chains (1, 3, 5, 10 middlewares)
 */
@State(Scope.Benchmark)
class MiddlewareBenchmark {
    private lateinit var baseHandler: Handler
    private lateinit var handlerWith1Middleware: Handler
    private lateinit var handlerWith3Middlewares: Handler
    private lateinit var handlerWith5Middlewares: Handler
    private lateinit var handlerWith10Middlewares: Handler
    private lateinit var testRequest: Request

    @Setup
    fun setup() {
        baseHandler = { _ ->
            Response(status = HttpStatus.OK, body = Body.Text("ok"))
        }

        val noopMiddleware: Middleware = { next ->
            { req ->
                next(req)
            }
        }

        handlerWith1Middleware = composeMiddlewares(
            listOf(noopMiddleware),
            baseHandler,
        )

        handlerWith3Middlewares = composeMiddlewares(
            listOf(noopMiddleware, noopMiddleware, noopMiddleware),
            baseHandler,
        )

        handlerWith5Middlewares = composeMiddlewares(
            List(MIDDLEWARE_COUNT_5) { noopMiddleware },
            baseHandler,
        )

        handlerWith10Middlewares = composeMiddlewares(
            List(MIDDLEWARE_COUNT_10) { noopMiddleware },
            baseHandler,
        )

        testRequest = Request(
            method = HttpMethod.GET,
            path = "/test",
            headers = emptyMap(),
            queryParams = QueryParams.empty(),
            pathParams = PathParams.empty(),
            body = Body.Empty,
            context = Context(),
        )
    }

    @Benchmark
    fun benchmarkBaseHandler(blackhole: Blackhole) {
        val response = runBlocking { baseHandler(testRequest) }
        blackhole.consume(response)
    }

    @Benchmark
    fun benchmarkHandlerWith1Middleware(blackhole: Blackhole) {
        val response = runBlocking { handlerWith1Middleware(testRequest) }
        blackhole.consume(response)
    }

    @Benchmark
    fun benchmarkHandlerWith3Middlewares(blackhole: Blackhole) {
        val response = runBlocking { handlerWith3Middlewares(testRequest) }
        blackhole.consume(response)
    }

    @Benchmark
    fun benchmarkHandlerWith5Middlewares(blackhole: Blackhole) {
        val response = runBlocking { handlerWith5Middlewares(testRequest) }
        blackhole.consume(response)
    }

    @Benchmark
    fun benchmarkHandlerWith10Middlewares(blackhole: Blackhole) {
        val response = runBlocking { handlerWith10Middlewares(testRequest) }
        blackhole.consume(response)
    }
}
