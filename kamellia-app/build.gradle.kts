dependencies {
    api(project(":kamellia-core"))
    api(project(":kamellia-router"))
    api(project(":kamellia-middleware"))
    api(project(":kamellia-netty"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
}
