plugins {
    kotlin("plugin.serialization")
}

dependencies {
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("io.netty:netty-all:4.1.108.Final")
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.3")

    testImplementation(project(":kotlinx-serialization"))
    testImplementation(project(":kotlin-result"))
    testImplementation("ch.qos.logback:logback-classic:1.5.18")
}
