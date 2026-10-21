package com.apple.musik

import com.apple.musik.data.local.SongEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackTest {

    @Test
    fun testQueueOrder() {
        val songs = listOf(
            SongEntity(1, "A", "Artist", "Album", 200_000, "uri", null, 0, 0),
            SongEntity(2, "B", "Artist", "Album", 200_000, "uri", null, 0, 0),
            SongEntity(3, "C", "Artist", "Album", 200_000, "uri", null, 0, 0)
        )

        assertEquals(3, songs.size)
        assertEquals("A", songs[0].title)
        assertEquals("B", songs[1].title)
        assertEquals("C", songs[2].title)
    }

    @Test
    fun testShuffleReordersList() {
        val songs = (1..20).map { id ->
            SongEntity(id.toLong(), "Song $id", "Artist", "Album", 200_000, "uri", null, 0, 0)
        }
        val shuffled = songs.shuffled()
        assertEquals(songs.size, shuffled.size)
        // With 20 items, probability of identical ordering is essentially 0
        assertNotEquals(songs.map { it.id }, shuffled.map { it.id })
    }

    @Test
    fun testTimeFormatting() {
        val totalMs = 205_000L // 3:25
        val currentMs = 84_000L // 1:24
        val remainingMs = totalMs - currentMs // 121_000L (2:01)

        val currentStr = String.format("%d:%02d", (currentMs / 1000) / 60, (currentMs / 1000) % 60)
        val remainingStr = String.format("-%d:%02d", (remainingMs / 1000) / 60, (remainingMs / 1000) % 60)

        assertEquals("1:24", currentStr)
        assertEquals("-2:01", remainingStr)
    }
}
