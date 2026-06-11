package com.sfedu.schedule.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sfedu.schedule.model.ScheduleAppData
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.scheduleDataStore by preferencesDataStore(name = "schedule_data")

class ScheduleRepository(
    private val context: Context
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val appDataJsonKey = stringPreferencesKey("app_data_json")

    val appDataFlow: Flow<ScheduleAppData> = context.scheduleDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val savedJson = preferences[appDataJsonKey]

            if (savedJson.isNullOrBlank()) {
                ScheduleAppData.default()
            } else {
                runCatching {
                    json.decodeFromString<ScheduleAppData>(savedJson)
                }.getOrElse {
                    ScheduleAppData.default()
                }
            }
        }

    suspend fun saveAppData(appData: ScheduleAppData) {
        context.scheduleDataStore.edit { preferences ->
            preferences[appDataJsonKey] = json.encodeToString(appData)
        }
    }
}