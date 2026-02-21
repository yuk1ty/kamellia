# Kamellia

## Tips

- You DON'T always need to think the backward compatibility of the implementation.
- Just use class names without package names if there is no ambiguity (means, don't use fully qualified name if there is no name conflict):

```kotlin
// DON'T
fun a(): io.github.kamellia.core.PathParams = TODO()

// DO
// Write it in imports
// import io.github.kamellia.core.PathParams
fun a(): PathParams = TODO()
```

- Always choose between value class, data class, class, and object based on their semantic intent. DON'T use them interchangeably.
- You MUST NOT catch `Throwable` exceptions. DON'T use `runCatching` in the standard Kotlin library.
- DO NOT modify detekt rules to bypass or suppress detekt errors. Fix the code to comply with the rules instead.
- When detekt violations are detected, treat them as high priority and fix them immediately. Never ignore or work around them.
