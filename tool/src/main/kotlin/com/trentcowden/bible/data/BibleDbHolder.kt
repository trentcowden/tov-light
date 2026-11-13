package com.trentcowden.bible.data

import com.thelightphone.sdk.SealedLightContext
import com.thelightphone.sdk.buildDatabase

/**
 * Holds a single shared [BibleDb]. Room instances are meant to be singletons, and
 * several screens now read the Bible, so we build it once (seeding from the prebuilt
 * bible.db in assets on first launch) and hand the same instance to every ViewModel.
 */
object BibleDbHolder {
    @Volatile
    private var instance: BibleDb? = null

    fun get(context: SealedLightContext): BibleDb =
        instance ?: synchronized(this) {
            instance ?: context
                .buildDatabase(BibleDb::class.java, "bible.db") { createFromAsset("bible.db") }
                .also { instance = it }
        }
}
