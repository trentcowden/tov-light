package com.trentcowden.bible

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.trentcowden.bible.data.Verse

/**
 * Turns a chapter's verses into display paragraphs, with each verse number styled by
 * [verseNumberStyle].
 *
 * Each verse's text already carries a trailing space, and "\n\n" marks a paragraph
 * break, which can fall mid-verse (e.g. Genesis 1:5). So we build the chapter as one
 * styled string, then split it at the breaks so each paragraph keeps its spans.
 */
fun chapterToParagraphs(
    verses: List<Verse>,
    verseNumberStyle: SpanStyle,
): List<AnnotatedString> {
    val chapter = buildAnnotatedString {
        verses.forEach { verse ->
            withStyle(verseNumberStyle) { append(verse.verse.toString()) }
            append(" ")
            append(verse.text)
        }
    }

    val paragraphs = mutableListOf<AnnotatedString>()
    var start = 0
    while (start < chapter.length) {
        val end = chapter.text.indexOf("\n\n", start).let { if (it == -1) chapter.length else it }
        paragraphs += chapter.subSequence(start, end).trimmed()
        start = end + 2
    }
    return paragraphs.filter { it.isNotEmpty() }
}

private fun AnnotatedString.trimmed(): AnnotatedString {
    val first = text.indexOfFirst { !it.isWhitespace() }
    if (first == -1) return AnnotatedString("")
    return subSequence(first, text.indexOfLast { !it.isWhitespace() } + 1)
}
