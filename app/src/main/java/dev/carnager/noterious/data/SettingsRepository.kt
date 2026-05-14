package dev.carnager.noterious.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "noterious_settings")

data class AppSettings(
    val serverUrl: String = "",
    val scopePrefix: String = "",
    val username: String = "",
    val password: String = "",
    val bearerToken: String = "",
    val startupTab: String = "pages",
    val quickTaskPage: String = "",
    val themeId: String = "system",
    val hasCompletedSetup: Boolean = false,
    val hasCompletedDefaultScopePrompt: Boolean = false,
)

class SettingsRepository(private val context: Context) {
    private object Keys {
        val ServerUrl = stringPreferencesKey("server_url")
        val ScopePrefix = stringPreferencesKey("scope_prefix")
        val Username = stringPreferencesKey("username")
        val Password = stringPreferencesKey("password")
        val BearerToken = stringPreferencesKey("bearer_token")
        val StartupTab = stringPreferencesKey("startup_tab")
        val QuickTaskPage = stringPreferencesKey("quick_task_page")
        val ThemeId = stringPreferencesKey("theme_id")
        val SetupComplete = booleanPreferencesKey("setup_complete")
        val DefaultScopePromptComplete = booleanPreferencesKey("default_scope_prompt_complete")
    }

    val settings: Flow<AppSettings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            AppSettings(
                serverUrl = preferences[Keys.ServerUrl].orEmpty(),
                scopePrefix = preferences[Keys.ScopePrefix].orEmpty(),
                username = preferences[Keys.Username].orEmpty(),
                password = preferences[Keys.Password].orEmpty(),
                bearerToken = preferences[Keys.BearerToken].orEmpty(),
                startupTab = normalizeStartupTab(preferences[Keys.StartupTab]),
                quickTaskPage = preferences[Keys.QuickTaskPage].orEmpty(),
                themeId = normalizeThemeId(preferences[Keys.ThemeId]),
                hasCompletedSetup = preferences[Keys.SetupComplete] ?: false,
                hasCompletedDefaultScopePrompt = preferences[Keys.DefaultScopePromptComplete] ?: false,
            )
        }

    suspend fun save(settings: AppSettings) {
        context.dataStore.edit { preferences ->
            preferences[Keys.ServerUrl] = settings.serverUrl.trim()
            preferences[Keys.ScopePrefix] = settings.scopePrefix.trim().trim('/')
            preferences[Keys.Username] = settings.username.trim()
            preferences[Keys.Password] = settings.password
            preferences[Keys.BearerToken] = settings.bearerToken.trim()
            preferences[Keys.StartupTab] = normalizeStartupTab(settings.startupTab)
            preferences[Keys.QuickTaskPage] = settings.quickTaskPage.trim()
            preferences[Keys.ThemeId] = normalizeThemeId(settings.themeId)
            preferences[Keys.SetupComplete] = settings.hasCompletedSetup
            preferences[Keys.DefaultScopePromptComplete] = settings.hasCompletedDefaultScopePrompt
        }
    }

    private fun normalizeStartupTab(value: String?): String {
        return when (value?.trim()?.lowercase()) {
            "tasks" -> "tasks"
            else -> "pages"
        }
    }

    private fun normalizeThemeId(value: String?): String {
        val normalized = value?.trim().orEmpty()
        return if (normalized.isBlank() || normalized.equals("system", ignoreCase = true)) {
            "system"
        } else {
            normalized
        }
    }
}
