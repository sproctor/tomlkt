package test

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.Measurement
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Param
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.Threads
import org.openjdk.jmh.annotations.Warmup
import org.openjdk.jmh.infra.Blackhole
import java.util.concurrent.TimeUnit

@State(Scope.Thread)
object TomlObjects {
    val tomlkt = dev.eav.tomlkt.Toml
    val toml4j = com.moandjiezana.toml.Toml()
    val ktoml = com.akuleshov7.ktoml.Toml
    val jackson = com.fasterxml.jackson.dataformat.toml.TomlMapper().apply {
        registerKotlinModule()
        registerModule(JavaTimeModule())
    }
    val night = com.electronwill.nightconfig.toml.TomlParser()
}

/*
    Parsing benchmark over a representative subset of the eno-lang sample corpus
    (https://github.com/eno-lang/benchmarks/tree/main/samples). The TOML files
    live in src/jmh/resources/samples and are selected through the `sample`
    parameter. Three samples are kept, each covering a distinct parser workload:

        invented_server_configuration    539 B  nested tables, string arrays, booleans
        yaml_invoice_example             651 B  mixed scalars, arrays-of-tables, multiline literals
        content_heavy                 18971 B  large input, multiline basic string throughput
        escape_heavy                  13510 B  large basic string dense with escape sequences
        literal_heavy                 18971 B  large multiline literal string throughput

    Run a single sample with, e.g., `-p sample=content_heavy`.
 */
@BenchmarkMode(Mode.AverageTime)
@Warmup(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Threads(4)
@Fork(1)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
class Benchmark {
    @Param(
        "invented_server_configuration",
        "yaml_invoice_example",
        "content_heavy",
        // "escape_heavy",
        // "literal_heavy",
    )
    lateinit var sample: String

    private lateinit var content: String

    @Setup
    fun loadSample() {
        val resource = "/samples/$sample.toml"
        content = checkNotNull(javaClass.getResourceAsStream(resource)) {
            "missing sample resource: $resource"
        }.bufferedReader().use { it.readText() }
    }

    @Benchmark
    fun tomlkt(hole: Blackhole) {
        hole.consume(TomlObjects.tomlkt.parseToTomlTable(content))
    }

    @Benchmark
    fun toml4j(hole: Blackhole) {
        hole.consume(TomlObjects.toml4j.read(content))
    }

    // Disabled: ktoml is an order of magnitude slower and dominates run time.
    // @Benchmark
    fun ktoml(hole: Blackhole) {
        hole.consume(TomlObjects.ktoml.tomlParser.parseString(content))
    }

    @Benchmark
    fun jackson(hole: Blackhole) {
        hole.consume(TomlObjects.jackson.readTree(content))
    }

    @Benchmark
    fun night(hole: Blackhole) {
        hole.consume(TomlObjects.night.parse(content))
    }

    // Disabled: tomlj is an order of magnitude slower and dominates run time.
    // @Benchmark
    fun tomlj(hole: Blackhole) {
        hole.consume(org.tomlj.Toml.parse(content))
    }
}
