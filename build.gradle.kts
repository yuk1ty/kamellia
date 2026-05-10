plugins {
    kotlin("jvm") version "2.2.21" apply false
    kotlin("plugin.serialization") version "2.2.21" apply false
    id("com.diffplug.spotless") version "7.0.2" apply false
    id("io.gitlab.arturbosch.detekt") version "1.23.7" apply false
}

allprojects {
    group = "io.github.kamellia"
    version = "0.1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    if (name == "kamellia-legacy") return@subprojects

    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "com.diffplug.spotless")
    apply(plugin = "io.gitlab.arturbosch.detekt")

    extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmExtension> {
        jvmToolchain(21)
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
            target("src/**/*.kt")
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
        config.setFrom(rootProject.file("config/detekt/detekt.yml"))
        val baseline = rootProject.file("config/detekt/baseline.xml")
        if (baseline.exists()) {
            this.baseline = baseline
        }
        source.setFrom(
            "src/main/kotlin",
            "src/test/kotlin",
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
}
