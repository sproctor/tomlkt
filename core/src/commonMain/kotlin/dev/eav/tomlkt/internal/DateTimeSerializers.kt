/*
    Copyright 2026 Loney Chou

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
 */

package dev.eav.tomlkt.internal

import dev.eav.tomlkt.NativeLocalDate
import dev.eav.tomlkt.NativeLocalDateTime
import dev.eav.tomlkt.NativeLocalTime
import dev.eav.tomlkt.NativeOffsetDateTime
import dev.eav.tomlkt.TomlDecoder
import dev.eav.tomlkt.TomlEncoder
import dev.eav.tomlkt.TomlLiteral
import dev.eav.tomlkt.asTomlLiteral
import dev.eav.tomlkt.toLocalDate
import dev.eav.tomlkt.toLocalDateTime
import dev.eav.tomlkt.toLocalTime
import dev.eav.tomlkt.toOffsetDateTime
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

internal object LocalDateTimeSerializer : KSerializer<NativeLocalDateTime> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        serialName = "dev.eav.tomlkt.NativeLocalDateTime",
        kind = PrimitiveKind.STRING
    )

    override fun serialize(encoder: Encoder, value: NativeLocalDateTime) {
        if (encoder is TomlEncoder) {
            encoder.encodeTomlElement(TomlLiteral(value))
        } else {
            encoder.encodeString(value.toString())
        }
    }

    override fun deserialize(decoder: Decoder): NativeLocalDateTime {
        return if (decoder is TomlDecoder) {
            decoder.decodeTomlElement().asTomlLiteral().toLocalDateTime()
        } else {
            NativeLocalDateTime(decoder.decodeString())
        }
    }
}

internal object OffsetDateTimeSerializer : KSerializer<NativeOffsetDateTime> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        serialName = "dev.eav.tomlkt.NativeOffsetDateTime",
        kind = PrimitiveKind.STRING
    )

    override fun serialize(encoder: Encoder, value: NativeOffsetDateTime) {
        if (encoder is TomlEncoder) {
            encoder.encodeTomlElement(TomlLiteral(value))
        } else {
            encoder.encodeString(value.toString())
        }
    }

    override fun deserialize(decoder: Decoder): NativeOffsetDateTime {
        return if (decoder is TomlDecoder) {
            decoder.decodeTomlElement().asTomlLiteral().toOffsetDateTime()
        } else {
            NativeOffsetDateTime(decoder.decodeString())
        }
    }
}

internal object LocalDateSerializer : KSerializer<NativeLocalDate> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        serialName = "dev.eav.tomlkt.NativeLocalDate",
        kind = PrimitiveKind.STRING
    )

    override fun serialize(encoder: Encoder, value: NativeLocalDate) {
        if (encoder is TomlEncoder) {
            encoder.encodeTomlElement(TomlLiteral(value))
        } else {
            encoder.encodeString(value.toString())
        }
    }

    override fun deserialize(decoder: Decoder): NativeLocalDate {
        return if (decoder is TomlDecoder) {
            decoder.decodeTomlElement().asTomlLiteral().toLocalDate()
        } else {
            NativeLocalDate(decoder.decodeString())
        }
    }
}

internal object LocalTimeSerializer : KSerializer<NativeLocalTime> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        serialName = "dev.eav.tomlkt.NativeLocalTime",
        kind = PrimitiveKind.STRING
    )

    override fun serialize(encoder: Encoder, value: NativeLocalTime) {
        if (encoder is TomlEncoder) {
            encoder.encodeTomlElement(TomlLiteral(value))
        } else {
            encoder.encodeString(value.toString())
        }
    }

    override fun deserialize(decoder: Decoder): NativeLocalTime {
        return if (decoder is TomlDecoder) {
            decoder.decodeTomlElement().asTomlLiteral().toLocalTime()
        } else {
            NativeLocalTime(decoder.decodeString())
        }
    }
}
