package com.trentcowden.tov

/** A place in the Bible, passed back up the navigation stack when the user picks a chapter. */
data class BibleRef(val book: Int, val chapter: Int)
