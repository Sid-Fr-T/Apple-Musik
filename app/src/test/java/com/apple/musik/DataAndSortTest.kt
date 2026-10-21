package com.apple.musik

import com.apple.musik.data.datastore.SortOption
import com.apple.musik.data.local.SongEntity
import com.apple.musik.ui.components.getArtPaletteFor
import com.apple.musik.ui.screens.formatDuration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class DataAndSortTest {

    @Test
    fun testSortOptions() {
        val options = SortOption.entries
        assertEquals(3, options.size)
        assertEquals("Name (A–Z)", options[0].title)
        assertEquals("Modified Date", options[1].title)
        assertEquals("Date Added", options[2].title)
    }

    @Test
    fun testDurationFormatting() {
        assertEquals("3:42", formatDuration(222_000L))
        assertEquals("4:05", formatDuration(245_000L))
        assertEquals("2:58", formatDuration(178_000L))
        assertEquals("0:00", formatDuration(0L))
    }

    @Test
    fun testSongEntityFields() {
        val song = SongEntity(
            id = 100L,
            title = "Slow Bloom",
            artist = "Aria Vale",
            album = "Slow Bloom",
            duration = 201_000L,
            uri = "content://media/external/audio/media/100",
            albumArtUri = "content://media/external/audio/albumart/10",
            dateAdded = 1700000000L,
            dateModified = 1700000500L
        )

        assertEquals(100L, song.id)
        assertEquals("Slow Bloom", song.title)
        assertEquals("Aria Vale", song.artist)
        assertEquals("Slow Bloom", song.album)
        assertEquals(201_000L, song.duration)
        assertEquals("content://media/external/audio/media/100", song.uri)
        assertEquals("content://media/external/audio/albumart/10", song.albumArtUri)
        assertEquals(1700000000L, song.dateAdded)
        assertEquals(1700000500L, song.dateModified)
    }

    @Test
    fun testArtPaletteDeterminism() {
        val palette1 = getArtPaletteFor("Slow Bloom")
        val palette2 = getArtPaletteFor("Slow Bloom")
        assertEquals(palette1, palette2)
        assertNotNull(palette1.first)
        assertNotNull(palette1.second)
    }
}
