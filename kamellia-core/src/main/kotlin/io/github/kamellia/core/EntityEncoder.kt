package io.github.kamellia.core

interface EntityEncoder<in T> {
    val contentType: ContentType

    fun encode(value: T): Body

    companion object
}

fun <T> EntityEncoder(contentType: ContentType, encode: (T) -> Body): EntityEncoder<T> = object : EntityEncoder<T> {
    override val contentType: ContentType = contentType
    override fun encode(value: T): Body = encode.invoke(value)
}
