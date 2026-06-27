package io.github.kamellia.core

@JvmInline
value class QueryParams internal constructor(
    @PublishedApi
    internal val params: Map<String, List<String>>,
) {
    fun int(key: String): Int? = params[key]?.firstOrNull()?.toIntOrNull()

    fun long(key: String): Long? = params[key]?.firstOrNull()?.toLongOrNull()

    fun string(key: String): String? = params[key]?.firstOrNull()

    fun boolean(key: String): Boolean? = params[key]?.firstOrNull()?.toBooleanStrictOrNull()

    fun double(key: String): Double? = params[key]?.firstOrNull()?.toDoubleOrNull()

    fun list(key: String): List<String> = params[key] ?: emptyList()

    fun intList(key: String): List<Int> = params[key]?.mapNotNull { it.toIntOrNull() } ?: emptyList()

    fun longList(key: String): List<Long> = params[key]?.mapNotNull { it.toLongOrNull() } ?: emptyList()

    inline fun <reified T> get(key: String): T? {
        val value = params[key]?.firstOrNull() ?: return null
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
        fun empty(): QueryParams = QueryParams(emptyMap())

        fun of(params: Map<String, List<String>>): QueryParams = QueryParams(params)
    }
}
