plugins {
    kotlin("plugin.serialization")
}

dependencies {
    api(project(":core"))
    api(project(":kotlinx-serialization"))
    api("com.michael-bull.kotlin-result:kotlin-result:2.0.0")
}
