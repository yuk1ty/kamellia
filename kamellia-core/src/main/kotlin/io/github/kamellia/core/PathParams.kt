package io.github.kamellia.core

@JvmInline
value class PathParams internal constructor(
    @PublishedApi
    internal val params: Map<String, String>,
) {
    fun int(key: String): Int? = params[key]?.toIntOrNull()

    fun long(key: String): Long? = params[key]?.toLongOrNull()

    fun string(key: String): String? = params[key]

    fun boolean(key: String): Boolean? = params[key]?.toBooleanStrictOrNull()

    fun double(key: String): Double? = params[key]?.toDoubleOrNull()

    inline fun <reified T> get(key: String): T? {
        val value = params[key] ?: return null
        return when (T::class) {
            Int::class -> value.toIntOrNull() as? T
            Long::class -> value.toLongOrNull() as? T
            String::class -> value as? T
            Boolean::class -> value.toBooleanStrictOrNull() as? T
            Double::class -> value.toDoubleOrNull() as? T
            else -> null
        }
    }

    companion object {
        fun empty(): PathParams = PathParams(emptyMap())

        fun of(params: Map<String, String>): PathParams = PathParams(params)
    }
}
