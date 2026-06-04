import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// A throwaway harness that adapts tomlkt to the toml-test compliance suite
// (https://github.com/toml-lang/toml-test). Not part of the published library.

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    application
}

dependencies {
    implementation(project(":core"))

    val serializationVersion: String by rootProject
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
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
