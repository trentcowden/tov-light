package com.trentcowden.tov.data

import androidx.room.Dao
import androidx.room.Query

/**
 * A lightweight projection for a table-of-contents list. Not a table itself —
 * Room maps the SELECTed columns onto these fields by name, so the aliases in
 * the query below must match the property names here.
 */
data class BookInfo(
    val book: Int,
    val bookId: String,
    val bookName: String,
    val chapterCount: Int,
)

@Dao
interface VerseDao {
    /** All 66 books with their chapter counts, in canonical order. */
    @Query(
        "SELECT book, book_id AS bookId, book_name AS bookName, MAX(chapter) AS chapterCount " +
            "FROM verses GROUP BY book ORDER BY book"
    )
    suspend fun books(): List<BookInfo>

    /** Every verse of one chapter, in order. */
    @Query("SELECT * FROM verses WHERE book = :book AND chapter = :chapter ORDER BY verse")
    suspend fun chapter(book: Int, chapter: Int): List<Verse>

    /** The chapter's title (a Psalm title), or null if it has none. */
    @Query("SELECT text FROM chapter_titles WHERE book = :book AND chapter = :chapter")
    suspend fun chapterTitle(book: Int, chapter: Int): String?

    /** Number of chapters in a book — handy for next/previous navigation bounds. */
    @Query("SELECT MAX(chapter) FROM verses WHERE book = :book")
    suspend fun chapterCount(book: Int): Int

    /** Simple substring search. Fine to start; swap for SQLite FTS later if you want ranking. */
    @Query(
        "SELECT * FROM verses WHERE text LIKE '%' || :query || '%' " +
            "ORDER BY book, chapter, verse LIMIT :limit"
    )
    suspend fun search(query: String, limit: Int): List<Verse>
}
