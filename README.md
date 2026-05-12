# Kamellia

A modern, type-safe HTTP server library for Kotlin, built on top of Netty.

## Overview

Kamellia is inspired by [Hono](https://hono.dev/) (TypeScript) and [Axum](https://github.com/tokio-rs/axum) (Rust), focusing on a simple, functional approach: `Handler = Request -> Response`.

Examples — prepare handlers and start a server:

```kotlin
import io.github.kamellia.Kamellia
import io.github.kamellia.core.Request
import io.github.kamellia.core.Text
import io.github.kamellia.dsl.get

// Simple handler that returns a greeting
suspend fun helloHandler(req: Request): Text {
  return Text("Hello, World!")
}

fun main() {
    // Build application and register routes
    val app = Kamellia()
    app.get("/", ::helloHandler)

    // Start Netty server on port 8080
    app.start(9090)
}
```

Example — JSON request -> JSON response:

```kotlin
import io.github.kamellia.Kamellia
import io.github.kamellia.core.IntoResponse
import io.github.kamellia.core.Request
import io.github.kamellia.core.Response
import io.github.kamellia.dsl.post
import kotlinx.serialization.Serializable
import io.github.kamellia.serialization.json

@Serializable
data class HelloRequest(val name: String)

@Serializable
data class HelloResponse(val message: String) : IntoResponse {
    override fun intoResponse(): Response = json(this).intoResponse()

}

suspend fun jsonHandler(req: Request): HelloResponse {
  val input = req.json<HelloRequest>()
  return HelloResponse(message = "Hello, ${input.name}")
}

fun main() {
    val app = Kamellia()
    app.post("/hello", ::jsonHandler)
    app.start(9090)
}
```

This demonstrates reading a JSON body into a @Serializable data class, producing a typed response, encoding it to JSON, and returning it with the appropriate Content-Type header. Adjust the small body-reading/response helpers to the actual Kamellia API in code.

## Features (Planned)

- 🚀 Built on Netty for high performance
- 🔒 Type-safe with kotlinx.serialization
- 🎯 Simple, functional handler design
- 🔧 Composable middleware system
- ⚡ Coroutines support for async operations

## Development

Development and contribution guidelines have moved to CONTRIBUTING.md. Please see CONTRIBUTING.md for setup, build, test, and code quality instructions.


## License

TBD

## Contributing

TBD
