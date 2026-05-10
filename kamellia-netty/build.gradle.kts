dependencies {
    implementation(project(":kamellia-core"))
    implementation(project(":kamellia-router"))
    implementation(project(":kamellia-middleware"))
    implementation("io.netty:netty-all:4.1.108.Final")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.8.0")
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.3")
}
