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

### MiddlewareExample
**File**: `MiddlewareExample.kt`
**Run**: `./gradlew runExample -PexampleMain=MiddlewareExampleKt`

Demonstrates a complete middleware pipeline with:
- **Error handling** - Catches and handles exceptions gracefully
- **Request ID tracking** - Assigns unique IDs to each request
- **Logging** - Comprehensive request/response logging with MDC
- **CORS** - Cross-origin resource sharing configuration
- **Response time** - Performance metrics in headers
- **Authentication** - Simple Bearer token authentication
- **Custom headers** - Adding custom response headers

The server starts on port 3001 by default.

**Middleware execution order**:
```
Request → Error Handler → Request ID → Logging → CORS → Timing → Auth → Custom Headers → Route → Response
```

#### Testing the MiddlewareExample

Once the server is running, you can test various middleware features:

```bash
# Basic request - see all middleware headers
curl -v http://localhost:3001/

# Public endpoint - no authentication needed
curl http://localhost:3001/api/public

# Protected endpoint - requires authentication (will fail)
curl http://localhost:3001/api/protected

# Protected endpoint - with authentication (will succeed)
curl -H "Authorization: Bearer mytoken123" http://localhost:3001/api/protected

# Test error handling - validation error
curl http://localhost:3001/api/error?type=validation

# Test error handling - server error
curl http://localhost:3001/api/error?type=server

# POST request - see request body logging
curl -X POST http://localhost:3001/api/users \
  -H "Content-Type: application/json" \
  -d '{"username":"john","email":"john@example.com"}'

# CORS preflight request
curl -X OPTIONS http://localhost:3001/api/public \
  -H "Origin: http://localhost:3000" \
  -H "Access-Control-Request-Method: GET"

# Custom request ID
curl -H "X-Request-ID: my-custom-id-123" http://localhost:3001/

# Admin endpoint with auth
curl -H "Authorization: Bearer admin-token" http://localhost:3001/api/admin/settings
```

**Expected Response Headers**:
- `X-Request-ID` - Unique request identifier
- `X-Response-Time` - Request processing time in milliseconds
- `X-Powered-By` - Server identification
- `Access-Control-*` - CORS headers
```
