package io.github.kamellia.core

@JvmInline
value class Context(private val state: MutableMap<String, Any> = mutableMapOf()) {
    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: String): T? = state[key] as? T

    fun <T> set(key: String, value: T) {
        state[key] = value as Any
    }

    fun remove(key: String) {
        state.remove(key)
    }

    fun clear() {
        state.clear()
    }
}
