package com.trentcowden.bible

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.thelightphone.sdk.InitialScreen
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightBottomBar
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightScrollView
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.designVerticalPxToDp
import com.thelightphone.sdk.ui.designVerticalPxToSp
import com.thelightphone.sdk.ui.gridUnitsAsDp
import com.trentcowden.bible.data.BibleDbHolder
import com.trentcowden.bible.data.ReadingPositionStore
import com.trentcowden.bible.data.Verse

@InitialScreen
class BibleScreen(sealedActivity: SealedLightActivity) :
    LightScreen<Unit, BibleViewModel>(sealedActivity) {

    override val viewModelClass: Class<BibleViewModel>
        get() = BibleViewModel::class.java

    override fun createViewModel() = BibleViewModel(
        db = BibleDbHolder.get(lightContext),
        positionStore = ReadingPositionStore(lightContext.dataStore),
    )

    @Composable
    override fun Content() {
        val state by viewModel.state.collectAsState()
        val themeColors by LightThemeController.colors.collectAsState()

        // Jump back to the top whenever the chapter changes. Keyed on the reference so
        // it fires on next/previous/goTo, but not on unrelated recompositions.
        val scrollState = rememberScrollState()
        LaunchedEffect(state.reference) { scrollState.scrollTo(0) }

        LightTheme(colors = themeColors) {
            val colors = LightThemeTokens.colors
            val paragraphStyle = LightThemeTokens.typography.paragraph.scaledForScreen()
            val verseNumberStyle = SpanStyle(
                fontSize = LightThemeTokens.typography.superfine.fontSize.scaledForScreen(),
                color = colors.contentSecondary,
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.background)
            ) {
                LightScrollView(
                    scrollState = scrollState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        // Pad only the start: LightScrollView already reserves a gutter
                        // on the end for its scroll bar, which should sit at the edge.
                        .padding(start = 24.dp, top = 1f.gridUnitsAsDp())
                ) {
                    val paragraphs = chapterToParagraphs(state.verses, verseNumberStyle)
                    paragraphs.forEachIndexed { index, paragraph ->
                        // Plain Text rather than LightText, which only takes a String:
                        // verse numbers need their own span style. Styled to match
                        // LightTextVariant.Paragraph.
                        Text(
                            text = paragraph,
                            style = paragraphStyle,
                            color = colors.content,
                        )
                        if (index < paragraphs.lastIndex) {
                            Spacer(
                                modifier = Modifier.height(
                                    (PARAGRAPH_FONT_PX * PARAGRAPH_SPACING_MULTIPLIER)
                                        .designVerticalPxToDp()
                                )
                            )
                        }
                    }
                }

                // Chapter navigation: previous / title / next. Arrows hide at the two
                // ends of the Bible; tapping the title opens the table of contents.
                LightBottomBar(
                    items = listOf(
                        if (state.hasPrevious) {
                            LightBarButton.LightIcon(
                                icon = LightIcons.BACK,
                                onClick = { viewModel.previous() },
                            )
                        } else null,
                        LightBarButton.Text(
                            text = state.reference.ifEmpty { "Loading…" },
                            onClick = {
                                navigateTo({ activity -> BookListScreen(activity) }) { ref ->
                                    viewModel.goTo(ref.book, ref.chapter)
                                }
                            },
                        ),
                        if (state.hasNext) {
                            LightBarButton.LightIcon(
                                icon = LightIcons.ARROW_RIGHT,
                                onClick = { viewModel.next() },
                            )
                        } else null,
                    ),
                )
            }
        }
    }
}

private fun chapterToParagraphs(
    verses: List<Verse>,
    verseNumberStyle: SpanStyle,
): List<AnnotatedString> {
    // Each verse's text already carries a trailing space, and "\n\n" marks a paragraph
    // break (which can fall mid-verse). Build the chapter as one styled string, then
    // split it at the breaks so each paragraph keeps its verse-number spans.
    val chapter = buildAnnotatedString {
        verses.forEach { verse ->
            withStyle(verseNumberStyle) { append(verse.verse.toString()) }
            append(" ")
            append(verse.text)
        }
    }

    val paragraphs = mutableListOf<AnnotatedString>()
    var start = 0
    while (start < chapter.length) {
        val end = chapter.text.indexOf("\n\n", start).let { if (it == -1) chapter.length else it }
        paragraphs += chapter.subSequence(start, end).trimmed()
        start = end + 2
    }
    return paragraphs.filter { it.isNotEmpty() }
}

private fun AnnotatedString.trimmed(): AnnotatedString {
    val first = text.indexOfFirst { !it.isWhitespace() }
    if (first == -1) return AnnotatedString("")
    return subSequence(first, text.indexOfLast { !it.isWhitespace() } + 1)
}

// The SDK's typography is in design pixels; LightText scales it to the screen with an
// internal helper. These do the same using the public px→sp conversion.
@Composable
private fun TextUnit.scaledForScreen(): TextUnit =
    if (this == TextUnit.Unspecified) this else value.designVerticalPxToSp()

@Composable
private fun TextStyle.scaledForScreen(): TextStyle = copy(
    fontSize = fontSize.scaledForScreen(),
    lineHeight = lineHeight.scaledForScreen(),
    letterSpacing = letterSpacing.scaledForScreen(),
)

private const val PARAGRAPH_FONT_PX = 24.5f
private const val PARAGRAPH_SPACING_MULTIPLIER = 0.7f
