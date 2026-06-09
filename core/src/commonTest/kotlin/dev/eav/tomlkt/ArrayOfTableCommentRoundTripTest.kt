package dev.eav.tomlkt

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * With per-entry comments now bound positionally by the parser, a parse -> emit
 * round trip must place each comment directly above its `[[head]]`, with a blank
 * line separating entries but none between a comment and the head it annotates.
 */
class ArrayOfTableCommentRoundTripTest {
    private fun assertRoundTrip(toml: String) {
        val table = Toml.parseToTomlTable(toml)
        assertEquals(toml, Toml.encodeToString(TomlTable.serializer(), table))
    }

    @Test
    fun singleEntryComment() {
        assertRoundTrip(
            """
            # the first highlight
            [[highlights]]
            id = "a"
            """.trimIndent()
        )
    }

    @Test
    fun perEntryComments() {
        assertRoundTrip(
            """
            # highlight A
            [[highlights]]
            id = "a"

            # highlight B
            [[highlights]]
            id = "b"
            """.trimIndent()
        )
    }

    @Test
    fun mixedCommentedAndPlainEntries() {
        assertRoundTrip(
            """
            [[highlights]]
            id = "a"

            # only B is annotated
            [[highlights]]
            id = "b"
            """.trimIndent()
        )
    }
}
