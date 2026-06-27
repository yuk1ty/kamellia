plugins {
    kotlin("plugin.serialization")
}

dependencies {
    api(project(":core"))
    api(project(":kotlinx-serialization"))
    api("io.arrow-kt:arrow-core:1.2.4")
}
