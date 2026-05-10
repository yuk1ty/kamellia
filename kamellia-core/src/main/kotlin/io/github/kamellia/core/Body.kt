package io.github.kamellia.core

import kotlinx.coroutines.flow.Flow

sealed interface Body {
    val length: Long?

    data object Empty : Body {
        override val length: Long get() = 0L
    }

    @JvmInline
    value class Strict(val bytes: ByteArray) : Body {
        override val length: Long get() = bytes.size.toLong()
    }

    class Streamed(val source: Flow<ByteArray>, override val length: Long? = null) : Body
}
