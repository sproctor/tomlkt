import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// A throwaway harness that adapts tomlkt to the toml-test compliance suite
// (https://github.com/toml-lang/toml-test). Not part of the published library.

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

dependencies {
    implementation(project(":core"))

    implementation(libs.kotlinx.serialization.json)
}

application {
    // First arg selects direction: "decode" (default) or "encode".
    mainClass = "harness.MainKt"
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
        optIn.add("dev.eav.tomlkt.TomlSpecific")
    }
}
