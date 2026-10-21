package com.apple.musik.playback

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.apple.musik.data.local.SongEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PlaybackManager private constructor(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private var exoPlayer: ExoPlayer? = null
    private var playbackService: PlaybackService? = null

    private val _currentSong = MutableStateFlow<SongEntity?>(null)
    val currentSong: StateFlow<SongEntity?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _totalDuration = MutableStateFlow(0L)
    val totalDuration: StateFlow<Long> = _totalDuration.asStateFlow()

    private val _queue = MutableStateFlow<List<SongEntity>>(emptyList())
    val queue: StateFlow<List<SongEntity>> = _queue.asStateFlow()

    private val _shuffleModeEnabled = MutableStateFlow(false)
    val shuffleModeEnabled: StateFlow<Boolean> = _shuffleModeEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private var currentIndex: Int = -1
    private var originalQueue: List<SongEntity> = emptyList()

    private var progressJob: Job? = null
    private var isSimulatingDemoPlayback = false

    companion object {
        @Volatile
        private var INSTANCE: PlaybackManager? = null

        fun getInstance(context: Context): PlaybackManager {
            return INSTANCE ?: synchronized(this) {
                val instance = PlaybackManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
            if (isPlaying) {
                startProgressTracker()
            } else if (!isSimulatingDemoPlayback) {
                stopProgressTracker()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_READY -> {
                    isSimulatingDemoPlayback = false
                    val dur = exoPlayer?.duration ?: 0L
                    if (dur > 0) {
                        _totalDuration.value = dur
                    }
                }
                Player.STATE_ENDED -> {
                    handleTrackEnded()
                }
                else -> {}
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            Log.w("PlaybackManager", "ExoPlayer playback error, falling back to simulated playback: ${error.message}")
            // For demo URIs or missing local files, simulate playback smoothly
            startSimulatedPlayback()
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _shuffleModeEnabled.value = shuffleModeEnabled
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            _repeatMode.value = repeatMode
        }
    }

    fun attachPlayer(player: ExoPlayer, service: PlaybackService) {
        exoPlayer = player
        playbackService = service
        player.addListener(playerListener)

        // Sync initial state if song is already set
        _currentSong.value?.let { song ->
            prepareAndPlaySong(song, playWhenReady = _isPlaying.value)
        }
    }

    fun detachPlayer() {
        exoPlayer?.removeListener(playerListener)
        exoPlayer = null
        playbackService = null
    }

    private fun ensureServiceRunning() {
        try {
            val intent = Intent(context, PlaybackService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(context, intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            Log.e("PlaybackManager", "Error starting PlaybackService", e)
        }
    }

    fun playSong(song: SongEntity, newQueue: List<SongEntity>? = null) {
        ensureServiceRunning()

        if (newQueue != null && newQueue.isNotEmpty()) {
            originalQueue = newQueue
            _queue.value = if (_shuffleModeEnabled.value) {
                listOf(song) + (newQueue - song).shuffled()
            } else {
                newQueue
            }
        } else if (_queue.value.none { it.id == song.id }) {
            _queue.value = _queue.value + song
        }

        currentIndex = _queue.value.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        _currentSong.value = song
        _currentPosition.value = 0L
        _totalDuration.value = song.duration

        prepareAndPlaySong(song, playWhenReady = true)
    }

    fun playQueue(songs: List<SongEntity>, startIndex: Int = 0) {
        if (songs.isEmpty()) return
        originalQueue = songs
        _queue.value = songs
        val validIndex = startIndex.coerceIn(0, songs.size - 1)
        playSong(songs[validIndex], songs)
    }

    fun shuffleAndPlay(songs: List<SongEntity>) {
        if (songs.isEmpty()) return
        _shuffleModeEnabled.value = true
        exoPlayer?.shuffleModeEnabled = true
        val shuffled = songs.shuffled()
        originalQueue = songs
        _queue.value = shuffled
        playSong(shuffled.first(), shuffled)
    }

    private fun prepareAndPlaySong(song: SongEntity, playWhenReady: Boolean) {
        _currentSong.value = song
        _totalDuration.value = song.duration
        _currentPosition.value = 0L

        val isDemoUri = song.uri.startsWith("content://demo") || song.uri.isBlank()

        if (isDemoUri || exoPlayer == null) {
            startSimulatedPlayback()
            return
        }

        try {
            val metadata = MediaMetadata.Builder()
                .setTitle(song.title)
                .setArtist(song.artist)
                .setAlbumTitle(song.album)
                .setArtworkUri(song.albumArtUri?.let { Uri.parse(it) })
                .build()

            val mediaItem = MediaItem.Builder()
                .setMediaId(song.id.toString())
                .setUri(Uri.parse(song.uri))
                .setMediaMetadata(metadata)
                .build()

            exoPlayer?.apply {
                setMediaItem(mediaItem)
                prepare()
                this.playWhenReady = playWhenReady
            }
            _isPlaying.value = playWhenReady
        } catch (e: Exception) {
            Log.e("PlaybackManager", "Error preparing media item", e)
            startSimulatedPlayback()
        }
    }

    private fun startSimulatedPlayback() {
        isSimulatingDemoPlayback = true
        _isPlaying.value = true
        startProgressTracker()
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive && _isPlaying.value) {
                if (!isSimulatingDemoPlayback && exoPlayer != null) {
                    val pos = exoPlayer?.currentPosition ?: 0L
                    _currentPosition.value = pos
                    val dur = exoPlayer?.duration ?: 0L
                    if (dur > 0) {
                        _totalDuration.value = dur
                    }
                } else {
                    // Demo simulation tick
                    val newPos = _currentPosition.value + 500L
                    val total = _totalDuration.value.takeIf { it > 0 } ?: 200_000L
                    if (newPos >= total) {
                        _currentPosition.value = total
                        handleTrackEnded()
                        break
                    } else {
                        _currentPosition.value = newPos
                    }
                }
                delay(500)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
    }

    private fun handleTrackEnded() {
        when (_repeatMode.value) {
            Player.REPEAT_MODE_ONE -> {
                seekTo(0L)
                resume()
            }
            Player.REPEAT_MODE_ALL -> {
                skipToNext()
            }
            else -> {
                if (currentIndex < _queue.value.size - 1) {
                    skipToNext()
                } else {
                    pause()
                    seekTo(0L)
                }
            }
        }
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            pause()
        } else {
            resume()
        }
    }

    fun pause() {
        _isPlaying.value = false
        exoPlayer?.pause()
        stopProgressTracker()
    }

    fun resume() {
        if (_currentSong.value == null && _queue.value.isNotEmpty()) {
            playSong(_queue.value.first())
            return
        }
        _isPlaying.value = true
        exoPlayer?.play()
        startProgressTracker()
    }

    fun skipToNext() {
        val q = _queue.value
        if (q.isEmpty()) return
        val nextIndex = (currentIndex + 1) % q.size
        currentIndex = nextIndex
        playSong(q[nextIndex], q)
    }

    fun skipToPrevious() {
        val q = _queue.value
        if (q.isEmpty()) return
        // If track played > 3 seconds, restart current track
        if (_currentPosition.value > 3000L) {
            seekTo(0L)
            return
        }
        val prevIndex = if (currentIndex - 1 < 0) q.size - 1 else currentIndex - 1
        currentIndex = prevIndex
        playSong(q[prevIndex], q)
    }

    fun seekTo(positionMs: Long) {
        val clamped = positionMs.coerceIn(0L, _totalDuration.value)
        _currentPosition.value = clamped
        if (!isSimulatingDemoPlayback) {
            exoPlayer?.seekTo(clamped)
        }
    }

    fun toggleShuffle() {
        val newShuffle = !_shuffleModeEnabled.value
        _shuffleModeEnabled.value = newShuffle
        exoPlayer?.shuffleModeEnabled = newShuffle

        val current = _currentSong.value
        if (newShuffle && originalQueue.isNotEmpty()) {
            val reordered = if (current != null) {
                listOf(current) + (originalQueue - current).shuffled()
            } else {
                originalQueue.shuffled()
            }
            _queue.value = reordered
            currentIndex = 0
        } else if (!newShuffle && originalQueue.isNotEmpty()) {
            _queue.value = originalQueue
            currentIndex = current?.let { originalQueue.indexOf(it) } ?: 0
        }
    }

    fun toggleRepeat() {
        val nextMode = when (_repeatMode.value) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        _repeatMode.value = nextMode
        exoPlayer?.repeatMode = nextMode
    }
}
