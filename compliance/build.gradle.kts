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

// ---- toml-test compliance runner --------------------------------------------
//
// `./gradlew :compliance:complianceTest` builds the harness and runs the whole
// toml-test suite (decoder + encoder) against it. Configure with:
//   -PtomlVersion=1.0.0   TOML version to test (default 1.1.0)
//   -PtomlTest=/path/to/toml-test   the runner (else $TOML_TEST, ~/go/bin, PATH)
// Install toml-test with:
//   go install github.com/toml-lang/toml-test/cmd/toml-test@latest

// The `application` plugin's installDist produces this launcher.
val harnessLauncher = layout.buildDirectory.file("install/compliance/bin/compliance")

val tomlVersion = (findProperty("tomlVersion") as String?) ?: "1.1.0"

// These valid encoder cases "fail" only because toml-test's own bundled
// reference parser cannot decode its canonical TOML 1.1 output; tomlkt emits
// valid TOML for all of them. Skip them so the encoder suite is green.
val knownEncoderHarnessBugs = listOf(
    "valid/string/escape-esc",
    "valid/string/hex-escape",
    "valid/datetime/no-seconds",
    "valid/inline-table/newline",
)

fun resolveTomlTest(): String {
    (findProperty("tomlTest") as String?)?.let { return it }
    System.getenv("TOML_TEST")?.let { return it }
    val home = System.getProperty("user.home")
    val goBin = file("$home/go/bin/toml-test")
    if (goBin.exists()) {
        return goBin.absolutePath
    }
    // Assume it is on PATH.
    return "toml-test"
}

// Resolve the runner and fail with install instructions when an explicit path
// does not exist. A bare "toml-test" is left for the OS to resolve on PATH.
fun requireTomlTest(): String {
    val tomlTest = resolveTomlTest()
    if (tomlTest.contains('/') && !file(tomlTest).exists()) {
        throw GradleException(
            "toml-test runner not found at '$tomlTest'. Install it with:\n" +
                "  go install github.com/toml-lang/toml-test/cmd/toml-test@latest\n" +
                "or point at it with -PtomlTest=/path/to/toml-test (or \$TOML_TEST).",
        )
    }
    return tomlTest
}

val complianceDecode by tasks.registering(Exec::class) {
    group = "verification"
    description = "Run the toml-test decoder suite against the harness."
    dependsOn(tasks.named("installDist"))
    doFirst {
        commandLine(
            requireTomlTest(),
            "-toml", tomlVersion,
            harnessLauncher.get().asFile.absolutePath, "decode",
        )
    }
}

val complianceEncode by tasks.registering(Exec::class) {
    group = "verification"
    description = "Run the toml-test encoder suite against the harness."
    dependsOn(tasks.named("installDist"))
    doFirst {
        commandLine(
            buildList {
                add(requireTomlTest())
                add("-toml"); add(tomlVersion)
                add("-encoder")
                knownEncoderHarnessBugs.forEach { add("-skip"); add(it) }
                add(harnessLauncher.get().asFile.absolutePath); add("encode")
            },
        )
    }
}

tasks.register("complianceTest") {
    group = "verification"
    description = "Build the harness and run the full toml-test suite (decode + encode)."
    dependsOn(complianceDecode, complianceEncode)
}
