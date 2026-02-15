package io.github.kamellia.routing

import io.github.kamellia.core.PathParams

class PathPatternMatcher(pattern: String) {
    private val paramNames = mutableListOf<String>()
    private val regex: Regex

    init {
        // Extract parameter names from pattern like "/users/{id}"
        val paramPattern = """\{(\w+)\}""".toRegex()
        paramPattern.findAll(pattern).forEach { match ->
            paramNames.add(match.groupValues[1])
        }

        // Convert "/users/{id}/posts/{postId}" to regex with named groups
        // Result: "^/users/(?<id>[^/]+)/posts/(?<postId>[^/]+)$"
        val regexPattern =
            pattern
                .replace("{", "(?<")
                .replace("}", ">[^/]+)")

        regex = Regex("^$regexPattern\$")
    }

    fun match(path: String): PathParams? {
        val matchResult = regex.matchEntire(path) ?: return null

        val params = mutableMapOf<String, String>()
        paramNames.forEach { name ->
            matchResult.groups[name]?.value?.let { value ->
                params[name] = value
            }
        }

        return PathParams.of(params)
    }

    fun matches(path: String): Boolean {
        return regex.matches(path)
    }
}
