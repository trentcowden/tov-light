package com.trentcowden.bible.data

import java.io.File
import java.io.RandomAccessFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Guards the bundled bible.db against drifting from [BibleDb.VERSION]. If they differ,
 * Room treats the asset as needing a migration. Fix by rerunning
 * scripts/build_bible_db.py, which stamps the current VERSION into the file.
 */
class BibleDbAssetTest {

    // Unit tests run with the module directory (tool/) as the working directory.
    private val asset = File("src/main/assets/bible.db")

    @Test fun `asset is a SQLite database`() {
        val magic = ByteArray(16).also { RandomAccessFile(asset, "r").use { f -> f.readFully(it) } }
        assertTrue(String(magic, Charsets.US_ASCII).startsWith("SQLite format 3"))
    }

    @Test fun `asset user_version matches BibleDb VERSION`() {
        // SQLite stores user_version as a big-endian Int at byte offset 60 of the header.
        val userVersion = RandomAccessFile(asset, "r").use { f -> f.seek(60); f.readInt() }
        assertEquals(BibleDb.VERSION, userVersion, "Rerun scripts/build_bible_db.py")
    }
}
