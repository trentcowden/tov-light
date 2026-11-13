package com.trentcowden.bible.data

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * The Room database, seeded read-only from the prebuilt bible.db in assets.
 *
 * To ship updated Bible text: rebuild bible.db with scripts/build_bible_db.py and bump
 * [VERSION]. The script reads [VERSION] from this file and stamps it into the .db as
 * `PRAGMA user_version`, so the two can't drift (BibleDbAssetTest checks this too).
 * On next launch Room sees the version change and, because [BibleDbHolder] allows
 * destructive migration, replaces the on-device copy with the new asset. Nothing the
 * user owns lives in this database (reading position is in DataStore), so that's safe.
 *
 * exportSchema is off because there are no migrations to track.
 */
@Database(entities = [Verse::class, ChapterTitle::class], version = BibleDb.VERSION, exportSchema = false)
abstract class BibleDb : RoomDatabase() {
    abstract fun verseDao(): VerseDao

    companion object {
        const val VERSION = 3
    }
}
