plugins {
    kotlin("plugin.serialization") version "2.2.21"
    kotlin("plugin.allopen") version "2.2.21"
    id("org.jetbrains.kotlinx.benchmark") version "0.4.16"
    application
}

dependencies {
    // Netty
    implementation("io.netty:netty-all:4.1.108.Final")

    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.8.0")

    // kotlinx.serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // Logging
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.3")
    implementation("ch.qos.logback:logback-classic:1.5.18")

    // Benchmarking
    implementation("org.jetbrains.kotlinx:kotlinx-benchmark-runtime:0.4.16")
}

application {
    mainClass.set("examples.BasicServerKt")
}

allOpen {
    annotation("org.openjdk.jmh.annotations.State")
}

sourceSets {
    create("examples") {
        java {
            srcDir("examples")
        }
        compileClasspath += sourceSets.main.get().output
        runtimeClasspath += sourceSets.main.get().output
    }

    create("benchmark") {
        java {
            srcDir("src/benchmark/kotlin")
        }
        compileClasspath += sourceSets.main.get().output
        runtimeClasspath += sourceSets.main.get().output
    }
}

configurations {
    named("examplesImplementation") {
        extendsFrom(configurations.implementation.get())
    }
    named("benchmarkImplementation") {
        extendsFrom(configurations.implementation.get())
    }
}

tasks.register<JavaExec>("runExample") {
    group = "examples"
    description = "Run an example (use -PexampleMain=BasicServerKt)"
    classpath = sourceSets["examples"].runtimeClasspath
    val exampleMain = project.findProperty("exampleMain") as String? ?: ""
    if (exampleMain.isNotEmpty()) {
        mainClass.set("examples.$exampleMain")
    }
}

benchmark {
    targets {
        register("benchmark")
    }

    configurations {
        named("main") {
            warmups = 10
            iterations = 10
            iterationTime = 1
            iterationTimeUnit = "s"
            outputTimeUnit = "ms"
            mode = "thrpt"
        }

        register("smoke") {
            warmups = 3
            iterations = 5
            iterationTime = 500
            iterationTimeUnit = "ms"
            outputTimeUnit = "ms"
            mode = "thrpt"
        }
    }
}
