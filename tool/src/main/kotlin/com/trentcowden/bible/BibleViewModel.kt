package com.trentcowden.bible

import androidx.lifecycle.viewModelScope
import com.trentcowden.bible.data.BibleDb
import com.trentcowden.bible.data.ReadingPositionStore
import com.trentcowden.bible.data.Verse
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SimpleLightScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class BibleViewModel(
    private val db: BibleDb,
    private val positionStore: ReadingPositionStore,
) : LightViewModel<Unit>() {

    /** Everything the screen needs to draw one chapter, in a single object. */
    data class ChapterState(
        val reference: String = "",          // e.g. "John 3"
        val verses: List<Verse> = emptyList(),
        val hasPrevious: Boolean = false,
        val hasNext: Boolean = false,
    )

    private val _state = MutableStateFlow(ChapterState())
    val state: StateFlow<ChapterState> = _state

    // Where we currently are. Kept private — the screen only reads _state.
    // Seeded from the saved position on first show.
    private var book = 1
    private var chapter = 1

    override fun onScreenShow(screen: SimpleLightScreen<Unit>) {
        super.onScreenShow(screen)
        // Only load on first show. On return from the book/chapter picker the state
        // is already populated, and the picker drives the update via goTo().
        if (_state.value.verses.isEmpty()) viewModelScope.launch {
            val saved = positionStore.load()
            load(saved.book, saved.chapter)
        }
    }

    /** Jump straight to a chapter chosen from the picker. */
    fun goTo(book: Int, chapter: Int) = viewModelScope.launch { load(book, chapter) }

    /** Advance one chapter, rolling into the next book at a book's end. No-op at Revelation 22. */
    fun next() = viewModelScope.launch {
        val lastInBook = db.verseDao().chapterCount(book)
        when {
            chapter < lastInBook -> load(book, chapter + 1)
            book < LAST_BOOK -> load(book + 1, 1)
        }
    }

    /** Go back one chapter, rolling into the previous book at a book's start. No-op at Genesis 1. */
    fun previous() = viewModelScope.launch {
        when {
            chapter > 1 -> load(book, chapter - 1)
            book > FIRST_BOOK -> load(book - 1, db.verseDao().chapterCount(book - 1))
        }
    }

    // Room suspend queries are main-safe, so no Dispatchers.IO needed.
    private suspend fun load(book: Int, chapter: Int) {
        val dao = db.verseDao()
        val lastInBook = dao.chapterCount(book)
        val target = chapter.coerceIn(1, lastInBook)
        val verses = dao.chapter(book, target)

        this.book = book
        this.chapter = target
        _state.value = ChapterState(
            reference = verses.firstOrNull()?.let { "${it.bookName} ${it.chapter}" } ?: "",
            verses = verses,
            // Chevrons hide only at the two ends of the whole Bible.
            hasPrevious = !(book == FIRST_BOOK && target == 1),
            hasNext = !(book == LAST_BOOK && target == lastInBook),
        )
        positionStore.save(book, target)   // remember where we are for next launch
    }

    private companion object {
        const val FIRST_BOOK = 1    // Genesis
        const val LAST_BOOK = 66    // Revelation
    }
}
