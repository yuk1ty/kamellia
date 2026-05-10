plugins {
    kotlin("plugin.serialization")
}

dependencies {
    api(project(":kamellia-core"))
    api(project(":kamellia-router"))
    api(project(":kamellia-middleware"))
    api(project(":kamellia-netty"))
    testImplementation(project(":kamellia-kotlinx-serialization"))
    testImplementation(project(":kamellia-kotlin-result"))
}
