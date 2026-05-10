plugins {
    kotlin("plugin.serialization")
    application
}

dependencies {
    implementation(project(":kamellia-app"))
    implementation(project(":kamellia-kotlinx-serialization"))
    implementation(project(":kamellia-kotlin-result"))
    implementation(project(":kamellia-arrow-kt"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("ch.qos.logback:logback-classic:1.5.18")
}

application {
    mainClass.set("examples.BasicServerKt")
}
