package com.trentcowden.bible

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChapterNavTest {

    // Real chapter counts for the books these tests touch.
    private val counts = mapOf(1 to 50, 2 to 40, 64 to 1, 65 to 1, 66 to 22)
    private val chapterCount: suspend (Int) -> Int = { book -> counts.getValue(book) }

    private fun next(book: Int, chapter: Int) =
        runBlocking { ChapterNav.next(BibleRef(book, chapter), chapterCount) }

    private fun previous(book: Int, chapter: Int) =
        runBlocking { ChapterNav.previous(BibleRef(book, chapter), chapterCount) }

    @Test fun `next moves within a book`() =
        assertEquals(BibleRef(1, 2), next(1, 1))

    @Test fun `next rolls into the following book`() =
        assertEquals(BibleRef(2, 1), next(1, 50))

    @Test fun `next steps through one-chapter books`() {
        assertEquals(BibleRef(65, 1), next(64, 1))   // 3 John -> Jude
        assertEquals(BibleRef(66, 1), next(65, 1))   // Jude -> Revelation
    }

    @Test fun `next stops at Revelation 22`() =
        assertNull(next(66, 22))

    @Test fun `previous moves within a book`() =
        assertEquals(BibleRef(2, 39), previous(2, 40))

    @Test fun `previous rolls into the last chapter of the previous book`() =
        assertEquals(BibleRef(1, 50), previous(2, 1))

    @Test fun `previous stops at Genesis 1`() =
        assertNull(previous(1, 1))

    @Test fun `first and last chapters of the Bible`() {
        assertTrue(ChapterNav.isFirst(BibleRef(1, 1)))
        assertFalse(ChapterNav.isFirst(BibleRef(1, 2)))
        assertTrue(ChapterNav.isLast(BibleRef(66, 22), lastChapterInBook = 22))
        assertFalse(ChapterNav.isLast(BibleRef(66, 21), lastChapterInBook = 22))
        assertFalse(ChapterNav.isLast(BibleRef(65, 1), lastChapterInBook = 1))
    }
}
