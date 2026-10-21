package com.apple.musik.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "user_settings")

enum class SortOption(val title: String, val headerText: String) {
    NAME_ASC("Name (A–Z)", "Sorted by Title"),
    MODIFIED_DATE("Modified Date", "Sorted by Modified Date"),
    DATE_ADDED("Date Added", "Sorted by Date Added")
}

class SortPreferences(private val context: Context) {

    private val SORT_ORDER_KEY = stringPreferencesKey("sort_order")

    val sortOrderFlow: Flow<SortOption> = context.dataStore.data.map { preferences ->
        val savedName = preferences[SORT_ORDER_KEY] ?: SortOption.NAME_ASC.name
        try {
            SortOption.valueOf(savedName)
        } catch (_: Exception) {
            SortOption.NAME_ASC
        }
    }

    suspend fun saveSortOrder(option: SortOption) {
        context.dataStore.edit { preferences ->
            preferences[SORT_ORDER_KEY] = option.name
        }
    }
}
