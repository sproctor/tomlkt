package dev.eav.tomlkt

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The element transforms keep each value glued to its annotation list, so a
 * structural edit (move/insert/remove/replace) can never desync the parallel
 * [TomlArray.annotations] / [TomlTable.annotations] from the content.
 */
class ElementTransformTest {
    private fun comment(text: String): List<Annotation> = listOf(TomlComment(text))

    private fun arr(): TomlArray = buildTomlArray {
        element(TomlLiteral("a"), TomlComment("note a"))
        element(TomlLiteral("b"), TomlComment("note b"))
        element(TomlLiteral("c"), TomlComment("note c"))
    }

    @Test
    fun arrayWithMovedKeepsAnnotationsInLockstep() {
        val moved = arr().withMoved(0, 2)
        assertEquals(listOf("b", "c", "a"), moved.map { (it as TomlLiteral).content })
        assertEquals("note b", (moved.annotationAt(0).single() as TomlComment).text)
        assertEquals("note c", (moved.annotationAt(1).single() as TomlComment).text)
        assertEquals("note a", (moved.annotationAt(2).single() as TomlComment).text)
    }

    @Test
    fun arrayWithInsertedShiftsAnnotations() {
        val inserted = arr().withInserted(1, TomlLiteral("x"), comment("note x"))
        assertEquals(listOf("a", "x", "b", "c"), inserted.map { (it as TomlLiteral).content })
        assertEquals("note a", (inserted.annotationAt(0).single() as TomlComment).text)
        assertEquals("note x", (inserted.annotationAt(1).single() as TomlComment).text)
        assertEquals("note b", (inserted.annotationAt(2).single() as TomlComment).text)
    }

    @Test
    fun arrayWithRemovedAtDropsItsAnnotation() {
        val removed = arr().withRemovedAt(1)
        assertEquals(listOf("a", "c"), removed.map { (it as TomlLiteral).content })
        assertEquals("note a", (removed.annotationAt(0).single() as TomlComment).text)
        assertEquals("note c", (removed.annotationAt(1).single() as TomlComment).text)
    }

    @Test
    fun arrayWithReplacesValueKeepingAnnotationByDefault() {
        val replaced = arr().with(1, TomlLiteral("B"))
        assertEquals(listOf("a", "B", "c"), replaced.map { (it as TomlLiteral).content })
        assertEquals("note b", (replaced.annotationAt(1).single() as TomlComment).text)
    }

    @Test
    fun arrayAnnotationAtIsEmptyWhenUnset() {
        val plain = TomlArray(listOf(TomlLiteral("a")))
        assertEquals(emptyList(), plain.annotationAt(0))
    }

    private fun tbl(): TomlTable = buildTomlTable {
        element("x", TomlLiteral(1))
        element("y", TomlLiteral(2))
    }.annotated("x" to comment("note x"))

    @Test
    fun tableWithReplacesKeepingAnnotationByDefault() {
        val updated = tbl().with("x", TomlLiteral(10))
        assertEquals(TomlLiteral(10), updated["x"])
        assertEquals("note x", (updated.annotationFor("x").single() as TomlComment).text)
    }

    @Test
    fun tableWithNewKeyAppends() {
        val updated = tbl().with("z", TomlLiteral(3), comment("note z"))
        assertEquals(listOf("x", "y", "z"), updated.keys.toList())
        assertEquals("note z", (updated.annotationFor("z").single() as TomlComment).text)
    }

    @Test
    fun tableWithoutDropsKeyAndAnnotation() {
        val updated = tbl().without("x")
        assertEquals(listOf("y"), updated.keys.toList())
        assertEquals(emptyList(), updated.annotationFor("x"))
    }
}
