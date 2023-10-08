package ru.ivanik.ha_vosk.repository

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore("settings")

class DataStoreRepository(val context: Context) {
    suspend fun get(key: String): String? {
        return context.dataStore.data.map {
            Log.i("FLOW", "i")
            return@map it[stringPreferencesKey(key)]
        }.first()
    }

    suspend fun save(key: String, value: String) {
        context.dataStore.edit { settings ->
            settings[stringPreferencesKey(key)] = value
        }
    }

}