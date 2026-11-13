package com.trentcowden.tov.data

import androidx.room.Entity

/**
 * A title shown above a chapter's first verse: in the BSB, the Psalm titles (e.g.
 * "A Psalm of David, when he fled from his son Absalom."). Most chapters have none.
 * Maps 1:1 to the `chapter_titles` table built by scripts/build_bible_db.py.
 */
@Entity(tableName = "chapter_titles", primaryKeys = ["book", "chapter"])
data class ChapterTitle(
    val book: Int,
    val chapter: Int,
    val text: String,
)
