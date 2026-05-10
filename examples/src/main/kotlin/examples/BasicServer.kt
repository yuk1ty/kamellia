package examples

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import io.github.kamellia.Kamellia
import io.github.kamellia.core.HttpStatus
import io.github.kamellia.core.IntoResponses
import io.github.kamellia.core.Request
import io.github.kamellia.dsl.get
import io.github.kamellia.json.jsonIntoResponse
import io.github.kamellia.result.resultIntoResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.Serializable

private val logger = KotlinLogging.logger {}
private const val DEFAULT_PORT = 3000

@Serializable
data class User(val id: String, val name: String)

@Serializable
data class AppError(val code: String, val message: String)

private fun loadUser(id: String?): Result<User, AppError> = when (id) {
    null, "" -> Err(AppError("E_INVALID", "id is required"))
    else -> Ok(User(id = id, name = "John Doe"))
}

fun main() {
    val app = Kamellia()

    with(IntoResponses.plainText) {
        app.get("/") { _: Request -> "Hello, Kamellia!" }
    }

    with(jsonIntoResponse<User>()) {
        app.get("/users/:id") { req: Request ->
            User(id = req.pathParams.string("id") ?: "unknown", name = "John Doe")
        }
    }

    with(
        resultIntoResponse(
            ok = jsonIntoResponse<User>(),
            err = jsonIntoResponse<AppError>(status = HttpStatus.BAD_REQUEST),
        ),
    ) {
        app.get("/users/:id/safe") { req: Request -> loadUser(req.pathParams.string("id")) }
    }

    logger.info { "Starting Kamellia server on port $DEFAULT_PORT..." }
    app.start(DEFAULT_PORT)
}
