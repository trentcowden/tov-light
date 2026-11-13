package com.trentcowden.bible

/**
 * Moving between chapters across book boundaries. Pure logic so it can be unit tested;
 * [chapterCount] looks up how many chapters a book has (the DAO, in the app).
 */
object ChapterNav {
    const val FIRST_BOOK = 1    // Genesis
    const val LAST_BOOK = 66    // Revelation

    /** The chapter after [ref], rolling into the next book. Null at Revelation 22. */
    suspend fun next(ref: BibleRef, chapterCount: suspend (book: Int) -> Int): BibleRef? = when {
        ref.chapter < chapterCount(ref.book) -> ref.copy(chapter = ref.chapter + 1)
        ref.book < LAST_BOOK -> BibleRef(ref.book + 1, 1)
        else -> null
    }

    /** The chapter before [ref], rolling into the previous book's last chapter. Null at Genesis 1. */
    suspend fun previous(ref: BibleRef, chapterCount: suspend (book: Int) -> Int): BibleRef? = when {
        ref.chapter > 1 -> ref.copy(chapter = ref.chapter - 1)
        ref.book > FIRST_BOOK -> BibleRef(ref.book - 1, chapterCount(ref.book - 1))
        else -> null
    }

    fun isFirst(ref: BibleRef) = ref.book == FIRST_BOOK && ref.chapter == 1

    fun isLast(ref: BibleRef, lastChapterInBook: Int) =
        ref.book == LAST_BOOK && ref.chapter == lastChapterInBook
}
