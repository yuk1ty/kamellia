package examples

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import io.github.kamellia.Kamellia
import io.github.kamellia.core.Body
import io.github.kamellia.core.ErrorHandler
import io.github.kamellia.core.HttpStatus
import io.github.kamellia.core.Response
import io.github.kamellia.core.Text
import io.github.kamellia.dsl.get
import io.github.kamellia.error.HttpException
import io.github.kamellia.result.asJsonIntoResponse
import io.github.kamellia.serialization.json
import io.github.kamellia.serialization.jsonBytes
import kotlinx.serialization.Serializable

@Serializable
data class User(val id: String, val name: String)

@Serializable
data class AppError(val code: String, val message: String)

private fun loadUser(id: String?): Result<User, AppError> = when {
    id.isNullOrEmpty() -> Err(AppError("E_INVALID", "id is required"))
    else -> Ok(User(id = id, name = "John Doe"))
}

private const val PORT = 3000

fun main() {
    val app = Kamellia()

    app.get("/") { _ -> Text("Hello, Kamellia!") }

    app.get("/users/:id") { req ->
        json(User(id = req.pathParams.string("id") ?: "unknown", name = "John Doe"))
    }

    app.get("/users/:id/safe") { req ->
        loadUser(req.pathParams.string("id")).asJsonIntoResponse()
    }

    app.get("/created") { _ ->
        Response(
            status = HttpStatus.CREATED,
            headers = mapOf("Content-Type" to "application/json; charset=utf-8"),
            body = Body.Strict(jsonBytes(User("1", "Alice"))),
        )
    }

    val errorHandler: ErrorHandler = { error, _ ->
        when (error) {
            is HttpException -> Response(
                status = error.status,
                headers = mapOf("Content-Type" to "application/json; charset=utf-8"),
                body = Body.Strict(jsonBytes(AppError("E_HTTP", error.message ?: ""))),
            )

            else -> Response(
                status = HttpStatus.INTERNAL_SERVER_ERROR,
                headers = mapOf("Content-Type" to "application/json; charset=utf-8"),
                body = Body.Strict(jsonBytes(AppError("E_INTERNAL", error.message ?: "unknown"))),
            )
        }
    }

    app.start(PORT, errorHandler)
}
