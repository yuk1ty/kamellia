package io.github.kamellia.core

typealias Handler<T> = suspend (Request) -> T
