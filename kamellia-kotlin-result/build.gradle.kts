plugins {
    kotlin("plugin.serialization")
}

dependencies {
    api(project(":kamellia-core"))
    api(project(":kamellia-kotlinx-serialization"))
    api("com.michael-bull.kotlin-result:kotlin-result:2.0.0")
}
