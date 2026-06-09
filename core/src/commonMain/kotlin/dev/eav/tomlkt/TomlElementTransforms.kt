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

/*
 * Structural edits for the immutable [TomlElement] tree. Each transform keeps a
 * value glued to its annotation slot, so the parallel annotation list/map can
 * never desync from the content -- the failure mode of editing the two by hand.
 * Every transform returns a new instance and leaves the receiver untouched.
 */

// -------- TomlArray --------

/**
 * Returns the annotations attached to the value at [index], or an empty list if
 * that slot carries none.
 *
 * Unlike indexing [TomlArray.annotations] directly, this is safe when the
 * annotation list is shorter than the content (a value with no annotations need
 * not occupy a slot).
 */
public fun TomlArray.annotationAt(index: Int): List<Annotation> {
    return annotations.getOrNull(index) ?: emptyList()
}

/**
 * Returns a copy with the value at [index] replaced by [element], keeping the
 * existing annotations for that slot unless [annotations] is given.
 *
 * **NOTE**: this returns a new instance.
 */
public fun TomlArray.with(
    index: Int,
    element: TomlElement,
    annotations: List<Annotation> = annotationAt(index)
): TomlArray {
    val content = toMutableList().also { it[index] = element }
    val newAnnotations = normalizedAnnotations().also { it[index] = annotations }
    return TomlArray(content, newAnnotations)
}

/**
 * Returns a copy with [element] inserted at [index], shifting later values and
 * their annotations up by one.
 *
 * **NOTE**: this returns a new instance.
 */
public fun TomlArray.withInserted(
    index: Int,
    element: TomlElement,
    annotations: List<Annotation> = emptyList()
): TomlArray {
    val content = toMutableList().also { it.add(index, element) }
    val newAnnotations = normalizedAnnotations().also { it.add(index, annotations) }
    return TomlArray(content, newAnnotations)
}

/**
 * Returns a copy with the value at [index] -- and its annotations -- removed.
 *
 * **NOTE**: this returns a new instance.
 */
public fun TomlArray.withRemovedAt(index: Int): TomlArray {
    val content = toMutableList().also { it.removeAt(index) }
    val newAnnotations = normalizedAnnotations().also { it.removeAt(index) }
    return TomlArray(content, newAnnotations)
}

/**
 * Returns a copy with the value at [fromIndex] moved to [toIndex], carrying its
 * annotations with it so a reorder never strands a comment at the old position.
 *
 * **NOTE**: this returns a new instance.
 */
public fun TomlArray.withMoved(fromIndex: Int, toIndex: Int): TomlArray {
    if (fromIndex == toIndex) {
        return this
    }
    val content = toMutableList()
    val newAnnotations = normalizedAnnotations()
    content.add(toIndex, content.removeAt(fromIndex))
    newAnnotations.add(toIndex, newAnnotations.removeAt(fromIndex))
    return TomlArray(content, newAnnotations)
}

// A per-value annotation list of exactly [size] entries, so that index-based
// edits stay aligned even when the source annotations were sparse or empty.
private fun TomlArray.normalizedAnnotations(): MutableList<List<Annotation>> {
    return MutableList(size) { annotations.getOrNull(it) ?: emptyList() }
}

// -------- TomlTable --------

/**
 * Returns the annotations attached to [key], or an empty list if that entry
 * carries none.
 */
public fun TomlTable.annotationFor(key: String): List<Annotation> {
    return annotations[key] ?: emptyList()
}

/**
 * Returns a copy with [key] mapped to [element], keeping the existing
 * annotations for that key unless [annotations] is given. A new key is appended;
 * an existing key keeps its position.
 *
 * **NOTE**: this returns a new instance.
 */
public fun TomlTable.with(
    key: String,
    element: TomlElement,
    annotations: List<Annotation> = annotationFor(key)
): TomlTable {
    val content = toMutableMap().also { it[key] = element }
    val newAnnotations = this.annotations.toMutableMap()
    if (annotations.isEmpty()) {
        newAnnotations.remove(key)
    } else {
        newAnnotations[key] = annotations
    }
    return TomlTable(content, newAnnotations)
}

/**
 * Returns a copy with [key] -- and its annotations -- removed.
 *
 * **NOTE**: this returns a new instance.
 */
public fun TomlTable.without(key: String): TomlTable {
    val content = toMutableMap().also { it.remove(key) }
    val newAnnotations = annotations.toMutableMap().also { it.remove(key) }
    return TomlTable(content, newAnnotations)
}
