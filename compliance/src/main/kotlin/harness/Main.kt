package harness

import dev.eav.tomlkt.Toml
import dev.eav.tomlkt.TomlArray
import dev.eav.tomlkt.TomlElement
import dev.eav.tomlkt.TomlLiteral
import dev.eav.tomlkt.TomlNull
import dev.eav.tomlkt.TomlTable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import kotlin.system.exitProcess

/**
 * Adapter between tomlkt and the toml-test compliance suite.
 *
 * - decode (default): read TOML on stdin, emit toml-test "tagged" JSON on stdout.
 * - encode:           read tagged JSON on stdin, emit TOML on stdout.
 *
 * On any failure the process exits non-zero, which is how toml-test asserts that
 * "invalid" inputs are rejected.
 */
fun main(args: Array<String>) {
    val mode = args.firstOrNull() ?: "decode"
    // Decode strictly: invalid UTF-8 (and surrogate code points like U+D800,
    // which TOML forbids) must be rejected, so throw rather than substitute the
    // Unicode replacement character. This is what the toml-test encoding/* cases
    // exercise -- the byte/string boundary is the harness's responsibility.
    val input = System.`in`.readBytes().decodeToString(throwOnInvalidSequence = true)
    try {
        when (mode) {
            "decode" -> {
                val table = Toml.parseToTomlTable(input)
                print(Json.encodeToString(JsonElement.serializer(), table.toTaggedJson()))
            }
            "encode" -> {
                val json = Json.parseToJsonElement(input)
                val element = json.toTomlElement()
                print(Toml.encodeToString(TomlElement.serializer(), element))
            }
            else -> error("unknown mode: $mode")
        }
    } catch (e: Throwable) {
        System.err.println("${e::class.simpleName}: ${e.message}")
        exitProcess(1)
    }
}

// -------- decode: TomlElement -> tagged JSON --------

private fun TomlElement.toTaggedJson(): JsonElement = when (this) {
    is TomlTable -> buildJsonObject {
        for ((k, v) in content) put(k, v.toTaggedJson())
    }
    is TomlArray -> JsonArray(content.map { it.toTaggedJson() })
    is TomlLiteral -> taggedValue(tomlTestType(type), normalize(type, content))
    TomlNull -> error("TOML has no null value")
}

private fun taggedValue(type: String, value: String): JsonObject = buildJsonObject {
    put("type", JsonPrimitive(type))
    put("value", JsonPrimitive(value))
}

private fun tomlTestType(type: TomlLiteral.Type): String = when (type) {
    TomlLiteral.Type.Boolean -> "bool"
    TomlLiteral.Type.Integer -> "integer"
    TomlLiteral.Type.Float -> "float"
    TomlLiteral.Type.String -> "string"
    TomlLiteral.Type.LocalDateTime -> "datetime-local"
    TomlLiteral.Type.OffsetDateTime -> "datetime"
    TomlLiteral.Type.LocalDate -> "date-local"
    TomlLiteral.Type.LocalTime -> "time-local"
}

// toml-test expects floats spelled inf/-inf/nan, not the JVM's Infinity/NaN.
private fun normalize(type: TomlLiteral.Type, content: String): String =
    if (type == TomlLiteral.Type.Float) {
        when (content) {
            "Infinity", "+Infinity" -> "inf"
            "-Infinity" -> "-inf"
            "NaN", "-NaN" -> "nan"
            else -> content
        }
    } else {
        content
    }

// -------- encode: tagged JSON -> TomlElement --------

private fun JsonElement.toTomlElement(): TomlElement = when (this) {
    is JsonArray -> TomlArray(map { it.toTomlElement() })
    is JsonObject -> if (isTaggedValue()) toTaggedLiteral() else TomlTable(mapValues { it.value.toTomlElement() })
    is JsonPrimitive -> error("bare primitive is not valid toml-test JSON: $this")
}

private fun JsonObject.isTaggedValue(): Boolean =
    size == 2 && this["type"]?.let { it is JsonPrimitive && it.isString } == true && containsKey("value")

private fun JsonObject.toTaggedLiteral(): TomlLiteral {
    val type = getValue("type").jsonPrimitive.content
    val value = getValue("value").jsonPrimitive.content
    return when (type) {
        "string" -> TomlLiteral(value)
        "integer" -> TomlLiteral(value.toLong())
        "bool" -> TomlLiteral(value.toBooleanStrict())
        "float" -> when (value) {
            "inf", "+inf" -> TomlLiteral(Double.POSITIVE_INFINITY)
            "-inf" -> TomlLiteral(Double.NEGATIVE_INFINITY)
            "nan", "+nan", "-nan" -> TomlLiteral(Double.NaN)
            else -> TomlLiteral(value.toDouble())
        }
        "datetime" -> TomlLiteral(OffsetDateTime.parse(value))
        "datetime-local" -> TomlLiteral(LocalDateTime.parse(value))
        "date-local" -> TomlLiteral(LocalDate.parse(value))
        "time-local" -> TomlLiteral(LocalTime.parse(value))
        else -> error("unknown tagged type: $type")
    }
}
