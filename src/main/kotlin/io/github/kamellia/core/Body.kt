package io.github.kamellia.core

sealed interface Body {
    data object Empty : Body

    data class Text(val content: String) : Body

    data class Binary(val bytes: ByteArray) : Body {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Binary) return false
            return bytes.contentEquals(other.bytes)
        }

        override fun hashCode(): Int {
            return bytes.contentHashCode()
        }
    }
}
