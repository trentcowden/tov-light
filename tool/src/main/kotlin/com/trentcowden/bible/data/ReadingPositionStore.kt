package com.trentcowden.bible.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.trentcowden.bible.BibleRef
import kotlinx.coroutines.flow.first

/** Where the reader left off: a chapter, and how far down it they'd scrolled. */
data class ReadingPosition(val ref: BibleRef, val scrollPx: Int)

/**
 * Persists where the user was reading, using the SDK-provided Preferences DataStore
 * (get it from lightContext.dataStore).
 *
 * The scroll offset is in pixels. That's only meaningful for the same layout, which
 * holds here: the tool is portrait-locked and the phone's text size is fixed. If the
 * layout does change, the screen clamps the offset to the chapter's length.
 */
class ReadingPositionStore(private val dataStore: DataStore<Preferences>) {

    /** The saved position, or the top of Genesis 1 the first time the tool is opened. */
    suspend fun load(): ReadingPosition {
        val prefs = dataStore.data.first()
        return ReadingPosition(
            ref = BibleRef(
                book = prefs[BOOK_KEY] ?: DEFAULT_BOOK,
                chapter = prefs[CHAPTER_KEY] ?: DEFAULT_CHAPTER,
            ),
            scrollPx = prefs[SCROLL_KEY] ?: 0,
        )
    }

    /** Save a chapter and scroll offset together, so they're never out of sync. */
    suspend fun save(ref: BibleRef, scrollPx: Int) {
        dataStore.edit { prefs ->
            prefs[BOOK_KEY] = ref.book
            prefs[CHAPTER_KEY] = ref.chapter
            prefs[SCROLL_KEY] = scrollPx
        }
    }

    private companion object {
        val BOOK_KEY = intPreferencesKey("reading_book")
        val CHAPTER_KEY = intPreferencesKey("reading_chapter")
        val SCROLL_KEY = intPreferencesKey("reading_scroll_px")
        const val DEFAULT_BOOK = 1      // Genesis
        const val DEFAULT_CHAPTER = 1
    }
}
