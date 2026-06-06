plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.allopen)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.kotlin.serialization)

    alias(libs.plugins.jmh)
}

dependencies {
    jmh(libs.jmh.core)
    kaptJmh(libs.jmh.generator.annprocess)

    // tomlkt
    jmh(project(":core"))
    jmh(libs.kotlinx.serialization.core)
    // toml4j
    jmh(libs.toml4j)
    // ktoml
    jmh(libs.ktoml.core)
    // jackson
    jmh(libs.jackson.dataformat.toml)
    jmh(libs.jackson.module.kotlin)
    jmh(libs.jackson.datatype.jsr310)
    // night config
    jmh(libs.night.config.toml)
    // tomlj
    jmh(libs.tomlj)
}

jmh {
    // Both the decoding benchmark (test.DecodeBenchmark) and the encoding
    // benchmark (test.EncodeBenchmark) live in the `test` package.
    includes.set(listOf("test.DecodeBenchmark", "test.EncodeBenchmark"))
}

// Always re-run the benchmark: its inputs rarely change, but cached results
// from a different machine state (load, power profile) are misleading.
tasks.named("jmh") {
    outputs.upToDateWhen { false }
}

allOpen {
    annotation("org.openjdk.jmh.annotations.Measurement")
}

kotlin {
    jvmToolchain(17)
}
