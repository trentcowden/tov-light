package com.trentcowden.tov

import androidx.lifecycle.viewModelScope
import com.trentcowden.tov.data.BibleDb
import com.trentcowden.tov.data.BookInfo
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SimpleLightScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class BookListViewModel(private val db: BibleDb) : LightViewModel<BibleRef>() {

    private val _books = MutableStateFlow<List<BookInfo>>(emptyList())
    val books: StateFlow<List<BookInfo>> = _books

    override fun onScreenShow(screen: SimpleLightScreen<BibleRef>) {
        super.onScreenShow(screen)
        if (_books.value.isEmpty()) {
            viewModelScope.launch { _books.value = db.verseDao().books() }
        }
    }
}
