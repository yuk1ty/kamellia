plugins {
    kotlin("plugin.serialization")
    application
}

dependencies {
    implementation(project(":core"))
    implementation(project(":kotlinx-serialization"))
    implementation(project(":kotlin-result"))
    implementation(project(":arrow-kt"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("ch.qos.logback:logback-classic:1.5.18")
}

application {
    mainClass.set("examples.BasicServerKt")
}

tasks.register<JavaExec>("runRoutingExample") {
    group = "application"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("examples.routing.RoutingExampleKt")
}

tasks.register<JavaExec>("runMiddlewareExample") {
    group = "application"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("examples.middleware.MiddlewareExampleKt")
}

tasks.register<JavaExec>("runResultExample") {
    group = "application"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("examples.result.ResultExampleKt")
}

tasks.register<JavaExec>("runArrowExample") {
    group = "application"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("examples.arrow.ArrowExampleKt")
}
