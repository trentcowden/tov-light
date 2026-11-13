# Tov for Light Phone

Tov is a minimalist Bible reader for the Light Phone, based on Tov, my minimalist Bible app for iOS. It's built with the [Light SDK](https://github.com/lightphone/light-sdk).

## Features

- Uses the Berean Standard Bible (BSB), a reliable, readable translation in the public domain
- Works fully offline: the whole Bible ships with the tool in a speedy SQLite database, and the tool requests no permissions
- No indents and nice paragraph spacing for easy reading
- Navigate chapters with the left and right arrows in the bottom bar
- Tap the chapter title to open the table of contents and jump to any book and chapter
- Picks up right where you left off, down to your place in the chapter

## Screenshots

<img width="1080" height="1240" alt="Screenshot_20260718_174412" src="https://github.com/user-attachments/assets/e58c6598-7b43-4132-8124-5db1b517ff16" />
<img width="1080" height="1240" alt="Screenshot_20260718_174425" src="https://github.com/user-attachments/assets/87df02f7-99a3-4e88-acbc-ecaf6737d99d" />
<img width="1080" height="1240" alt="Screenshot_20260718_174434" src="https://github.com/user-attachments/assets/2567da70-8aca-48cc-8762-869e2918841a" />
<img width="1080" height="1240" alt="Screenshot_20260718_174447" src="https://github.com/user-attachments/assets/0f5f58cb-9691-40c3-8cd9-9faf54832dc0" />

## Bible text

The Holy Bible, Berean Standard Bible, BSB is produced in cooperation with Bible Hub, Discovery Bible, OpenBible.com, and the Berean Bible Translation Committee. This text of God's Word has been dedicated to the public domain.

The BSB was [dedicated to the public domain](https://berean.bible/terms.htm) on April 30, 2023. The text here comes from the BSB's USJ download (files dated March 8, 2026). Psalm titles are shown above verse 1 in a quieter color. Section headings, cross references, and footnotes are left out, and a few export formatting quirks are cleaned up (see [`scripts/usj_to_chapters.py`](scripts/usj_to_chapters.py)); the wording is otherwise verbatim.

## Development

The tool lives in [`tool/`](tool). Everything else in this repo is the [Light SDK](https://github.com/lightphone/light-sdk), merged in from upstream unchanged. Keep it that way: Light's build server compiles only `tool/` against its own pinned copy of the SDK, so edits to the SDK modules won't be in the build Light signs.

Run the unit tests with:

```
./gradlew :tool:testDebugUnitTest
```

### Updating the Bible text

The text flows from the BSB's USJ download, to [`chapters.json`](chapters.json), to the prebuilt database at `tool/src/main/assets/bible.db`. To ship a change to the text:

1. Download the BSB in USJ format from [berean.bible](https://berean.bible/downloads.htm), unzip it outside the repo, and run `python3 scripts/usj_to_chapters.py path/to/bsb_usj`. This rewrites `chapters.json`. Review its diff before going on.
2. Bump `VERSION` in [`BibleDb.kt`](tool/src/main/kotlin/com/trentcowden/tov/data/BibleDb.kt).
3. Run `python3 scripts/build_bible_db.py`. It reads `VERSION` from `BibleDb.kt` and stamps it into the database.

On next launch, existing installs see the new version and replace their copy of the database. Reading position is stored separately, so it isn't affected. A unit test fails if the database and `VERSION` ever disagree.
