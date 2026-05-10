plugins {
    kotlin("plugin.serialization")
}

dependencies {
    api(project(":kamellia-core"))
    api(project(":kamellia-kotlinx-serialization"))
    api("io.arrow-kt:arrow-core:1.2.4")
}
