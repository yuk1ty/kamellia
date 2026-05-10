plugins {
    application
    kotlin("plugin.serialization") version "2.2.21"
}

dependencies {
    implementation(project(":kamellia-app"))
    implementation(project(":kamellia-json"))
    implementation(project(":kamellia-result"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.6.3")
    implementation("ch.qos.logback:logback-classic:1.5.18")
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.3")
}

application {
    mainClass.set("examples.BasicServerKt")
}
