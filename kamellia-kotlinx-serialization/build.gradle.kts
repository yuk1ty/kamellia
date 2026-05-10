plugins {
    kotlin("plugin.serialization")
}

dependencies {
    api(project(":kamellia-core"))
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
}
