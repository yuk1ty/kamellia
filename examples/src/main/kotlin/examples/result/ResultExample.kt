package examples.result

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import io.github.kamellia.Kamellia
import io.github.kamellia.core.Body
import io.github.kamellia.core.HttpStatus
import io.github.kamellia.core.IntoResponse
import io.github.kamellia.core.Request
import io.github.kamellia.core.Response
import io.github.kamellia.dsl.delete
import io.github.kamellia.dsl.get
import io.github.kamellia.dsl.post
import io.github.kamellia.error.HttpException
import io.github.kamellia.result.asIntoResponse
import io.github.kamellia.result.asJsonIntoResponse
import io.github.kamellia.serialization.json
import io.github.kamellia.serialization.jsonBytes
import kotlinx.serialization.Serializable

private const val PORT = 3003

@Serializable
data class User(val id: String, val name: String)

@Serializable
data class UserResponse(val id: String, val name: String) : IntoResponse {
    override fun intoResponse(): Response = json(this).intoResponse()
}

@Serializable
data class AppError(val code: String, val message: String) : IntoResponse {
    override fun intoResponse(): Response = Response(
        status = HttpStatus.BAD_REQUEST,
        headers = mapOf("Content-Type" to "application/json; charset=utf-8"),
        body = Body.Strict(jsonBytes(this)),
    )
}

private fun findUser(req: Request): Result<UserResponse, AppError> {
    val id = req.pathParams.string("id")
    return if (id.isNullOrEmpty()) {
        Err(AppError("E_INVALID_ID", "id is required"))
    } else {
        Ok(UserResponse(id, "Alice"))
    }
}

private suspend fun createUser(req: Request): Result<User, AppError> = try {
    val input = req.json<User>()
    Ok(input.copy(id = "generated-${input.id}"))
} catch (e: IllegalArgumentException) {
    Err(AppError("E_INVALID_INPUT", e.message ?: "invalid input"))
}

fun main() {
    val app = Kamellia()

    app.get("/users/:id") { findUser(it).asIntoResponse() }

    app.post("/users") { createUser(it).asJsonIntoResponse() }

    app.delete("/users/:id") {
        val id = it.pathParams.string("id")
        if (id.isNullOrEmpty()) {
            throw HttpException(HttpStatus.BAD_REQUEST, "id is required")
        }
        io.github.kamellia.result.Unit
    }

    app.start(PORT)
}
