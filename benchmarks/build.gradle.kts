plugins {
    kotlin("plugin.allopen") version "2.2.21"
    id("org.jetbrains.kotlinx.benchmark") version "0.4.16"
}

dependencies {
    implementation(project(":kamellia-core"))
    implementation(project(":kamellia-router"))
    implementation(project(":kamellia-middleware"))
    implementation(project(":kamellia-netty"))
    implementation("io.netty:netty-all:4.1.108.Final")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-benchmark-runtime:0.4.16")
}

allOpen {
    annotation("org.openjdk.jmh.annotations.State")
}

benchmark {
    targets {
        register("main")
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
