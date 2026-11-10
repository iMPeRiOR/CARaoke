package com.example.androidautolyrics.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LrcParserTest {
    @Test
    fun parsesFractionsMultipleTimestampsAndOffset() {
        val lines = LrcParser.parse(
            """
            [ar:Example]
            [offset:+100]
            [00:01.2]First line
            [00:02.34][00:03.345]Repeated line
            """.trimIndent(),
        )

        assertEquals(
            listOf(
                LyricLine(1_300, "First line"),
                LyricLine(2_440, "Repeated line"),
                LyricLine(3_445, "Repeated line"),
            ),
            lines,
        )
    }

    @Test
    fun findsOnlyTheMostRecentLine() {
        val lines = listOf(
            LyricLine(1_000, "one"),
            LyricLine(2_000, "two"),
            LyricLine(4_000, "three"),
        )

        assertNull(LrcParser.lineAt(lines, 999))
        assertEquals("one", LrcParser.lineAt(lines, 1_999)?.text)
        assertEquals("two", LrcParser.lineAt(lines, 3_999)?.text)
        assertEquals("three", LrcParser.lineAt(lines, 10_000)?.text)
    }

    @Test
    fun ignoresMetadataAndBlankTimedLines() {
        val lines = LrcParser.parse(
            """
            [ti:Song]
            [00:00.00]
            [00:04.00]Sing this
            """.trimIndent(),
        )

        assertEquals(listOf(LyricLine(4_000, "Sing this")), lines)
    }
}
