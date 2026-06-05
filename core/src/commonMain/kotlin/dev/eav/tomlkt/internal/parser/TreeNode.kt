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

package dev.eav.tomlkt.internal.parser

import dev.eav.tomlkt.TomlElement
import dev.eav.tomlkt.internal.Path

internal sealed class TreeNode(val key: String) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as TreeNode
        return key == other.key
    }

    override fun hashCode(): Int {
        return key.hashCode()
    }
}

internal class KeyNode(
    key: String,
    val isLast: Boolean
) : TreeNode(key) {
    val children: MutableMap<String, TreeNode> = mutableMapOf()

    // Allocated lazily: most documents have no comments or string-style markers,
    // so the common node keeps the shared empty map instead of a per-node one.
    private var lazyAnnotations: MutableMap<String, List<Annotation>>? = null

    val annotations: Map<String, List<Annotation>>
        get() = lazyAnnotations ?: emptyMap()

    fun add(child: TreeNode, childAnnotations: List<Annotation> = emptyList()) {
        children[child.key] = child
        if (childAnnotations.isNotEmpty()) {
            val map = lazyAnnotations
                ?: mutableMapOf<String, List<Annotation>>().also { lazyAnnotations = it }
            map[child.key] = childAnnotations
        }
    }

    operator fun get(key: String): TreeNode? {
        return children[key]
    }
}

internal class ArrayNode(key: String) : TreeNode(key) {
    val children: MutableList<KeyNode> = mutableListOf()

    // Allocated lazily and only once an element actually carries annotations, at
    // which point earlier elements are back-filled so the list stays index-
    // aligned with [children] (the emitter reads it by index).
    private var lazyAnnotations: MutableList<List<Annotation>>? = null

    val annotations: List<List<Annotation>>
        get() = lazyAnnotations ?: emptyList()

    fun add(child: KeyNode, childAnnotations: List<Annotation> = emptyList()) {
        val index = children.size
        children.add(child)
        val existing = lazyAnnotations
        when {
            existing != null -> existing.add(childAnnotations)
            childAnnotations.isNotEmpty() -> {
                lazyAnnotations = ArrayList<List<Annotation>>(index + 1).apply {
                    repeat(index) { add(emptyList()) }
                    add(childAnnotations)
                }
            }
        }
    }

    operator fun get(index: Int): KeyNode {
        return children[index]
    }
}

internal class ValueNode(
    key: String,
    val element: TomlElement
) : TreeNode(key)

// -------- Extensions --------

internal fun KeyNode.addByPath(
    path: Path,
    node: TreeNode,
    arrayOfTableIndices: Map<Path, Int>?,
    annotations: List<Annotation> = emptyList()
): Boolean {
    return addByPathRecursively(path, node, arrayOfTableIndices, annotations, 0)
}

private tailrec fun KeyNode.addByPathRecursively(
    path: Path,
    node: TreeNode,
    arrayOfTableIndices: Map<Path, Int>?,
    annotations: List<Annotation>,
    index: Int
): Boolean {
    val child = get(path[index])
    if (index == path.lastIndex) {
        return when {
            child == null -> {
                add(node, annotations)
                true
            }
            child !is KeyNode -> {
                false
            }
            node !is KeyNode -> {
                false
            }
            else -> {
                // If false, this table is a super-table after a sub-table.
                child.isLast.not()
            }
        }
    }
    return when (child) {
        null -> {
            val intermediate = KeyNode(path[index], isLast = node is ValueNode)
            add(intermediate)
            intermediate.addByPathRecursively(path, node, arrayOfTableIndices, annotations, index + 1)
        }
        is KeyNode -> {
            child.addByPathRecursively(path, node, arrayOfTableIndices, annotations, index + 1)
        }
        is ArrayNode -> {
            check(arrayOfTableIndices != null)
            val currentPath = path.subList(0, index + 1)
            val childIndex = arrayOfTableIndices[currentPath]!!
            val grandChild = child[childIndex]
            grandChild.addByPathRecursively(path, node, arrayOfTableIndices, annotations, index + 1)
        }
        is ValueNode -> {
            false
        }
    }
}

internal fun <N : TreeNode> KeyNode.getByPath(
    path: Path,
    arrayOfTableIndices: Map<Path, Int>?
): N {
    return getByPathRecursively(path, arrayOfTableIndices, 0)
}

private tailrec fun <N : TreeNode> KeyNode.getByPathRecursively(
    path: Path,
    arrayOfTableIndices: Map<Path, Int>?,
    index: Int
): N {
    val child = get(path[index])
    if (index == path.lastIndex) {
        @Suppress("UNCHECKED_CAST")
        return child as? N ?: error("Node on $path not found")
    }
    return when (child) {
        null, is ValueNode -> {
            error("Node on $path not found")
        }
        is KeyNode -> {
            child.getByPathRecursively(path, arrayOfTableIndices, index + 1)
        }
        is ArrayNode -> {
            check(arrayOfTableIndices != null)
            val currentPath = path.subList(0, index + 1)
            val childIndex = arrayOfTableIndices[currentPath]!!
            val grandChild = child[childIndex]
            grandChild.getByPathRecursively(path, arrayOfTableIndices, index + 1)
        }
    }
}
