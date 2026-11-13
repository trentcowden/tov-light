package com.trentcowden.tov.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One verse. Columns map 1:1 to the `verses` table in the prebuilt bible.db
 * produced by scripts/build_bible_db.py. If you change a column here, change it
 * there too (and rebuild the .db) or reads will break.
 */
@Entity(
    tableName = "verses",
    indices = [Index(value = ["book", "chapter"], name = "index_verses_book_chapter")],
)
data class Verse(
    @PrimaryKey val id: Int,
    val book: Int,                                          // canonical book number, 1..66
    @ColumnInfo(name = "book_id") val bookId: String,       // e.g. "JHN"
    @ColumnInfo(name = "book_name") val bookName: String,   // e.g. "John"
    val chapter: Int,
    val verse: Int,
    val text: String,
)
