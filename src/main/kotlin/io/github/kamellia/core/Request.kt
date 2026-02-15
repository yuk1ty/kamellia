package io.github.kamellia.core

typealias Headers = Map<String, String>

data class Request(
    val method: HttpMethod,
    val path: String,
    val headers: Headers,
    val queryParams: QueryParams,
    val pathParams: PathParams,
    val body: Body,
    val context: Context,
)

suspend fun Request.text(): String {
    return when (val body = this.body) {
        is Body.Text -> body.content
        is Body.Binary -> body.bytes.decodeToString()
        Body.Empty -> ""
    }
}
