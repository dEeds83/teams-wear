package de.streamonkey.teamswear.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore by preferencesDataStore("settings")

/** Nutzer-Einstellungen. Aktuell: Push an/aus. */
@Singleton
class SettingsStore @Inject constructor(@ApplicationContext private val context: Context) {

    val pushEnabled: Flow<Boolean> =
        context.settingsDataStore.data.map { it[KEY_PUSH] ?: true }

    suspend fun pushEnabledNow(): Boolean =
        context.settingsDataStore.data.first()[KEY_PUSH] ?: true

    /** Synchron fuer den FCM-Service (BroadcastReceiver-Kontext). */
    fun pushEnabledBlocking(): Boolean = runBlocking { pushEnabledNow() }

    suspend fun setPushEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[KEY_PUSH] = enabled }
    }

    private companion object {
        val KEY_PUSH = booleanPreferencesKey("push_enabled")
    }
}
