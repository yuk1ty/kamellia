# Contributing

Thank you for contributing to Kamellia! This document covers how to set up the development environment, build and test the project, and follow code quality checks used in CI.

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

If you have repository-specific contribution guidelines (branch naming, commit message style, PR process), please add them here.
