# Kamellia

A modern, type-safe HTTP server library for Kotlin, built on top of Netty.

## Overview

Kamellia is inspired by [Hono](https://hono.dev/) (TypeScript) and [Axum](https://github.com/tokio-rs/axum) (Rust), focusing on a simple, functional approach: `Handler = Request -> Response`.

## Features (Planned)

- 🚀 Built on Netty for high performance
- 🔒 Type-safe with kotlinx.serialization
- 🎯 Simple, functional handler design
- 🔧 Composable middleware system
- ⚡ Coroutines support for async operations

## Development

### Prerequisites

- JDK 21 or higher
- Gradle 8.x

### Building

```bash
./gradlew build
```

### Testing

```bash
./gradlew test
```

### Code Quality

#### Formatting with Spotless

Check formatting:
```bash
./gradlew spotlessCheck
```

Apply formatting:
```bash
./gradlew spotlessApply
```

#### Linting with Detekt

Run linter:
```bash
./gradlew detekt
```

Generate baseline (to ignore existing issues):
```bash
./gradlew detektBaseline
```

### CI/CD

GitHub Actions runs the following checks on every push and pull request:

- Build
- Tests
- Code formatting (Spotless)
- Linting (Detekt)

## License

TBD

## Contributing

TBD
