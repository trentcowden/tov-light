package com.trentcowden.bible

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import com.trentcowden.bible.data.Verse
import kotlin.test.Test
import kotlin.test.assertEquals

class ChapterTextTest {

    private val numberStyle = SpanStyle(color = Color.Gray)

    private fun verse(number: Int, text: String) = Verse(
        id = number, book = 1, bookId = "GEN", bookName = "Genesis",
        chapter = 1, verse = number, text = text,
    )

    private fun paragraphs(vararg verses: Verse) =
        chapterToParagraphs(verses.toList(), numberStyle)

    @Test fun `verses flow together until a paragraph break`() {
        val result = paragraphs(
            verse(1, "In the beginning. "),
            verse(2, "Now the earth.\n\n"),
            verse(3, "And God said. "),
        )
        assertEquals(listOf("1 In the beginning. 2 Now the earth.", "3 And God said."), result.map { it.text })
    }

    @Test fun `a break in the middle of a verse starts a new paragraph`() {
        // Shaped like Genesis 1:5 in the BSB.
        val result = paragraphs(
            verse(5, "God called the light “day.”\n\nAnd there was evening.\n\n"),
            verse(6, "And God said. "),
        )
        assertEquals(
            listOf("5 God called the light “day.”", "And there was evening.", "6 And God said."),
            result.map { it.text },
        )
    }

    @Test fun `only verse numbers get the number style`() {
        val result = paragraphs(verse(9, "Ten words. "), verse(10, "More.\n\n"))
        val styled = result.single().spanStyles.map { result.single().text.substring(it.start, it.end) }
        assertEquals(listOf("9", "10"), styled)
        result.single().spanStyles.forEach { assertEquals(numberStyle, it.item) }
    }

    @Test fun `number styles stay on the right characters after a split`() {
        val result = paragraphs(verse(1, "A.\n\nB. "), verse(2, "C. "))
        val second = result[1]
        assertEquals("B. 2 C.", second.text)
        assertEquals(listOf("2"), second.spanStyles.map { second.text.substring(it.start, it.end) })
    }

    @Test fun `no empty paragraphs from trailing breaks`() {
        assertEquals(1, paragraphs(verse(1, "End.\n\n")).size)
        assertEquals(emptyList(), paragraphs())
    }
}
