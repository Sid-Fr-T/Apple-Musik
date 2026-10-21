package com.apple.musik.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.apple.musik.data.datastore.SortOption
import com.apple.musik.data.local.AlbumEntity
import com.apple.musik.data.local.SongEntity
import com.apple.musik.data.repository.MusicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MusicRepository(application)

    val sortOption: StateFlow<SortOption> = repository.sortOption.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SortOption.NAME_ASC
    )

    val songs: StateFlow<List<SongEntity>> = repository.songs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val recentlyAdded: StateFlow<List<SongEntity>> = repository.recentlyAddedSongs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val recentlyPlayed: StateFlow<List<SongEntity>> = repository.recentlyPlayedSongs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val albums: StateFlow<List<AlbumEntity>> = repository.albums.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val songCount: StateFlow<Int> = repository.songCount.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _hasStoragePermission = MutableStateFlow(false)
    val hasStoragePermission: StateFlow<Boolean> = _hasStoragePermission.asStateFlow()

    fun updatePermissionStatus(granted: Boolean) {
        _hasStoragePermission.value = granted
        if (granted) {
            scanLocalMedia()
        }
    }

    fun scanLocalMedia() {
        viewModelScope.launch {
            _isScanning.value = true
            try {
                repository.scanMedia()
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun setSortOption(option: SortOption) {
        viewModelScope.launch {
            repository.setSortOption(option)
        }
    }
}
