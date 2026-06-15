package examples.arrow

import arrow.core.Either
import io.github.kamellia.Kamellia
import io.github.kamellia.arrow.EitherIntoResponse
import io.github.kamellia.core.Body
import io.github.kamellia.core.HttpStatus
import io.github.kamellia.core.IntoResponse
import io.github.kamellia.core.Request
import io.github.kamellia.core.Response
import io.github.kamellia.dsl.post
import io.github.kamellia.serialization.json
import io.github.kamellia.serialization.jsonBytes
import kotlinx.serialization.Serializable

private const val PORT = 3004

@Serializable
data class CreateUserRequest(val name: String)

@Serializable
data class User(val id: String, val name: String)

@Serializable
data class AppError(val code: String, val message: String) : IntoResponse {
    override fun intoResponse(): Response = Response(
        status = HttpStatus.BAD_REQUEST,
        headers = mapOf("Content-Type" to "application/json; charset=utf-8"),
        body = Body.Strict(jsonBytes(this)),
    )
}

private suspend fun registerUser(req: Request): Either<AppError, User> {
    val input = req.json<CreateUserRequest>()
    return if (input.name.isBlank()) {
        Either.Left(AppError("E_INVALID_NAME", "Name cannot be blank"))
    } else {
        Either.Right(User(id = "uuid-1", name = input.name))
    }
}

fun main() {
    val app = Kamellia()

    app.post("/users") { req ->
        val result = registerUser(req).map { json(it) }
        EitherIntoResponse(result)
    }

    app.start(PORT)
}
