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

@file:OptIn(InternalSerializationApi::class, SealedSerializationApi::class)

package dev.eav.tomlkt.internal

import dev.eav.tomlkt.TomlArray
import dev.eav.tomlkt.TomlElement
import dev.eav.tomlkt.TomlLiteral
import dev.eav.tomlkt.TomlNull
import dev.eav.tomlkt.TomlTable
import dev.eav.tomlkt.asTomlArray
import dev.eav.tomlkt.asTomlDecoder
import dev.eav.tomlkt.asTomlEncoder
import dev.eav.tomlkt.asTomlLiteral
import dev.eav.tomlkt.asTomlNull
import dev.eav.tomlkt.asTomlTable
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SealedSerializationApi
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

// -------- TomlElementSerializer --------

internal object TomlElementSerializer : KSerializer<TomlElement> {
    override val descriptor: SerialDescriptor = buildSerialDescriptor(
        serialName = "dev.eav.tomlkt.TomlElement",
        kind = SerialKind.CONTEXTUAL
    )

    override fun serialize(encoder: Encoder, value: TomlElement) {
        encoder.asTomlEncoder().encodeTomlElement(value)
    }

    override fun deserialize(decoder: Decoder): TomlElement {
        return decoder.asTomlDecoder().decodeTomlElement()
    }
}

// -------- TomlNullSerializer --------

internal object TomlNullSerializer : KSerializer<TomlNull> {
    override val descriptor: SerialDescriptor = buildSerialDescriptor(
        serialName = "dev.eav.tomlkt.TomlNull",
        kind = SerialKind.CONTEXTUAL
    )

    override fun serialize(encoder: Encoder, value: TomlNull) {
        encoder.asTomlEncoder().encodeTomlElement(value)
    }

    override fun deserialize(decoder: Decoder): TomlNull {
        return decoder.asTomlDecoder().decodeTomlElement().asTomlNull()
    }
}

// -------- TomlLiteralSerializer --------

internal object TomlLiteralSerializer : KSerializer<TomlLiteral> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        serialName = "dev.eav.tomlkt.TomlLiteral",
        kind = PrimitiveKind.STRING
    )

    override fun serialize(encoder: Encoder, value: TomlLiteral) {
        encoder.asTomlEncoder().encodeTomlElement(value)
    }

    override fun deserialize(decoder: Decoder): TomlLiteral {
        return decoder.asTomlDecoder().decodeTomlElement().asTomlLiteral()
    }
}

// -------- TomlArraySerializer --------

internal object TomlArraySerializer : KSerializer<TomlArray> {
    private val delegate: KSerializer<List<TomlElement>> = ListSerializer(
        elementSerializer = TomlElement.serializer()
    )

    override val descriptor: SerialDescriptor = object : SerialDescriptor by delegate.descriptor {
        override val serialName: String = "dev.eav.tomlkt.TomlArray"
    }

    override fun serialize(encoder: Encoder, value: TomlArray) {
        delegate.serialize(encoder.asTomlEncoder(), value)
    }

    override fun deserialize(decoder: Decoder): TomlArray {
        return decoder.asTomlDecoder().decodeTomlElement().asTomlArray()
    }
}

// -------- TomlTableSerializer --------

internal object TomlTableSerializer : KSerializer<TomlTable> {
    private val delegate: KSerializer<Map<String, TomlElement>> = MapSerializer(
        keySerializer = String.serializer(),
        valueSerializer = TomlElement.serializer()
    )

    override val descriptor: SerialDescriptor = object : SerialDescriptor by delegate.descriptor {
        override val serialName: String = "dev.eav.tomlkt.TomlTable"
    }

    override fun serialize(encoder: Encoder, value: TomlTable) {
        delegate.serialize(encoder.asTomlEncoder(), value)
    }

    override fun deserialize(decoder: Decoder): TomlTable {
        return decoder.asTomlDecoder().decodeTomlElement().asTomlTable()
    }
}
