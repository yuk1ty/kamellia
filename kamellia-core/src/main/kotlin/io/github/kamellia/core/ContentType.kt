package io.github.kamellia.core

@JvmInline
value class ContentType(val value: String) {
    override fun toString(): String = value

    companion object {
        val TEXT_PLAIN_UTF8 = ContentType("text/plain; charset=utf-8")
        val APPLICATION_JSON = ContentType("application/json; charset=utf-8")
        val APPLICATION_OCTET_STREAM = ContentType("application/octet-stream")
    }
}
