plugins {
    kotlin("plugin.serialization") version "2.2.21"
}

dependencies {
    api(project(":kamellia-core"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
}
