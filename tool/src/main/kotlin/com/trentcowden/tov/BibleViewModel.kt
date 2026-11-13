package com.trentcowden.tov

import androidx.lifecycle.viewModelScope
import com.trentcowden.tov.data.BibleDb
import com.trentcowden.tov.data.ReadingPositionStore
import com.trentcowden.tov.data.Verse
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
        val ref: BibleRef? = null,           // null until the first chapter loads
        val reference: String = "",          // e.g. "John 3"
        val title: String? = null,           // a Psalm title, shown above verse 1
        val verses: List<Verse> = emptyList(),
        val hasPrevious: Boolean = false,
        val hasNext: Boolean = false,
    )

    private val _state = MutableStateFlow(ChapterState())
    val state: StateFlow<ChapterState> = _state

    // Where we currently are. Kept private — the screen only reads _state.
    // Seeded from the saved position on first show.
    private var ref = BibleRef(ChapterNav.FIRST_BOOK, 1)

    /**
     * How far down the current chapter the reader is, in pixels. The screen restores
     * this whenever it (re)draws a chapter, so it survives both relaunching the tool
     * and a trip to the table of contents and back.
     */
    var scrollPx = 0
        private set

    override fun onScreenShow(screen: SimpleLightScreen<Unit>) {
        super.onScreenShow(screen)
        // Only load on first show. On return from the book/chapter picker the state
        // is already populated, and the picker drives the update via goTo().
        if (_state.value.verses.isEmpty()) viewModelScope.launch {
            val saved = positionStore.load()
            load(saved.ref, scrollPx = saved.scrollPx)
        }
    }

    /** Jump to a chapter chosen from the picker. Picking the current one keeps your place. */
    fun goTo(book: Int, chapter: Int) = viewModelScope.launch {
        val target = BibleRef(book, chapter)
        if (target != ref) load(target)
    }

    /** Advance one chapter, rolling into the next book at a book's end. No-op at Revelation 22. */
    fun next() = viewModelScope.launch {
        ChapterNav.next(ref, ::chapterCount)?.let { load(it) }
    }

    /** Go back one chapter, rolling into the previous book at a book's start. No-op at Genesis 1. */
    fun previous() = viewModelScope.launch {
        ChapterNav.previous(ref, ::chapterCount)?.let { load(it) }
    }

    /**
     * The screen reports scrolling here (debounced). [shownRef] is the chapter that was
     * on screen, so a late report from a chapter we've since left is ignored rather
     * than saved against the new one.
     */
    fun onScrolled(shownRef: BibleRef, px: Int) {
        if (shownRef != ref || px == scrollPx) return
        scrollPx = px
        viewModelScope.launch { positionStore.save(ref, px) }
    }

    private suspend fun chapterCount(book: Int) = db.verseDao().chapterCount(book)

    // Room suspend queries are main-safe, so no Dispatchers.IO needed.
    private suspend fun load(requested: BibleRef, scrollPx: Int = 0) {
        val lastInBook = chapterCount(requested.book)
        val target = requested.copy(chapter = requested.chapter.coerceIn(1, lastInBook))
        val verses = db.verseDao().chapter(target.book, target.chapter)
        val title = db.verseDao().chapterTitle(target.book, target.chapter)

        ref = target
        // A saved offset only applies to the chapter it was saved for.
        this.scrollPx = if (target == requested) scrollPx else 0
        _state.value = ChapterState(
            ref = target,
            reference = verses.firstOrNull()?.let { "${it.bookName} ${it.chapter}" } ?: "",
            title = title,
            verses = verses,
            // Chevrons hide only at the two ends of the whole Bible.
            hasPrevious = !ChapterNav.isFirst(target),
            hasNext = !ChapterNav.isLast(target, lastInBook),
        )
        positionStore.save(target, this.scrollPx)   // remember where we are for next launch
    }
}
