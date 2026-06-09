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

package dev.eav.tomlkt

import kotlinx.serialization.SerializationStrategy

/**
 * Serializes [value] into a [TomlElement] using [serializer], then grafts the
 * comments from a previously parsed document ([preserveCommentsFrom]) onto the
 * matching nodes of the freshly encoded tree.
 *
 * This bridges the typed world (`encodeToTomlElement`) and the document world
 * (`parseToTomlTable`, where comments live in [TomlTable.annotations] /
 * [TomlArray.annotations]): a consumer can keep a typed model and still round-
 * trip the user's comments. The typical flow is to parse the file once for the
 * template, decode it for reads, and on save pass the same template here.
 *
 * Carry rules:
 * - **Comments come from the template; formatting comes from the value.** For a
 *   slot present in both, the carried annotations are the encoded slot's
 *   non-comment annotations (so schema formatting such as [TomlInline] or
 *   [TomlInteger] is preserved) plus the template slot's [TomlComment]s. A slot
 *   present only in the encoded value keeps its own annotations verbatim,
 *   including any schema [TomlComment].
 * - **Tables match by key**; keys only in the template are dropped (the value no
 *   longer has them).
 * - **Arrays match by identity** when [arrayElementKey] is supplied: an element's
 *   key maps it to a template element regardless of position, so a reorder
 *   carries each comment to the element's new slot, a removed element drops its
 *   comment, and a new element (no matching key) starts blank. When the key is
 *   `null` (the default for every element, or when the extractor returns `null`),
 *   elements match positionally. Duplicate keys in the template resolve to the
 *   first occurrence.
 *
 * If [preserveCommentsFrom] is `null`, this is equivalent to the plain
 * [encodeToTomlElement].
 *
 * @throws TomlEncodingException if `value` cannot be serialized.
 */
public fun <T> Toml.encodeToTomlElement(
    serializer: SerializationStrategy<T>,
    value: T,
    preserveCommentsFrom: TomlElement?,
    arrayElementKey: (TomlElement) -> Any? = { null }
): TomlElement {
    val encoded = encodeToTomlElement(serializer, value)
    if (preserveCommentsFrom == null) {
        return encoded
    }
    return carryComments(encoded, preserveCommentsFrom, arrayElementKey)
}

/**
 * Serializes [value] into a TOML string using [serializer], carrying the
 * comments from a previously parsed document ([preserveCommentsFrom]) onto the
 * output. See [encodeToTomlElement] for the carry rules.
 *
 * Only comments are preserved, not byte-exact formatting: the emitter still owns
 * blank lines, indentation, and inline-table spacing, so the user's comments
 * come back on the right nodes but with tomlkt's canonical whitespace.
 *
 * @throws TomlEncodingException if `value` cannot be serialized.
 */
public fun <T> Toml.encodeToString(
    serializer: SerializationStrategy<T>,
    value: T,
    preserveCommentsFrom: TomlElement?,
    arrayElementKey: (TomlElement) -> Any? = { null }
): String {
    val element = encodeToTomlElement(serializer, value, preserveCommentsFrom, arrayElementKey)
    return encodeToString(TomlElement.serializer(), element)
}

// ======== Internal ========

private fun carryComments(
    encoded: TomlElement,
    template: TomlElement?,
    idOf: (TomlElement) -> Any?
): TomlElement {
    return when {
        encoded is TomlTable && template is TomlTable -> carryTable(encoded, template, idOf)
        encoded is TomlArray && template is TomlArray -> carryArray(encoded, template, idOf)
        // Scalars and type mismatches: the parent decides this slot's comments;
        // the value itself is returned unchanged.
        else -> encoded
    }
}

private fun carryTable(
    encoded: TomlTable,
    template: TomlTable,
    idOf: (TomlElement) -> Any?
): TomlTable {
    val content = LinkedHashMap<String, TomlElement>(encoded.size)
    val annotations = LinkedHashMap<String, List<Annotation>>()
    for ((key, element) in encoded) {
        val inTemplate = template.containsKey(key)
        content[key] = carryComments(element, if (inTemplate) template[key] else null, idOf)
        val merged = mergeAnnotations(
            encoded = encoded.annotationFor(key),
            template = if (inTemplate) template.annotationFor(key) else null
        )
        if (merged.isNotEmpty()) {
            annotations[key] = merged
        }
    }
    return TomlTable(content, annotations)
}

private fun carryArray(
    encoded: TomlArray,
    template: TomlArray,
    idOf: (TomlElement) -> Any?
): TomlArray {
    val templateIndexById = HashMap<Any, Int>()
    template.forEachIndexed { index, element ->
        idOf(element)?.let { id -> if (id !in templateIndexById) templateIndexById[id] = index }
    }
    val content = ArrayList<TomlElement>(encoded.size)
    val annotations = ArrayList<List<Annotation>>(encoded.size)
    encoded.forEachIndexed { index, element ->
        val id = idOf(element)
        val matchIndex = if (id != null) {
            templateIndexById[id]
        } else {
            index.takeIf { it in template.indices }
        }
        content.add(carryComments(element, matchIndex?.let { template[it] }, idOf))
        annotations.add(
            mergeAnnotations(
                encoded = encoded.annotationAt(index),
                template = matchIndex?.let { template.annotationAt(it) }
            )
        )
    }
    return TomlArray(content, annotations)
}

// Formatting from the encoded slot, comments from the template slot. A slot with
// no template match (template == null) keeps the encoded annotations verbatim.
private fun mergeAnnotations(
    encoded: List<Annotation>,
    template: List<Annotation>?
): List<Annotation> {
    if (template == null) {
        return encoded
    }
    val nonComments = encoded.filterNot { it is TomlComment }
    val comments = template.filterIsInstance<TomlComment>()
    return nonComments + comments
}
