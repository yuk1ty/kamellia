plugins {
    kotlin("jvm") version "2.2.21" apply false
    kotlin("plugin.serialization") version "2.2.21" apply false
    kotlin("plugin.allopen") version "2.2.21" apply false
    id("org.jetbrains.kotlinx.benchmark") version "0.4.16" apply false
    id("com.diffplug.spotless") version "7.0.2" apply false
    id("io.gitlab.arturbosch.detekt") version "1.23.7" apply false
}

group = "io.github.kamellia"
version = "0.1.0-SNAPSHOT"

allprojects {
    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "com.diffplug.spotless")
    apply(plugin = "io.gitlab.arturbosch.detekt")

    group = rootProject.group
    version = rootProject.version

    extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmExtension> {
        jvmToolchain(21)
        compilerOptions {
            freeCompilerArgs.add("-Xcontext-parameters")
        }
    }

    dependencies {
        "testImplementation"(kotlin("test"))
        "testImplementation"("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
        "detektPlugins"("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.7")
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }

    extensions.configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        kotlin {
            target("src/**/*.kt", "examples/**/*.kt", "src/benchmark/**/*.kt")
            ktlint("1.8.0")
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
            ktlint("1.8.0")
        }
    }

    extensions.configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
        buildUponDefaultConfig = true
        allRules = false
        val rootConfig = rootProject.file("config/detekt/detekt.yml")
        if (rootConfig.exists()) {
            config.setFrom(rootConfig)
        }
        val rootBaseline = rootProject.file("config/detekt/baseline.xml")
        if (rootBaseline.exists()) {
            baseline = rootBaseline
        }
    }

    tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
        // Exclude files using Kotlin 2.2 preview `context(...)` syntax that
        // detekt-formatting (bundled ktlint) cannot yet parse correctly.
        exclude("**/dsl/Routing.kt")
        reports {
            html.required.set(true)
            xml.required.set(true)
            txt.required.set(true)
            sarif.required.set(true)
            md.required.set(true)
        }
    }
}
