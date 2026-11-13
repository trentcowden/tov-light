package com.trentcowden.tov

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import com.thelightphone.sdk.InitialScreen
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightBottomBar
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightScrollView
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.designVerticalPxToSp
import com.thelightphone.sdk.ui.gridUnitsAsDp
import com.trentcowden.tov.data.BibleDbHolder
import com.trentcowden.tov.data.ReadingPositionStore
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

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

        val scrollState = rememberScrollState()
        RestoreAndTrackScroll(state.ref, scrollState, viewModel)

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
                        .padding(start = 1f.gridUnitsAsDp(), top = 1f.gridUnitsAsDp())
                ) {
                    // A Psalm title, in the quieter secondary color like the verse numbers.
                    state.title?.let { title ->
                        LightText(
                            text = title,
                            variant = LightTextVariant.Paragraph,
                            lighten = true,
                        )
                        Spacer(modifier = Modifier.height(PARAGRAPH_GAP_UNITS.gridUnitsAsDp()))
                    }

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
                            Spacer(modifier = Modifier.height(PARAGRAPH_GAP_UNITS.gridUnitsAsDp()))
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

/**
 * Whenever a chapter is shown, scroll to where the reader left off in it (the top, for
 * a newly opened chapter), then keep reporting the position so it can be saved.
 * Keyed on [ref], so moving to another chapter cancels the old tracking and restarts.
 */
@OptIn(FlowPreview::class)
@Composable
private fun RestoreAndTrackScroll(
    ref: BibleRef?,
    scrollState: ScrollState,
    viewModel: BibleViewModel,
) {
    LaunchedEffect(ref) {
        if (ref == null) return@LaunchedEffect
        val target = viewModel.scrollPx
        if (target > 0) {
            // scrollTo clamps to the current content height, so wait for the chapter
            // to be laid out first. Give up after a moment if it never gets that tall
            // (e.g. the layout changed), and settle for as far as it goes.
            withTimeoutOrNull(RESTORE_TIMEOUT_MS) {
                snapshotFlow { scrollState.maxValue }.first { it >= target }
            }
        }
        scrollState.scrollTo(target)
        snapshotFlow { scrollState.value }
            .debounce(SCROLL_SAVE_DEBOUNCE_MS)
            .collect { viewModel.onScrolled(ref, it) }
    }
}

private const val RESTORE_TIMEOUT_MS = 1_000L
private const val SCROLL_SAVE_DEBOUNCE_MS = 400L

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

private const val PARAGRAPH_GAP_UNITS = 0.75f
