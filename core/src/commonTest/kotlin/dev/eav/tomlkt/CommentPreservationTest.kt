package dev.eav.tomlkt

import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The comment-carrying encode path: decode-edit-re-encode a typed model while
 * grafting the user's comments back from a previously parsed template.
 */
class CommentPreservationTest {
    @Serializable
    data class Highlight(val id: String, val pattern: String)

    @Serializable
    data class Config(val title: String, val highlights: List<Highlight>)

    private val byId: (TomlElement) -> Any? = { element ->
        (element as? TomlTable)?.get("id")?.let { (it as? TomlLiteral)?.content }
    }

    private val template = """
        # the title
        title = "Mine"

        # highlight A
        [[highlights]]
        id = "a"
        pattern = "foo"

        # highlight B
        [[highlights]]
        id = "b"
        pattern = "bar"
    """.trimIndent()

    @Test
    fun valueOnlyEditPreservesComments() {
        val parsed = Toml.parseToTomlTable(template)
        val config = Toml.decodeFromTomlElement(Config.serializer(), parsed)
        // No structural change: re-encoding with the template restores comments.
        val out = Toml.encodeToString(Config.serializer(), config, preserveCommentsFrom = parsed, arrayElementKey = byId)
        assertEquals(template, out)
    }

    @Test
    fun reorderCarriesPerEntryCommentsByIdentity() {
        val parsed = Toml.parseToTomlTable(template)
        val config = Toml.decodeFromTomlElement(Config.serializer(), parsed)
        // Reorder highlights (B before A) and edit A's pattern.
        val edited = config.copy(
            highlights = listOf(
                config.highlights[1],
                config.highlights[0].copy(pattern = "FOO")
            )
        )
        val out = Toml.encodeToString(Config.serializer(), edited, preserveCommentsFrom = parsed, arrayElementKey = byId)
        assertEquals(
            """
            # the title
            title = "Mine"

            # highlight B
            [[highlights]]
            id = "b"
            pattern = "bar"

            # highlight A
            [[highlights]]
            id = "a"
            pattern = "FOO"
            """.trimIndent(),
            out
        )
    }

    @Test
    fun newEntryStartsBlankAndDeletedDropsComment() {
        val parsed = Toml.parseToTomlTable(template)
        val config = Toml.decodeFromTomlElement(Config.serializer(), parsed)
        // Drop A, keep B, add a brand-new C.
        val edited = config.copy(
            highlights = listOf(
                config.highlights[1],
                Highlight(id = "c", pattern = "baz")
            )
        )
        val out = Toml.encodeToString(Config.serializer(), edited, preserveCommentsFrom = parsed, arrayElementKey = byId)
        assertEquals(
            """
            # the title
            title = "Mine"

            # highlight B
            [[highlights]]
            id = "b"
            pattern = "bar"

            [[highlights]]
            id = "c"
            pattern = "baz"
            """.trimIndent(),
            out
        )
    }

    @Test
    fun nullTemplateIsPlainEncode() {
        val config = Config("X", listOf(Highlight("a", "foo")))
        val withNull = Toml.encodeToString(Config.serializer(), config, preserveCommentsFrom = null)
        val plain = Toml.encodeToString(Config.serializer(), config)
        assertEquals(plain, withNull)
    }
}
