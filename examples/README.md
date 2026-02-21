# Kamellia Examples

This directory contains example applications demonstrating Kamellia's features.

## Running Examples

Run an example using:

```bash
./gradlew runExample -PexampleMain=BasicServerKt
```

Alternatively, you can use the `run` task:

```bash
./gradlew run
```

## Available Examples

### BasicServer
**File**: `BasicServer.kt`
**Run**: `./gradlew runExample -PexampleMain=BasicServerKt`

Demonstrates:
- Simple GET endpoint
- Path parameters (single and multiple)
- POST endpoint with request body
- Query parameters
- JSON serialization with kotlinx.serialization

The server starts on port 3000 by default.

#### Testing the BasicServer

Once the server is running, you can test the endpoints:

```bash
# Simple GET
curl http://localhost:3000/

# Path parameter
curl http://localhost:3000/users/123

# Multiple path parameters
curl http://localhost:3000/users/456/posts/789

# POST with body
curl -X POST http://localhost:3000/users -d "John Doe"

# Query parameters
curl http://localhost:3000/search?q=kotlin
```
