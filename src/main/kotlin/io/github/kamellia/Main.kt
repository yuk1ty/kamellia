package io.github.kamellia

import io.github.kamellia.core.Response
import io.github.kamellia.core.text
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.Serializable

private val logger = KotlinLogging.logger {}

private const val DEFAULT_PORT = 3000

@Serializable
data class User(val id: String, val name: String)

@Serializable
data class SearchResult(val query: String, val results: List<String>)

@Serializable
data class PostResponse(val userId: String?, val postId: String?, val title: String)

@Serializable
data class CreateUserResponse(val status: String, val data: String)

suspend fun main() {
    val app = Kamellia()

    // Simple GET endpoint
    app.get("/") { request ->
        Response.ok("Hello, Kamellia!")
    }

    // Path parameters
    app.get("/users/:id") { request ->
        val userId = request.pathParams.string("id")
        Response.json(User(id = userId ?: "unknown", name = "John Doe"))
    }

    // Multiple path parameters
    app.get("/users/:userId/posts/:postId") { request ->
        Response.json(
            PostResponse(
                userId = request.pathParams.string("userId"),
                postId = request.pathParams.string("postId"),
                title = "Sample Post",
            ),
        )
    }

    // POST endpoint
    app.post("/users") { request ->
        val body = request.text()
        logger.debug { "Received: $body" }
        Response.json(
            CreateUserResponse(
                status = "created",
                data = body,
            ),
        )
    }

    // Query parameters
    app.get("/search") { request ->
        val query = request.queryParams.string("q") ?: ""
        val result = SearchResult(query = query, results = emptyList())
        Response.json(result)
    }

    logger.info { "Starting Kamellia server on port $DEFAULT_PORT..." }
    app.start(DEFAULT_PORT)
}
