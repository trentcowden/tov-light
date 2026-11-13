package com.trentcowden.bible.data

import com.thelightphone.sdk.SealedLightContext
import com.thelightphone.sdk.buildDatabase

/**
 * Holds a single shared [BibleDb]. Room instances are meant to be singletons, and
 * several screens now read the Bible, so we build it once (seeding from the prebuilt
 * bible.db in assets on first launch) and hand the same instance to every ViewModel.
 *
 * Destructive migration makes Room re-copy the asset when [BibleDb.VERSION] changes,
 * which is how updated Bible text reaches existing installs. See [BibleDb].
 */
object BibleDbHolder {
    @Volatile
    private var instance: BibleDb? = null

    fun get(context: SealedLightContext): BibleDb =
        instance ?: synchronized(this) {
            instance ?: context
                .buildDatabase(BibleDb::class.java, DB_NAME) {
                    createFromAsset(DB_NAME).fallbackToDestructiveMigration(dropAllTables = true)
                }
                .also { instance = it }
        }

    private const val DB_NAME = "bible.db"
}
