package dev.eav.tomlkt

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pins where the parser attaches comments in the resulting [TomlElement] tree,
 * which the comment-preserving encode path then reads. A leading comment before
 * the i-th `[[aot]]` head binds to that element's positional slot
 * ([TomlArray.annotations] index i), not to the parent key slot, so every entry
 * keeps its own comment and the attribution survives a reorder.
 */
class CommentPlacementTest {
    private fun firstComment(annotations: List<Annotation>): String? {
        return annotations.filterIsInstance<TomlComment>().firstOrNull { !it.inline }?.text
    }

    @Test
    fun perEntryLeadingCommentsBindPositionally() {
        val text = """
            # banner before A
            [[h]]
            id = "a"
            # banner before B
            [[h]]
            id = "b"
        """.trimIndent()
        val table = Toml.parseToTomlTable(text)
        val array = table["h"] as TomlArray

        // No comment lands on the parent key slot anymore.
        assertEquals(emptyList(), table.annotations["h"] ?: emptyList<Annotation>())

        // Each entry keeps its own leading comment, by position.
        assertEquals(2, array.annotations.size)
        assertEquals("banner before A", firstComment(array.annotations[0]))
        assertEquals("banner before B", firstComment(array.annotations[1]))
    }

    @Test
    fun singleEntryLeadingCommentBindsToElementZero() {
        val text = """
            # only one
            [[h]]
            id = "a"
        """.trimIndent()
        val array = Toml.parseToTomlTable(text)["h"] as TomlArray
        assertEquals("only one", firstComment(array.annotations[0]))
    }
}
