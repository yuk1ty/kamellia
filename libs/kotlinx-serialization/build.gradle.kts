plugins {
    kotlin("plugin.serialization")
}

dependencies {
    api(project(":core"))
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
}
