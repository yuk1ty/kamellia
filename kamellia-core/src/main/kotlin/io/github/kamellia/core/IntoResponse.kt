package io.github.kamellia.core

fun interface IntoResponse<in T> {
    fun intoResponse(value: T): Response
}
