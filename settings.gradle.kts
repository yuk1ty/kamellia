plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "kamellia"

include(
    "core",
    "arrow-kt",
    "kotlin-result",
    "kotlinx-serialization",
    "examples",
    "benchmarks",
)

project(":arrow-kt").projectDir = file("libs/arrow-kt")
project(":kotlin-result").projectDir = file("libs/kotlin-result")
project(":kotlinx-serialization").projectDir = file("libs/kotlinx-serialization")
