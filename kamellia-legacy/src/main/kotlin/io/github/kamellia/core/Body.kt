package io.github.kamellia.core

sealed interface Body {
    data object Empty : Body

    @JvmInline
    value class Text(val content: String) : Body

    @JvmInline
    value class Binary(val bytes: ByteArray) : Body
}
