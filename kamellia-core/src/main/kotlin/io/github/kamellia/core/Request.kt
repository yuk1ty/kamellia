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

fun Request.text(): String = when (val body = this.body) {
    is Body.Strict -> body.bytes.decodeToString()
    Body.Empty -> ""
    is Body.Streamed -> error("Cannot synchronously decode a Streamed body; collect the flow first")
}
