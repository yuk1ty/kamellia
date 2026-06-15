package examples.routing

import io.github.kamellia.Kamellia
import io.github.kamellia.core.HttpStatus
import io.github.kamellia.core.IntoResponse
import io.github.kamellia.core.Request
import io.github.kamellia.core.Response
import io.github.kamellia.dsl.delete
import io.github.kamellia.dsl.get
import io.github.kamellia.dsl.patch
import io.github.kamellia.dsl.post
import io.github.kamellia.dsl.put
import io.github.kamellia.error.HttpException
import io.github.kamellia.serialization.json
import kotlinx.serialization.Serializable

private const val PORT = 3001

@Serializable
data class Task(val id: Int, val title: String, val status: String)

@Serializable
data class CreateTaskRequest(val title: String, val status: String = "todo")

@Serializable
data class UpdateTaskRequest(val title: String? = null, val status: String? = null)

private val tasks = mutableMapOf(
    1 to Task(1, "Learn Kamellia", "todo"),
    2 to Task(2, "Build an API", "done"),
)

private fun Request.requireTaskId(): Int {
    val id = pathParams.int("id")
    return id ?: throw HttpException(HttpStatus.BAD_REQUEST, "id must be an integer")
}

private suspend fun listTasks(req: Request): IntoResponse {
    val status = req.queryParams.string("status")
    val page = req.queryParams.int("page") ?: 1
    val filtered = tasks.values.filter { status == null || it.status == status }
    return json(
        mapOf(
            "page" to page,
            "tasks" to filtered,
        ),
    )
}

private suspend fun getTask(req: Request): IntoResponse {
    val id = req.requireTaskId()
    val task = tasks[id] ?: throw HttpException(HttpStatus.NOT_FOUND, "task not found")
    return json(task)
}

private suspend fun createTask(req: Request): IntoResponse {
    val input = req.json<CreateTaskRequest>()
    val nextId = (tasks.keys.maxOrNull() ?: 0) + 1
    val task = Task(nextId, input.title, input.status)
    tasks[nextId] = task
    return Response(
        status = HttpStatus.CREATED,
        headers = mapOf("Location" to "/tasks/$nextId"),
        body = json(task).intoResponse().body,
    )
}

private suspend fun updateTask(req: Request): IntoResponse {
    val id = req.requireTaskId()
    val input = req.json<UpdateTaskRequest>()
    val existing = tasks[id] ?: throw HttpException(HttpStatus.NOT_FOUND, "task not found")
    val updated = existing.copy(
        title = input.title ?: existing.title,
        status = input.status ?: existing.status,
    )
    tasks[id] = updated
    return json(updated)
}

private suspend fun deleteTask(req: Request): IntoResponse {
    val id = req.requireTaskId()
    tasks.remove(id) ?: throw HttpException(HttpStatus.NOT_FOUND, "task not found")
    return Response(HttpStatus.NO_CONTENT)
}

fun main() {
    val app = Kamellia()

    app.get("/tasks", ::listTasks)
    app.get("/tasks/:id", ::getTask)
    app.post("/tasks", ::createTask)
    app.put("/tasks/:id", ::updateTask)
    app.patch("/tasks/:id", ::updateTask)
    app.delete("/tasks/:id", ::deleteTask)

    app.start(PORT)
}
