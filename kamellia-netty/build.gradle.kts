dependencies {
    api(project(":kamellia-core"))
    api(project(":kamellia-router"))
    api(project(":kamellia-middleware"))
    implementation("io.netty:netty-all:4.1.108.Final")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.8.0")
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.3")
    testImplementation("ch.qos.logback:logback-classic:1.5.18")
}
