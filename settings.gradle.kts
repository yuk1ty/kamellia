plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "kamellia"

include(
    "kamellia-core",
    "kamellia-router",
    "kamellia-middleware",
    "kamellia-netty",
    "kamellia-app",
    "kamellia-kotlinx-serialization",
    "kamellia-kotlin-result",
    "kamellia-arrow-kt",
    "examples",
    "benchmarks",
)
