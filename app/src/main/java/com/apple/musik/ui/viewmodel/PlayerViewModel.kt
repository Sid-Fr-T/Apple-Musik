package com.apple.musik.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.apple.musik.data.local.SongEntity
import com.apple.musik.playback.PlaybackManager
import kotlinx.coroutines.flow.StateFlow

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val playbackManager = PlaybackManager.getInstance(application)

    val currentSong: StateFlow<SongEntity?> = playbackManager.currentSong
    val isPlaying: StateFlow<Boolean> = playbackManager.isPlaying
    val position: StateFlow<Long> = playbackManager.currentPosition
    val duration: StateFlow<Long> = playbackManager.totalDuration
    val queue: StateFlow<List<SongEntity>> = playbackManager.queue
    val shuffleModeEnabled: StateFlow<Boolean> = playbackManager.shuffleModeEnabled
    val repeatMode: StateFlow<Int> = playbackManager.repeatMode

    fun playSong(song: SongEntity, newQueue: List<SongEntity>? = null) {
        playbackManager.playSong(song, newQueue)
    }

    fun playAll(songs: List<SongEntity>) {
        playbackManager.playQueue(songs, startIndex = 0)
    }

    fun shuffle(songs: List<SongEntity>) {
        playbackManager.shuffleAndPlay(songs)
    }

    fun togglePlayPause() {
        playbackManager.togglePlayPause()
    }

    fun skipToNext() {
        playbackManager.skipToNext()
    }

    fun skipToPrevious() {
        playbackManager.skipToPrevious()
    }

    fun seekTo(positionMs: Long) {
        playbackManager.seekTo(positionMs)
    }

    fun toggleShuffle() {
        playbackManager.toggleShuffle()
    }

    fun toggleRepeat() {
        playbackManager.toggleRepeat()
    }
}
