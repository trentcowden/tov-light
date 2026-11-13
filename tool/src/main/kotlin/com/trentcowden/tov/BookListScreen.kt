package com.trentcowden.tov

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.trentcowden.tov.data.BibleDbHolder
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightScrollView
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.gridUnitsAsDp
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.lightClickable

class BookListScreen(sealedActivity: SealedLightActivity) :
    LightScreen<BibleRef, BookListViewModel>(sealedActivity) {

    override val viewModelClass: Class<BookListViewModel>
        get() = BookListViewModel::class.java

    override fun createViewModel() = BookListViewModel(BibleDbHolder.get(lightContext))

    @Composable
    override fun Content() {
        val books by viewModel.books.collectAsState()
        val themeColors by LightThemeController.colors.collectAsState()
        // Starts where the list was when a book was opened (the top, the first time).
        val scrollState = rememberScrollState(initial = viewModel.scrollPx)

        LightTheme(colors = themeColors) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background)
            ) {
                LightTopBar(
                    leftButton = LightBarButton.LightIcon(
                        icon = LightIcons.BACK,
                        onClick = { goBack() },
                    ),
                    center = LightTopBarCenter.Text("Books"),
                )

                LightScrollView(
                    scrollState = scrollState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(start = 1f.gridUnitsAsDp())
                        .padding(bottom = 1f.gridUnitsAsDp()),
                ) {
                    books.forEach { book ->
                        LightText(
                            text = book.bookName,
                            variant = LightTextVariant.Copy,
                            modifier = Modifier
                                .fillMaxWidth()
                                .lightClickable {
                                    viewModel.scrollPx = scrollState.value
                                    // Push the chapter list; when it returns a chapter,
                                    // go back again with the full ref so BibleScreen loads it.
                                    navigateTo(
                                        { activity ->
                                            ChapterListScreen(activity, book.bookName, book.chapterCount)
                                        },
                                    ) { chapter ->
                                        goBack(BibleRef(book.book, chapter))
                                    }
                                }
                                .padding(vertical = 0.75f.gridUnitsAsDp()),
                        )
                    }
                }
            }
        }
    }
}
