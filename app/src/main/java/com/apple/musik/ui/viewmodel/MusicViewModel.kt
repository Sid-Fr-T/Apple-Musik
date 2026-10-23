package com.apple.musik.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.apple.musik.data.datastore.SortOption
import com.apple.musik.data.local.AlbumEntity
import com.apple.musik.data.local.ArtistEntity
import com.apple.musik.data.local.SongEntity
import com.apple.musik.data.repository.MusicRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
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

    val artists: StateFlow<List<ArtistEntity>> = repository.artists.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val songCount: StateFlow<Int> = repository.songCount.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val searchSongs: StateFlow<List<SongEntity>> = _searchQuery
        .flatMapLatest { query ->
            val trimmed = query.trim()
            if (trimmed.isBlank()) flowOf(emptyList())
            else repository.searchSongs(trimmed)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val searchAlbums: StateFlow<List<AlbumEntity>> = _searchQuery
        .flatMapLatest { query ->
            val trimmed = query.trim()
            if (trimmed.isBlank()) flowOf(emptyList())
            else repository.searchAlbums(trimmed)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val searchArtists: StateFlow<List<ArtistEntity>> = _searchQuery
        .flatMapLatest { query ->
            val trimmed = query.trim()
            if (trimmed.isBlank()) flowOf(emptyList())
            else repository.searchArtists(trimmed)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun getSongsForAlbum(albumName: String): Flow<List<SongEntity>> {
        return repository.getSongsForAlbum(albumName)
    }

    fun getSongsForArtist(artistName: String): Flow<List<SongEntity>> {
        return repository.getSongsForArtist(artistName)
    }

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
