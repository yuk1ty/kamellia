# Kamellia Architecture

## Overview

Kamellia is a modern, type-safe HTTP server library for Kotlin built on top of Netty. It follows a simple, functional design inspired by [Hono](https://hono.dev/) (TypeScript) and [Axum](https://github.com/tokio-rs/axum) (Rust), centered around the concept:

```
Handler = Request -> Response
```

The architecture is intentionally modular, with clear separation of concerns across a single `core` Gradle project that uses Kotlin packages to organize routing, middleware, Netty integration, and the application facade. Optional integration libraries live as separate Gradle projects under the `libs` directory.

## Module Structure

The project is organized into the following modules, ordered from lowest to highest level of abstraction:

```
core                       (foundational types, routing, middleware, Netty integration, and app facade)
libs/kotlinx-serialization (JSON request/response helpers)
libs/kotlin-result         (integration with kotlin-result library)
libs/arrow-kt              (integration with Arrow Kt Either)
examples                   (usage examples)
benchmarks                 (performance benchmarks)
```

### Module Dependencies

```
core
    ▲
    └── libs/kotlinx-serialization
            ▲
            ├── libs/kotlin-result
            └── libs/arrow-kt
```

The `core` project contains the `Kamellia` facade and uses Kotlin packages (`io.github.kamellia.core`, `io.github.kamellia.routing`, `io.github.kamellia.middleware`, `io.github.kamellia.netty`, `io.github.kamellia.dsl`) to provide a single, easy-to-use entry point for building applications.

## Core Concepts

### 1. Request / Response Model

The HTTP model is intentionally simple and immutable:

- **`Request`** — Aggregates all information about an incoming HTTP request:
  - `method: HttpMethod` (GET, POST, PUT, DELETE, PATCH, etc.)
  - `path: String`
  - `headers: Map<String, String>`
  - `queryParams: QueryParams` — type-safe query parameter accessor
  - `pathParams: PathParams` — type-safe path parameter accessor
  - `body: Body` — `Empty`, `Strict(ByteArray)`, or `Streamed(Flow<ByteArray>)`
  - `context: Context` — mutable request-scoped key-value store

- **`Response`** — A simple data class:
  - `status: HttpStatus`
  - `headers: Map<String, String>` (default empty)
  - `body: Body` (default `Body.Empty`)

- **`IntoResponse`** — An interface for types that can be converted into a `Response`. This is the key abstraction that lets handlers return arbitrary types (e.g., `Text`, JSON responses, `Result`, `Either`) instead of raw `Response` objects.

### 2. Handler

A handler is a suspend function that transforms a `Request` into any type implementing `IntoResponse`:

```kotlin
typealias Handler<T> = suspend (Request) -> T
```

Internally, the routing layer uses a lower-level type:

```kotlin
typealias RawHandler = suspend (Request) -> Response
```

This separation allows the DSL to accept typed handlers (`T : IntoResponse`) while the router always works with concrete `Response` values.

### 3. Body Representation

Bodies are modeled as a sealed interface with three variants:

- **`Body.Empty`** — zero-length body
- **`Body.Strict(bytes: ByteArray)`** — fully materialized in-memory body
- **`Body.Streamed(source: Flow<ByteArray>)`** — streaming body for large or real-time payloads

This design allows handlers to work with both simple and streaming content without changing the handler signature.

### 4. Router

The router (`io.github.kamellia.routing`) is a straightforward linear matcher:

- Stores routes as an ordered list of `Route(method, pattern, handler, matcher)`
- `PathPatternMatcher` compiles `:param` segments into named regex capture groups
- Matching returns a `RouteMatch(pathParams, handler)` on success, `null` on failure
- Route matching is **order-dependent**; the first matching route wins

**Path parameter syntax:**
- Static segment: `/users`
- Named parameter: `/users/:id`

The router is intentionally simple. For large route tables, a trie or radix-tree implementation could be introduced later without changing the public API.

### 5. Middleware

Middleware is modeled as a higher-order function:

```kotlin
typealias Middleware = (RawHandler) -> RawHandler
```

This is a classic onion / wrapper pattern. Middleware receives the next handler in the chain and returns a new handler that may:

- Intercept the request (short-circuit)
- Modify the request before passing it on
- Modify the response returned by the next handler
- Perform side effects (logging, metrics, etc.)

**Composition:**

Multiple middlewares are composed right-to-left using `foldRight`:

```kotlin
composeMiddlewares([A, B, C], handler) == A(B(C(handler)))
```

Built-in middlewares include:
- `LoggingMiddleware` — request/response logging with optional body capture and request IDs
- `CorsMiddleware` — configurable CORS preflight and response header injection

### 6. Netty Integration

The Netty integration package (`io.github.kamellia.netty`) bridges Kamellia's core model with Netty's channel pipeline:

**`NettyServer`** — Bootstraps a Netty `ServerBootstrap` with:
- `NioEventLoopGroup(1)` for the boss (acceptor) thread
- `NioEventLoopGroup()` for worker threads
- `HttpServerCodec` + `HttpObjectAggregator` in the pipeline
- `KamelliaHandler` as the application handler

**`KamelliaHandler`** — A `SimpleChannelInboundHandler<FullHttpRequest>` that:
1. Converts the Netty `FullHttpRequest` to Kamellia `Request` via `RequestConverter`
2. Matches the request against the `Router`
3. Composes middlewares around the matched handler (or a 404 fallback)
4. Invokes the handler inside a coroutine scope bound to the Netty event loop
5. Catches exceptions and delegates to the `ErrorHandler`
6. Converts the resulting `Response` back to Netty `FullHttpResponse` via `ResponseConverter`
7. Releases Netty message references properly

**RequestConverter** — Translates Netty HTTP specifics (URI, headers, content) into the Kamellia `Request` model.

**ResponseConverter** — Two conversion paths:
- `convertFull(Response)` — for `Body.Empty` and `Body.Strict`, produces a `DefaultFullHttpResponse`
- `writeStreamed(Channel, Response)` — for `Body.Streamed`, writes headers followed by chunked content

### 7. Serialization Integration

`kotlinx-serialization` provides type-safe JSON request/response helpers built on `kotlinx.serialization`:

- **`json(value)`** — encodes a `@Serializable` value into a `JsonResponse` (an `IntoResponse`)
- **`Request.json<T>()`** — decodes the request body into a `@Serializable` type, with strict Content-Type validation
- `JsonResponse` is a `value class` wrapping a `ByteArray` to avoid re-serialization on conversion

### 8. Functional Error Handling Integrations

#### kotlin-result (`libs/kotlin-result`)

Provides `IntoResponse` wrappers for `Result<V, E>` from the `kotlin-result` library:

- `ResultIntoResponse` — folds `Ok` / `Err` into their respective `IntoResponse` conversions
- `Result.asJsonIntoResponse(errStatus)` — automatically JSON-encodes both success and error values
- `Result.asIntoResponse()` — for when both sides already implement `IntoResponse`
- `Unit` — a no-content `IntoResponse` singleton for `Result<Unit, E>` scenarios

#### Arrow Kt (`libs/arrow-kt`)

Provides `IntoResponse` wrapper for `Either<L, R>`:

- `EitherIntoResponse` — folds `Left` / `Right` into their respective `IntoResponse` conversions

## Application Facade (`core`)

The `Kamellia` class in `core` ties everything together:

```kotlin
class Kamellia {
    internal val router: Router = Router()
    internal val middlewares: MutableList<Middleware> = mutableListOf()

    fun use(middleware: Middleware): Kamellia
    fun start(port: Int, errorHandler: ErrorHandler = defaultErrorHandler)
}
```

The DSL in `dsl/Routing.kt` adds ergonomic extension functions:

```kotlin
inline fun <T : IntoResponse> Kamellia.get(path: String, crossinline handler: suspend (Request) -> T): Kamellia
// and post, put, delete, patch
```

These DSL functions wrap user handlers with `.intoResponse()` so they can return any `IntoResponse` type.

## Error Handling

Errors are handled at two levels:

1. **Handler-level exceptions** — Caught by `KamelliaHandler` inside the coroutine. The configured `ErrorHandler` receives the exception and the request (or `null`), and must return a `Response`.

2. **Default error handler** — Recognizes `HttpException` (our typed exception carrying an `HttpStatus`) and maps it to the appropriate status code. All other exceptions map to `500 Internal Server Error`.

Users can provide a custom `ErrorHandler` when calling `start()` to encode errors as JSON, log to external systems, etc.

## Request Lifecycle

A request flows through the system as follows:

```
Netty Channel
    │
    ▼
HttpServerCodec + HttpObjectAggregator
    │
    ▼
KamelliaHandler.channelRead0()
    │
    ├── RequestConverter.convert(FullHttpRequest) → Request
    │
    ▼
Router.match(Request) → RouteMatch? (pathParams + RawHandler)
    │
    ▼
composeMiddlewares(middlewares, handler) → wrapped RawHandler
    │
    ▼
Invoke handler(Request with pathParams) → Response (or Exception)
    │
    ▼
ErrorHandler(Exception, Request?) → Response  (if exception occurred)
    │
    ▼
ResponseConverter.convertFull(Response) → FullHttpResponse
    │
    ▼
Netty writeAndFlush → Client
```

## Design Principles

1. **Type Safety** — Handlers return typed values via `IntoResponse`. JSON serialization is compile-time verified with `kotlinx.serialization`.

2. **Immutability** — `Request` and `Response` are immutable data classes. The only mutable construct is `Context`, scoped to a single request.

3. **Composability** — Middleware, routing, and response types are designed to compose without ceremony.

4. **Minimal Abstractions** — The core model is tiny. No massive framework classes; just `Request -> Response`.

5. **Coroutine-First** — Handlers are `suspend` functions. Netty event loops are bridged to Kotlin coroutines via `Executor.asCoroutineDispatcher()`.

6. **Modularity** — `core` provides the full HTTP stack as Kotlin packages, while optional integrations live as separate projects under `libs`. Pull in the full stack via `core`, or add `libs` extensions as needed.

## Future Directions

- WebSocket support via Netty's `WebSocketServerProtocolHandler`
- Streaming request body handling (currently aggregated via `HttpObjectAggregator`)
- Route group prefixes and nested routers
- Trie-based routing for large route tables
- Additional middlewares (auth, rate limiting, request validation)
- Content negotiation (beyond JSON)
