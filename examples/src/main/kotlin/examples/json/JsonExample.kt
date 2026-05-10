package examples.json

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import io.github.kamellia.Kamellia
import io.github.kamellia.core.HttpStatus
import io.github.kamellia.core.IntoResponse
import io.github.kamellia.core.Request
import io.github.kamellia.core.Response
import io.github.kamellia.dsl.get
import io.github.kamellia.dsl.post
import io.github.kamellia.result.Unit
import io.github.kamellia.result.asIntoResponse
import io.github.kamellia.serialization.json
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
@JvmInline
value class UserName(val value: String) {
    init {
        require(value.isNotBlank()) { "Name cannot be blank" }
    }
}

@Serializable
data class User(@Contextual val id: UUID, val name: UserName) : IntoResponse {
    override fun intoResponse(): Response = json(this).intoResponse()
}

sealed interface AppError : IntoResponse {
    data object InvalidName : AppError
    data object UserNotFound : AppError
    data object InternalServerError : AppError

    override fun intoResponse(): Response = when (this) {
        is InvalidName -> Response(HttpStatus.BAD_REQUEST)
        is UserNotFound -> Response(HttpStatus.NOT_FOUND)
        is InternalServerError -> Response(HttpStatus.INTERNAL_SERVER_ERROR)
    }
}

suspend fun saveUser(req: Request): Result<Unit, AppError> {
    try {
        val user = req.json<User>()
        println("Saving user: ${user.name.value} with id: ${user.id}")
        return Ok(Unit)
    } catch (e: IllegalArgumentException) {
        System.err.println("Invalid user payload: ${e.message}")
        return Err(AppError.InvalidName)
    }
}

suspend fun fetchUser(req: Request): Result<User, AppError> {
    val id = UUID.fromString(req.pathParams.string("id"))
    return when {
        id == null -> Err(AppError.UserNotFound)
        else -> Ok(User(id, UserName("Alice")))
    }
}

fun main() {
    val app = Kamellia()

    app.get("/users/:id") { fetchUser(it).asIntoResponse() }
    app.post("/users/:id") { saveUser(it).asIntoResponse() }
}
