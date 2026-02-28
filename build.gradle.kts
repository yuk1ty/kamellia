plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.serialization") version "2.2.21"
    kotlin("plugin.allopen") version "2.2.21"
    id("org.jetbrains.kotlinx.benchmark") version "0.4.16"
    id("com.diffplug.spotless") version "6.25.0"
    id("io.gitlab.arturbosch.detekt") version "1.23.6"
    application
}

group = "io.github.kamellia"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
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

    // Testing
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")

    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.6")

    // Benchmarking
    implementation("org.jetbrains.kotlinx:kotlinx-benchmark-runtime:0.4.16")
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("examples.BasicServerKt")
}

// allopen configuration for JMH compatibility
allOpen {
    annotation("org.openjdk.jmh.annotations.State")
}

tasks.test {
    useJUnitPlatform()
}

// Examples source set configuration
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

// Task to run examples
tasks.register<JavaExec>("runExample") {
    group = "examples"
    description = "Run an example (use -PexampleMain=BasicServerKt)"
    classpath = sourceSets["examples"].runtimeClasspath
    val exampleMain = project.findProperty("exampleMain") as String? ?: ""
    if (exampleMain.isNotEmpty()) {
        mainClass.set("examples.$exampleMain")
    }
}

// kotlinx-benchmark configuration
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

// Spotless configuration
spotless {
    kotlin {
        target("src/**/*.kt", "examples/**/*.kt", "src/benchmark/**/*.kt")
        ktlint("1.2.1")
            .editorConfigOverride(
                mapOf(
                    "indent_size" to "4",
                    "max_line_length" to "120",
                    "ktlint_standard_no-wildcard-imports" to "disabled",
                    "ktlint_standard_wrapping" to "enabled",
                    "ktlint_function_naming_ignore_when_annotated_with" to "Composable",
                ),
            )
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint("1.2.1")
    }
}

// Detekt configuration
detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom("$projectDir/config/detekt/detekt.yml")
    baseline = file("$projectDir/config/detekt/baseline.xml")
    source.setFrom(
        "src/main/kotlin",
        "src/test/kotlin",
        "examples",
        "src/benchmark/kotlin",
    )
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    reports {
        html.required.set(true)
        xml.required.set(true)
        txt.required.set(true)
        sarif.required.set(true)
        md.required.set(true)
    }
}
