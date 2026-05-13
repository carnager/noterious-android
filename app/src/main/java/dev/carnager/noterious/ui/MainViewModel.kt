package dev.carnager.noterious.ui

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.carnager.noterious.data.AppSettings
import dev.carnager.noterious.data.NoteriousRepository
import dev.carnager.noterious.data.SettingsRepository
import dev.carnager.noterious.model.ApiPageDetail
import dev.carnager.noterious.model.ApiTaskItem
import dev.carnager.noterious.model.ApiPageSummary
import dev.carnager.noterious.model.DerivedPageResponse
import dev.carnager.noterious.model.DocumentRecord
import dev.carnager.noterious.model.PageRevisionRecord
import dev.carnager.noterious.model.QueryCopilotResponse
import dev.carnager.noterious.model.QueryWorkbenchResult
import dev.carnager.noterious.model.SavedQueryRecord
import dev.carnager.noterious.model.SearchResponse
import dev.carnager.noterious.model.ServerMetaResponse
import dev.carnager.noterious.model.ServerSettingsResponse
import dev.carnager.noterious.model.TaskItem
import dev.carnager.noterious.model.ThemeRecord
import dev.carnager.noterious.model.TodaySnapshot
import dev.carnager.noterious.model.TrashPageRecord
import dev.carnager.noterious.model.UserNotificationSettings
import dev.carnager.noterious.model.UserSettingsPayload
import dev.carnager.noterious.model.VaultRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

data class MainUiState(
    val settingsLoaded: Boolean = false,
    val settings: AppSettings = AppSettings(),
    val vaults: List<VaultRecord> = emptyList(),
    val isVaultsLoading: Boolean = false,
    val pages: List<ApiPageSummary> = emptyList(),
    val templatePages: List<ApiPageSummary> = emptyList(),
    val folders: List<String> = emptyList(),
    val documents: List<DocumentRecord> = emptyList(),
    val tasks: List<TaskItem> = emptyList(),
    val today: TodaySnapshot = TodaySnapshot(),
    val openPagePath: String? = null,
    val openPageContent: String? = null,
    val openPageFrontmatter: JsonObject? = null,
    val openPageTasks: List<ApiTaskItem> = emptyList(),
    val openPageDerived: DerivedPageResponse? = null,
    val openPageHistory: List<PageRevisionRecord> = emptyList(),
    val isPageLoading: Boolean = false,
    val isPageSaving: Boolean = false,
    val isPageHistoryLoading: Boolean = false,
    val isPageHistoryBusy: Boolean = false,
    val trashPages: List<TrashPageRecord> = emptyList(),
    val isTrashLoading: Boolean = false,
    val isTrashBusy: Boolean = false,
    val isDocumentsLoading: Boolean = false,
    val isDocumentsBusy: Boolean = false,
    val queryWorkbench: QueryWorkbenchResult? = null,
    val lastExecutedQuery: String = "",
    val isQueryWorkbenchLoading: Boolean = false,
    val themes: List<ThemeRecord> = emptyList(),
    val isThemesLoading: Boolean = false,
    val isThemeBusy: Boolean = false,
    val serverSettings: ServerSettingsResponse? = null,
    val userSettings: UserSettingsPayload = UserSettingsPayload(),
    val serverMeta: ServerMetaResponse? = null,
    val isSettingsDetailsLoading: Boolean = false,
    val isUserSettingsSaving: Boolean = false,
    val searchResults: SearchResponse? = null,
    val isSearching: Boolean = false,
    val isLoading: Boolean = false,
    val isLiveSyncConnected: Boolean = false,
    val liveSyncError: String? = null,
    val error: String? = null,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = NoteriousRepository()
    private val settingsRepository = SettingsRepository(application)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var settingsLoaded = false
    private var appInForeground = false
    private var eventStreamJob: Job? = null
    private var pendingLiveRefreshJob: Job? = null
    private var activeEventStreamKey: String? = null
    private var openPageJob: Job? = null
    private var queryWorkbenchJob: Job? = null
    private var templatePagesJob: Job? = null
    private var templatePagesCacheKey: String? = null
    private var themeLibraryCacheKey: String? = null
    private var settingsDetailsCacheKey: String? = null
    private val openPageBackStack = ArrayDeque<String>()
    private var pendingDeepLinkPagePath: String? = null

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                settingsLoaded = true
                val nextTemplatePagesCacheKey = templatePagesInventoryKey(settings)
                if (templatePagesCacheKey != null && templatePagesCacheKey != nextTemplatePagesCacheKey) {
                    templatePagesCacheKey = null
                    _uiState.update { it.copy(templatePages = emptyList()) }
                }
                val nextThemeLibraryCacheKey = themeInventoryKey(settings)
                if (themeLibraryCacheKey != null && themeLibraryCacheKey != nextThemeLibraryCacheKey) {
                    themeLibraryCacheKey = null
                    _uiState.update { it.copy(themes = emptyList()) }
                }
                val nextSettingsDetailsCacheKey = settingsDetailsInventoryKey(settings)
                if (settingsDetailsCacheKey != null && settingsDetailsCacheKey != nextSettingsDetailsCacheKey) {
                    settingsDetailsCacheKey = null
                    _uiState.update {
                        it.copy(
                            serverSettings = null,
                            userSettings = UserSettingsPayload(),
                            serverMeta = null,
                        )
                    }
                }
                _uiState.update { it.copy(settingsLoaded = true, settings = settings) }
                reconcileEventStream()
                consumePendingDeepLinkIfPossible()
                if (appInForeground) {
                    loadForegroundDataIfNeeded(trigger = "settings-loaded")
                }
            }
        }
    }

    private fun loadForegroundDataIfNeeded(trigger: String) {
        val state = _uiState.value
        if (state.settings.serverUrl.isBlank()) return
        if (!canAttemptBackgroundSync(state.settings)) return
        if (state.pages.isEmpty() || state.folders.isEmpty()) {
            refresh(trigger = trigger)
        }
        if (state.vaults.isEmpty()) {
            fetchVaults()
        }
        if (state.themes.isEmpty()) {
            ensureThemeLibraryLoaded()
        }
    }

    fun refresh(trigger: String = "manual") {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            if (settings.serverUrl.isBlank()) {
                _uiState.update { it.copy(error = "Configure the server URL first.") }
                return@launch
            }
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching {
                fetchCollectionsSnapshot(settings)
            }.onSuccess { result ->
                _uiState.update { current ->
                    current.copy(
                        pages = result.pages ?: current.pages,
                        folders = result.folders ?: current.folders,
                        tasks = result.tasks ?: current.tasks,
                        today = result.today ?: current.today,
                        isLoading = false,
                        error = null,
                    )
                }
                if (!settings.hasCompletedSetup) {
                    val completedSettings = settings.copy(hasCompletedSetup = true)
                    settingsRepository.save(completedSettings)
                    _uiState.update { current -> current.copy(settings = completedSettings) }
                }
                reloadOpenPageIfNeeded()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(isLoading = false, error = error.message ?: "Sync failed.")
                }
            }
        }
    }

    fun fetchVaults() {
        if (_uiState.value.isVaultsLoading) return
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            if (settings.serverUrl.isBlank()) return@launch
            _uiState.update { it.copy(isVaultsLoading = true) }
            runCatching {
                repository.fetchVaults(
                    url = settings.serverUrl,
                    bearerToken = settings.bearerToken,
                    username = settings.username,
                    password = settings.password,
                )
            }.onSuccess { vaults ->
                _uiState.update { it.copy(vaults = vaults, isVaultsLoading = false) }
            }.onFailure { error ->
                if (error is CancellationException) return@onFailure
                _uiState.update {
                    it.copy(
                        isVaultsLoading = false,
                        error = error.message ?: "Scopes could not be loaded.",
                    )
                }
            }
        }
    }

    fun selectVault(vault: VaultRecord) {
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            val scopePrefix = scopePrefixForVault(vault)
            settingsRepository.save(
                settings.copy(
                    scopePrefix = scopePrefix,
                    hasCompletedDefaultScopePrompt = true,
                ),
            )
            refresh(trigger = "vault-switch")
        }
    }

    fun completeDefaultScopePrompt() {
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            if (settings.hasCompletedDefaultScopePrompt) {
                return@launch
            }
            settingsRepository.save(settings.copy(hasCompletedDefaultScopePrompt = true))
        }
    }

    fun createVault(
        name: String,
        onResult: (VaultRecord?) -> Unit = {},
    ) {
        val settings = _uiState.value.settings
        val normalizedName = name.trim()
        if (settings.serverUrl.isBlank() || normalizedName.isBlank()) {
            onResult(null)
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(error = null, isLoading = true) }
            runCatching {
                val createdVault = repository.createVault(
                    url = settings.serverUrl,
                    name = normalizedName,
                    bearerToken = settings.bearerToken,
                    username = settings.username,
                    password = settings.password,
                )
                val nextScopePrefix = scopePrefixForVault(createdVault)
                val nextSettings = settings.copy(scopePrefix = nextScopePrefix)
                settingsRepository.save(nextSettings)
                VaultMutationResult(
                    vault = createdVault,
                    settings = nextSettings,
                    vaults = fetchVaultsSnapshot(nextSettings),
                    collections = fetchCollectionsSnapshot(nextSettings),
                )
            }.onSuccess { result ->
                openPageBackStack.clear()
                _uiState.update { current ->
                    current.copy(
                        settings = current.settings.copy(scopePrefix = result.settings.scopePrefix),
                        vaults = result.vaults ?: current.vaults,
                        pages = result.collections.pages ?: current.pages,
                        folders = result.collections.folders ?: current.folders,
                        tasks = result.collections.tasks ?: current.tasks,
                        today = result.collections.today ?: current.today,
                        openPagePath = null,
                        openPageContent = null,
                        openPageFrontmatter = null,
                        openPageTasks = emptyList(),
                        openPageDerived = null,
                        openPageHistory = emptyList(),
                        isPageLoading = false,
                        isPageSaving = false,
                        isPageHistoryLoading = false,
                        isPageHistoryBusy = false,
                        isLoading = false,
                    )
                }
                onResult(result.vault)
            }.onFailure { error ->
                if (error is CancellationException) return@onFailure
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = error.message ?: "Vault could not be created.",
                    )
                }
                onResult(null)
            }
        }
    }

    fun renameVault(
        vault: VaultRecord,
        nextName: String,
        onResult: (VaultRecord?) -> Unit = {},
    ) {
        val settings = _uiState.value.settings
        val normalizedName = nextName.trim()
        if (settings.serverUrl.isBlank() || vault.id <= 0L || normalizedName.isBlank()) {
            onResult(null)
            return
        }
        val previousScopePrefix = scopePrefixForVault(vault)
        val activeSearchQuery = _uiState.value.searchResults?.query?.takeIf(String::isNotBlank)

        viewModelScope.launch {
            _uiState.update { it.copy(error = null, isLoading = true) }
            runCatching {
                val updatedVault = repository.renameVault(
                    url = settings.serverUrl,
                    vaultId = vault.id,
                    name = normalizedName,
                    bearerToken = settings.bearerToken,
                    username = settings.username,
                    password = settings.password,
                )
                val nextScopePrefix = scopePrefixForVault(updatedVault)
                val currentScopeMatches = normalizeScopePrefix(settings.scopePrefix) ==
                    normalizeScopePrefix(previousScopePrefix)
                val nextSettings = if (currentScopeMatches) {
                    settings.copy(scopePrefix = nextScopePrefix)
                } else {
                    settings
                }
                if (nextSettings != settings) {
                    settingsRepository.save(nextSettings)
                }
                VaultMutationResult(
                    vault = updatedVault,
                    settings = nextSettings,
                    vaults = fetchVaultsSnapshot(nextSettings),
                    collections = fetchCollectionsSnapshot(nextSettings),
                )
            }.onSuccess { result ->
                val nextScopePrefix = scopePrefixForVault(result.vault)
                val nextOpenPagePath = _uiState.value.openPagePath?.let { currentOpenPagePath ->
                    remapPagePathInsideFolder(
                        pagePath = currentOpenPagePath,
                        fromFolderPath = previousScopePrefix,
                        toFolderPath = nextScopePrefix,
                    )
                }
                remapFolderReferencesInBackStack(
                    fromFolderPath = previousScopePrefix,
                    toFolderPath = nextScopePrefix,
                )
                _uiState.update { current ->
                    current.copy(
                        settings = if (
                            normalizeScopePrefix(current.settings.scopePrefix) ==
                            normalizeScopePrefix(previousScopePrefix)
                        ) {
                            current.settings.copy(scopePrefix = nextScopePrefix)
                        } else {
                            current.settings
                        },
                        vaults = result.vaults ?: current.vaults,
                        pages = result.collections.pages ?: current.pages,
                        folders = result.collections.folders ?: current.folders,
                        tasks = result.collections.tasks ?: current.tasks,
                        today = result.collections.today ?: current.today,
                        isLoading = false,
                    )
                }
                if (nextOpenPagePath != null) {
                    openPage(nextOpenPagePath, addToBackStack = false)
                }
                activeSearchQuery?.let(::search)
                onResult(result.vault)
            }.onFailure { error ->
                if (error is CancellationException) return@onFailure
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = error.message ?: "Vault could not be renamed.",
                    )
                }
                onResult(null)
            }
        }
    }

    fun saveSettings(
        serverUrl: String,
        scopePrefix: String,
        username: String,
        password: String,
        bearerToken: String,
        startupTab: String,
    ) {
        viewModelScope.launch {
            templatePagesCacheKey = null
            settingsDetailsCacheKey = null
            _uiState.update {
                it.copy(
                    templatePages = emptyList(),
                    serverSettings = null,
                    userSettings = UserSettingsPayload(),
                    serverMeta = null,
                )
            }
            settingsRepository.save(
                AppSettings(
                    serverUrl = serverUrl,
                    scopePrefix = scopePrefix,
                    username = username,
                    password = password,
                    bearerToken = bearerToken,
                    startupTab = startupTab,
                    themeId = _uiState.value.settings.themeId,
                    hasCompletedSetup = _uiState.value.settings.hasCompletedSetup && serverUrl.isNotBlank(),
                    hasCompletedDefaultScopePrompt = when {
                        serverUrl.isBlank() -> false
                        scopePrefix.isNotBlank() -> true
                        else -> _uiState.value.settings.hasCompletedDefaultScopePrompt
                    },
                ),
            )
            refresh(trigger = "settings")
        }
    }

    fun completeInitialSetup(
        serverUrl: String,
        username: String,
        password: String,
        bearerToken: String,
        startupTab: String,
    ) {
        val normalizedServerUrl = serverUrl.trim()
        val normalizedUsername = username.trim()
        val normalizedBearerToken = bearerToken.trim()
        val hasPartialPasswordAuth = normalizedBearerToken.isBlank() &&
            (normalizedUsername.isBlank() xor password.isBlank())

        if (normalizedServerUrl.isBlank()) {
            _uiState.update { it.copy(error = "Enter a server URL first.") }
            return
        }
        if (hasPartialPasswordAuth) {
            _uiState.update {
                it.copy(error = "Enter both username and password, or leave both empty.")
            }
            return
        }

        val currentSettings = _uiState.value.settings
        val candidateSettings = AppSettings(
            serverUrl = normalizedServerUrl,
            scopePrefix = currentSettings.scopePrefix,
            username = normalizedUsername,
            password = password,
            bearerToken = normalizedBearerToken,
            startupTab = startupTab,
            themeId = currentSettings.themeId,
            hasCompletedSetup = true,
            hasCompletedDefaultScopePrompt = currentSettings.scopePrefix.isNotBlank(),
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching {
                fetchCollectionsSnapshotForSetup(candidateSettings)
            }.onSuccess { collections ->
                settingsRepository.save(candidateSettings)
                _uiState.update { current ->
                    current.copy(
                        settings = candidateSettings,
                        settingsLoaded = true,
                        pages = collections.pages ?: current.pages,
                        folders = collections.folders ?: current.folders,
                        tasks = collections.tasks ?: current.tasks,
                        today = collections.today ?: current.today,
                        isLoading = false,
                        error = null,
                    )
                }
                fetchVaults()
            }.onFailure { error ->
                if (error is CancellationException) return@onFailure
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = initialSetupErrorMessage(
                            error = error,
                            username = normalizedUsername,
                            password = password,
                            bearerToken = normalizedBearerToken,
                        ),
                    )
                }
            }
        }
    }

    fun saveThemeSelection(themeId: String) {
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            settingsRepository.save(settings.copy(themeId = themeId))
        }
    }

    fun ensureThemeLibraryLoaded(force: Boolean = false) {
        val settings = _uiState.value.settings
        if (settings.serverUrl.isBlank() || !canAttemptBackgroundSync(settings)) return
        val cacheKey = themeInventoryKey(settings)
        if (!force && themeLibraryCacheKey == cacheKey && _uiState.value.themes.isNotEmpty()) {
            return
        }
        if (_uiState.value.isThemesLoading) {
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isThemesLoading = true, error = null) }
            runCatching {
                repository.fetchThemes(
                    url = settings.serverUrl,
                    bearerToken = settings.bearerToken,
                    username = settings.username,
                    password = settings.password,
                )
            }.onSuccess { themes ->
                themeLibraryCacheKey = cacheKey
                val selectedThemeId = settings.themeId.trim()
                if (
                    selectedThemeId.isNotBlank() &&
                    !selectedThemeId.equals("system", ignoreCase = true) &&
                    themes.none { it.id.equals(selectedThemeId, ignoreCase = true) }
                ) {
                    settingsRepository.save(settings.copy(themeId = "system"))
                }
                _uiState.update {
                    it.copy(
                        themes = themes,
                        isThemesLoading = false,
                    )
                }
            }.onFailure { error ->
                if (error is CancellationException) return@onFailure
                _uiState.update {
                    it.copy(
                        isThemesLoading = false,
                        error = error.message ?: "Themes could not be loaded.",
                    )
                }
            }
        }
    }

    fun ensureSettingsDetailsLoaded(force: Boolean = false) {
        val settings = _uiState.value.settings
        if (settings.serverUrl.isBlank() || !canAttemptBackgroundSync(settings)) return
        val cacheKey = settingsDetailsInventoryKey(settings)
        if (!force && settingsDetailsCacheKey == cacheKey) {
            return
        }
        if (_uiState.value.isSettingsDetailsLoading) {
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSettingsDetailsLoading = true, error = null) }
            var firstError: Throwable? = null

            val serverSettings = runCatching {
                repository.fetchServerSettings(
                    url = settings.serverUrl,
                    bearerToken = settings.bearerToken,
                    username = settings.username,
                    password = settings.password,
                )
            }.onFailure { error ->
                if (firstError == null && error !is CancellationException) {
                    firstError = error
                }
            }.getOrNull()

            val userSettings = runCatching {
                repository.fetchUserSettings(
                    url = settings.serverUrl,
                    bearerToken = settings.bearerToken,
                    username = settings.username,
                    password = settings.password,
                )
            }.onFailure { error ->
                if (firstError == null && error !is CancellationException) {
                    firstError = error
                }
            }.getOrNull()

            val serverMeta = runCatching {
                repository.fetchServerMeta(
                    url = settings.serverUrl,
                    bearerToken = settings.bearerToken,
                    username = settings.username,
                    password = settings.password,
                )
            }.onFailure { error ->
                if (firstError == null && error !is CancellationException) {
                    firstError = error
                }
            }.getOrNull()

            if (serverSettings != null || userSettings != null || serverMeta != null) {
                settingsDetailsCacheKey = cacheKey
            }

            _uiState.update { current ->
                current.copy(
                    serverSettings = serverSettings ?: current.serverSettings,
                    userSettings = userSettings ?: current.userSettings,
                    serverMeta = serverMeta ?: current.serverMeta,
                    isSettingsDetailsLoading = false,
                    error = firstError?.message ?: current.error,
                )
            }
        }
    }

    fun saveUserNotificationSettings(
        ntfyTopicUrl: String,
        ntfyToken: String,
        onResult: (Boolean) -> Unit = {},
    ) {
        val settings = _uiState.value.settings
        if (settings.serverUrl.isBlank() || !canAttemptBackgroundSync(settings)) {
            onResult(false)
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isUserSettingsSaving = true, error = null) }
            runCatching {
                repository.saveUserSettings(
                    url = settings.serverUrl,
                    settings = UserSettingsPayload(
                        notifications = UserNotificationSettings(
                            ntfyTopicUrl = ntfyTopicUrl.trim(),
                            ntfyToken = ntfyToken.trim(),
                        ),
                    ),
                    bearerToken = settings.bearerToken,
                    username = settings.username,
                    password = settings.password,
                )
            }.onSuccess { userSettings ->
                settingsDetailsCacheKey = settingsDetailsInventoryKey(settings)
                _uiState.update {
                    it.copy(
                        userSettings = userSettings,
                        isUserSettingsSaving = false,
                    )
                }
                onResult(true)
            }.onFailure { error ->
                if (error is CancellationException) return@onFailure
                _uiState.update {
                    it.copy(
                        isUserSettingsSaving = false,
                        error = error.message ?: "User settings could not be saved.",
                    )
                }
                onResult(false)
            }
        }
    }

    fun ensureTemplatePagesLoaded(force: Boolean = false) {
        val settings = _uiState.value.settings
        if (settings.serverUrl.isBlank()) return
        val cacheKey = templatePagesInventoryKey(settings)
        if (!force && templatePagesCacheKey == cacheKey) {
            return
        }
        if (templatePagesJob?.isActive == true) {
            return
        }

        templatePagesJob = viewModelScope.launch {
            runCatching {
                repository.fetchPages(
                    url = settings.serverUrl,
                    scopePrefix = "",
                    bearerToken = settings.bearerToken,
                    username = settings.username,
                    password = settings.password,
                )
            }.onSuccess { pages ->
                templatePagesCacheKey = cacheKey
                _uiState.update { it.copy(templatePages = pages) }
            }.onFailure { error ->
                if (error is CancellationException) return@onFailure
                _uiState.update {
                    it.copy(error = error.message ?: "Templates could not be loaded.")
                }
            }
        }
    }

    fun changePassword(
        currentPassword: String,
        newPassword: String,
        onResult: (Boolean) -> Unit = {},
    ) {
        val trimmedCurrentPassword = currentPassword.trim()
        val trimmedNewPassword = newPassword.trim()
        val settings = _uiState.value.settings
        if (
            settings.serverUrl.isBlank() ||
            trimmedCurrentPassword.isBlank() ||
            trimmedNewPassword.isBlank()
        ) {
            onResult(false)
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            runCatching {
                repository.changePassword(
                    url = settings.serverUrl,
                    currentPassword = trimmedCurrentPassword,
                    newPassword = trimmedNewPassword,
                    bearerToken = settings.bearerToken,
                    username = settings.username,
                    password = settings.password,
                )
            }.onSuccess {
                val nextSettings = if (settings.password.isNotBlank()) {
                    settings.copy(password = trimmedNewPassword)
                } else {
                    settings
                }
                if (nextSettings != settings) {
                    settingsRepository.save(nextSettings)
                    _uiState.update { it.copy(settings = nextSettings) }
                }
                onResult(true)
            }.onFailure { error ->
                if (error is CancellationException) return@onFailure
                _uiState.update {
                    it.copy(error = error.message ?: "Password could not be changed.")
                }
                onResult(false)
            }
        }
    }

    fun logout(onResult: (Boolean) -> Unit = {}) {
        val settings = _uiState.value.settings
        viewModelScope.launch {
            runCatching {
                if (settings.serverUrl.isNotBlank()) {
                    repository.logout(
                        url = settings.serverUrl,
                        bearerToken = settings.bearerToken,
                    )
                }
            }

            val nextSettings = settings.copy(password = "", bearerToken = "")
            templatePagesCacheKey = null
            settingsDetailsCacheKey = null
            openPageBackStack.clear()
            stopEventStream()
            settingsRepository.save(nextSettings)
            _uiState.value = MainUiState(settings = nextSettings)
            onResult(true)
        }
    }

    fun onAppForegrounded() {
        appInForeground = true
        reconcileEventStream()
        consumePendingDeepLinkIfPossible()
        loadForegroundDataIfNeeded(trigger = "foreground")
    }

    fun onAppBackgrounded() {
        appInForeground = false
        stopEventStream()
    }

    fun openPage(pagePath: String, addToBackStack: Boolean = true) {
        val settings = _uiState.value.settings
        if (settings.serverUrl.isBlank()) return
        val normalizedPagePath = applyScopePrefixToPagePath(pagePath, settings.scopePrefix)
        if (normalizedPagePath.isBlank()) return
        val currentPath = _uiState.value.openPagePath
        if (addToBackStack && currentPath != null && currentPath != normalizedPagePath) {
            openPageBackStack.addLast(currentPath)
        }

        openPageJob?.cancel()
        openPageJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    openPagePath = normalizedPagePath,
                    openPageContent = null,
                    openPageFrontmatter = null,
                    openPageTasks = emptyList(),
                    openPageDerived = null,
                    openPageHistory = emptyList(),
                    isPageLoading = true,
                    isPageHistoryLoading = false,
                    isPageHistoryBusy = false,
                )
            }
            runCatching {
                val detail = repository.fetchPageDetail(
                    url = settings.serverUrl,
                    pagePath = normalizedPagePath,
                    scopePrefix = settings.scopePrefix,
                    bearerToken = settings.bearerToken,
                    username = settings.username,
                    password = settings.password,
                )
                val derived = runCatching {
                    repository.fetchDerivedPage(
                        url = settings.serverUrl,
                        pagePath = normalizedPagePath,
                        scopePrefix = settings.scopePrefix,
                        bearerToken = settings.bearerToken,
                        username = settings.username,
                        password = settings.password,
                    )
                }.getOrNull()
                OpenPageLoadResult(
                    rawMarkdown = detail.rawMarkdown,
                    frontmatter = detail.frontmatter,
                    pageTasks = detail.tasks,
                    derived = derived,
                )
            }.onSuccess { result ->
                _uiState.update {
                    it.copy(
                        openPageContent = result.rawMarkdown,
                        openPageFrontmatter = result.frontmatter,
                        openPageTasks = result.pageTasks,
                        openPageDerived = result.derived,
                        isPageLoading = false,
                    )
                }
            }.onFailure { error ->
                if (error is CancellationException) return@onFailure
                _uiState.update {
                    it.copy(isPageLoading = false, error = error.message ?: "Page could not be loaded.")
                }
            }
        }
    }

    fun createPage(
        pagePath: String,
        initialMarkdown: String? = null,
        addToBackStack: Boolean = true,
        onResult: (Boolean) -> Unit = {},
    ) {
        val settings = _uiState.value.settings
        if (settings.serverUrl.isBlank()) {
            onResult(false)
            return
        }
        val normalizedPagePath = applyScopePrefixToPagePath(pagePath, settings.scopePrefix)
        if (normalizedPagePath.isBlank()) {
            onResult(false)
            return
        }

        val existingPage = _uiState.value.pages.firstOrNull {
            normalizePagePath(it.path).equals(normalizedPagePath, ignoreCase = true)
        }
        if (existingPage != null) {
            openPage(existingPage.path, addToBackStack = addToBackStack)
            onResult(true)
            return
        }

        val currentPath = _uiState.value.openPagePath
        if (addToBackStack && currentPath != null && currentPath != normalizedPagePath) {
            openPageBackStack.addLast(currentPath)
        }

        viewModelScope.launch {
            _uiState.update { it.copy(error = null, isPageSaving = true) }

            runCatching {
                val initialContent = initialMarkdown ?: pageTitleFromPath(normalizedPagePath)
                    .takeIf(String::isNotBlank)
                    ?.let { "# $it\n" }
                    .orEmpty()
                val detail = repository.createPage(
                    url = settings.serverUrl,
                    pagePath = normalizedPagePath,
                    rawMarkdown = initialContent,
                    scopePrefix = settings.scopePrefix,
                    bearerToken = settings.bearerToken,
                    username = settings.username,
                    password = settings.password,
                )
                val derived = runCatching {
                    repository.fetchDerivedPage(
                        url = settings.serverUrl,
                        pagePath = normalizedPagePath,
                        scopePrefix = settings.scopePrefix,
                        bearerToken = settings.bearerToken,
                        username = settings.username,
                        password = settings.password,
                    )
                }.getOrNull()
                val pages = runCatching {
                    repository.fetchPages(
                        url = settings.serverUrl,
                        scopePrefix = settings.scopePrefix,
                        bearerToken = settings.bearerToken,
                        username = settings.username,
                        password = settings.password,
                    )
                }.getOrNull()
                val folders = runCatching {
                    repository.fetchFolders(
                        url = settings.serverUrl,
                        scopePrefix = settings.scopePrefix,
                        bearerToken = settings.bearerToken,
                        username = settings.username,
                        password = settings.password,
                    )
                }.getOrNull()
                val tasks = runCatching {
                    repository.fetchTasks(
                        url = settings.serverUrl,
                        scopePrefix = settings.scopePrefix,
                        bearerToken = settings.bearerToken,
                        username = settings.username,
                        password = settings.password,
                    )
                }.getOrNull()
                SavePageResult(
                    rawMarkdown = detail.rawMarkdown,
                    frontmatter = detail.frontmatter,
                    pageTasks = detail.tasks,
                    derived = derived,
                    pages = pages,
                    folders = folders,
                    tasks = tasks,
                    today = tasks?.let(repository::buildTodaySnapshot),
                )
            }.onSuccess { result ->
                _uiState.update { current ->
                    current.copy(
                        openPagePath = normalizedPagePath,
                        openPageContent = result.rawMarkdown,
                        openPageFrontmatter = result.frontmatter,
                        openPageTasks = result.pageTasks,
                        openPageDerived = result.derived,
                        openPageHistory = emptyList(),
                        isPageLoading = false,
                        isPageSaving = false,
                        isPageHistoryLoading = false,
                        isPageHistoryBusy = false,
                        pages = result.pages ?: current.pages,
                        folders = result.folders ?: current.folders,
                        tasks = result.tasks ?: current.tasks,
                        today = result.today ?: current.today,
                    )
                }
                onResult(true)
            }.onFailure { error ->
                if (error is CancellationException) return@onFailure
                _uiState.update {
                    it.copy(isPageSaving = false, error = error.message ?: "Page could not be created.")
                }
                onResult(false)
            }
        }
    }

    internal fun createPageFromTemplate(
        template: NoteTemplate,
        pagePath: String,
        addToBackStack: Boolean = true,
        onResult: (Boolean) -> Unit = {},
    ) {
        val settings = _uiState.value.settings
        if (settings.serverUrl.isBlank()) {
            onResult(false)
            return
        }
        val normalizedTargetPath = buildPagePathFromTemplate(template, pagePath)
        val scopedTargetPath = applyScopePrefixToPagePath(normalizedTargetPath, settings.scopePrefix)
        if (normalizedTargetPath.isBlank() || scopedTargetPath.isBlank()) {
            onResult(false)
            return
        }

        val existingPage = _uiState.value.pages.firstOrNull {
            normalizePagePath(it.path).equals(scopedTargetPath, ignoreCase = true)
        }
        if (existingPage != null) {
            openPage(existingPage.path, addToBackStack = addToBackStack)
            onResult(true)
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(error = null, isPageSaving = true) }
            runCatching {
                val templatePage = repository.fetchPageDetail(
                    url = settings.serverUrl,
                    pagePath = template.id,
                    scopePrefix = "",
                    bearerToken = settings.bearerToken,
                    username = settings.username,
                    password = settings.password,
                )
                buildMarkdownFromTemplate(
                    pagePath = scopedTargetPath,
                    templatePage = templatePage,
                )
            }.onSuccess { markdown ->
                createPage(
                    pagePath = normalizedTargetPath,
                    initialMarkdown = markdown,
                    addToBackStack = addToBackStack,
                    onResult = onResult,
                )
            }.onFailure { error ->
                if (error is CancellationException) return@onFailure
                _uiState.update {
                    it.copy(
                        isPageSaving = false,
                        error = error.message ?: "Template note could not be created.",
                    )
                }
                onResult(false)
            }
        }
    }

    fun renamePage(
        pagePath: String,
        nextPagePath: String,
        onResult: (Boolean) -> Unit = {},
    ) {
        val settings = _uiState.value.settings
        if (settings.serverUrl.isBlank()) {
            onResult(false)
            return
        }
        val normalizedCurrentPagePath = applyScopePrefixToPagePath(pagePath, settings.scopePrefix)
        if (normalizedCurrentPagePath.isBlank()) {
            onResult(false)
            return
        }
        val normalizedTargetPagePath = applyScopePrefixToPagePath(nextPagePath, settings.scopePrefix)
        if (normalizedTargetPagePath.isBlank()) {
            onResult(false)
            return
        }
        if (normalizedCurrentPagePath.equals(normalizedTargetPagePath, ignoreCase = true)) {
            onResult(true)
            return
        }
        val activeSearchQuery = _uiState.value.searchResults?.query?.takeIf(String::isNotBlank)

        viewModelScope.launch {
            _uiState.update { it.copy(error = null, isPageSaving = true) }

            runCatching {
                val movedDetail = repository.movePage(
                    url = settings.serverUrl,
                    pagePath = normalizedCurrentPagePath,
                    targetPage = normalizedTargetPagePath,
                    scopePrefix = settings.scopePrefix,
                    bearerToken = settings.bearerToken,
                    username = settings.username,
                    password = settings.password,
                )
                buildPageMutationResult(
                    settings = settings,
                    pagePath = normalizedTargetPagePath,
                    detail = movedDetail,
                )
            }.onSuccess { result ->
                remapPageReferencesInBackStack(
                    fromPagePath = normalizedCurrentPagePath,
                    toPagePath = normalizedTargetPagePath,
                )
                _uiState.update { current ->
                    val isOpenPage = current.openPagePath?.let { openPagePath ->
                        normalizePagePath(openPagePath).equals(normalizedCurrentPagePath, ignoreCase = true)
                    } == true
                    current.copy(
                        openPagePath = if (isOpenPage) normalizedTargetPagePath else current.openPagePath,
                        openPageContent = if (isOpenPage) result.rawMarkdown else current.openPageContent,
                        openPageFrontmatter = if (isOpenPage) result.frontmatter else current.openPageFrontmatter,
                        openPageTasks = if (isOpenPage) result.pageTasks else current.openPageTasks,
                        openPageDerived = if (isOpenPage) result.derived else current.openPageDerived,
                        openPageHistory = if (isOpenPage) emptyList() else current.openPageHistory,
                        isPageLoading = if (isOpenPage) false else current.isPageLoading,
                        isPageSaving = false,
                        isPageHistoryLoading = if (isOpenPage) false else current.isPageHistoryLoading,
                        isPageHistoryBusy = if (isOpenPage) false else current.isPageHistoryBusy,
                        pages = result.pages ?: current.pages,
                        folders = result.folders ?: current.folders,
                        tasks = result.tasks ?: current.tasks,
                        today = result.today ?: current.today,
                    )
                }
                activeSearchQuery?.let(::search)
                onResult(true)
            }.onFailure { error ->
                if (error is CancellationException) return@onFailure
                _uiState.update {
                    it.copy(isPageSaving = false, error = error.message ?: "Page could not be renamed.")
                }
                onResult(false)
            }
        }
    }

    fun renameOpenPage(
        nextPagePath: String,
        onResult: (Boolean) -> Unit = {},
    ) {
        val currentPagePath = _uiState.value.openPagePath ?: run {
            onResult(false)
            return
        }
        renamePage(
            pagePath = currentPagePath,
            nextPagePath = nextPagePath,
            onResult = onResult,
        )
    }

    fun deletePage(
        pagePath: String,
        onResult: (Boolean) -> Unit = {},
    ) {
        val settings = _uiState.value.settings
        if (settings.serverUrl.isBlank()) {
            onResult(false)
            return
        }
        val normalizedCurrentPagePath = applyScopePrefixToPagePath(pagePath, settings.scopePrefix)
        if (normalizedCurrentPagePath.isBlank()) {
            onResult(false)
            return
        }
        val activeSearchQuery = _uiState.value.searchResults?.query?.takeIf(String::isNotBlank)

        viewModelScope.launch {
            _uiState.update { it.copy(error = null, isPageSaving = true) }

            runCatching {
                repository.deletePage(
                    url = settings.serverUrl,
                    pagePath = normalizedCurrentPagePath,
                    scopePrefix = settings.scopePrefix,
                    bearerToken = settings.bearerToken,
                    username = settings.username,
                    password = settings.password,
                )
                fetchCollectionsSnapshot(settings)
            }.onSuccess { collections ->
                remapPageReferencesInBackStack(
                    fromPagePath = normalizedCurrentPagePath,
                    toPagePath = null,
                )
                val isOpenPage = _uiState.value.openPagePath?.let { openPagePath ->
                    normalizePagePath(openPagePath).equals(normalizedCurrentPagePath, ignoreCase = true)
                } == true
                val nextPagePath = if (isOpenPage) {
                    popOpenPageBackStackCandidate(collections.pages)
                } else {
                    null
                }
                _uiState.update { current ->
                    current.copy(
                        openPagePath = if (isOpenPage) null else current.openPagePath,
                        openPageContent = if (isOpenPage) null else current.openPageContent,
                        openPageFrontmatter = if (isOpenPage) null else current.openPageFrontmatter,
                        openPageTasks = if (isOpenPage) emptyList() else current.openPageTasks,
                        openPageDerived = if (isOpenPage) null else current.openPageDerived,
                        openPageHistory = if (isOpenPage) emptyList() else current.openPageHistory,
                        isPageLoading = if (isOpenPage) false else current.isPageLoading,
                        isPageSaving = false,
                        isPageHistoryLoading = if (isOpenPage) false else current.isPageHistoryLoading,
                        isPageHistoryBusy = if (isOpenPage) false else current.isPageHistoryBusy,
                        pages = collections.pages ?: current.pages,
                        folders = collections.folders ?: current.folders,
                        tasks = collections.tasks ?: current.tasks,
                        today = collections.today ?: current.today,
                    )
                }
                if (nextPagePath != null) {
                    openPage(nextPagePath, addToBackStack = false)
                }
                activeSearchQuery?.let(::search)
                onResult(true)
            }.onFailure { error ->
                if (error is CancellationException) return@onFailure
                _uiState.update {
                    it.copy(isPageSaving = false, error = error.message ?: "Page could not be deleted.")
                }
                onResult(false)
            }
        }
    }

    fun deleteOpenPage(
        onResult: (Boolean) -> Unit = {},
    ) {
        val currentPagePath = _uiState.value.openPagePath ?: run {
            onResult(false)
            return
        }
        deletePage(
            pagePath = currentPagePath,
            onResult = onResult,
        )
    }

    fun createFolder(
        folderPath: String,
        onResult: (Boolean) -> Unit = {},
    ) {
        val settings = _uiState.value.settings
        if (settings.serverUrl.isBlank()) {
            onResult(false)
            return
        }
        val normalizedFolderPath = applyScopePrefixToPagePath(folderPath, settings.scopePrefix)
        if (normalizedFolderPath.isBlank()) {
            onResult(false)
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(error = null, isLoading = true) }

            runCatching {
                repository.createFolder(
                    url = settings.serverUrl,
                    folderPath = normalizedFolderPath,
                    scopePrefix = settings.scopePrefix,
                    bearerToken = settings.bearerToken,
                    username = settings.username,
                    password = settings.password,
                )
                fetchCollectionsSnapshot(settings)
            }.onSuccess { collections ->
                _uiState.update { current ->
                    current.copy(
                        pages = collections.pages ?: current.pages,
                        folders = collections.folders ?: current.folders,
                        tasks = collections.tasks ?: current.tasks,
                        today = collections.today ?: current.today,
                        isLoading = false,
                    )
                }
                onResult(true)
            }.onFailure { error ->
                if (error is CancellationException) return@onFailure
                _uiState.update {
                    it.copy(isLoading = false, error = error.message ?: "Folder could not be created.")
                }
                onResult(false)
            }
        }
    }

    fun renameFolder(
        folderPath: String,
        nextFolderPath: String,
        onResult: (Boolean) -> Unit = {},
    ) {
        val settings = _uiState.value.settings
        if (settings.serverUrl.isBlank()) {
            onResult(false)
            return
        }
        val normalizedCurrentFolderPath = applyScopePrefixToPagePath(folderPath, settings.scopePrefix)
        if (normalizedCurrentFolderPath.isBlank()) {
            onResult(false)
            return
        }
        val normalizedTargetFolderPath = applyScopePrefixToPagePath(nextFolderPath, settings.scopePrefix)
        if (normalizedTargetFolderPath.isBlank()) {
            onResult(false)
            return
        }
        if (normalizedCurrentFolderPath.equals(normalizedTargetFolderPath, ignoreCase = true)) {
            onResult(true)
            return
        }
        val activeSearchQuery = _uiState.value.searchResults?.query?.takeIf(String::isNotBlank)

        viewModelScope.launch {
            _uiState.update { it.copy(error = null, isLoading = true) }

            runCatching {
                repository.moveFolder(
                    url = settings.serverUrl,
                    folderPath = normalizedCurrentFolderPath,
                    targetFolderPath = normalizedTargetFolderPath,
                    scopePrefix = settings.scopePrefix,
                    bearerToken = settings.bearerToken,
                    username = settings.username,
                    password = settings.password,
                )
                fetchCollectionsSnapshot(settings)
            }.onSuccess { collections ->
                val nextOpenPagePath = _uiState.value.openPagePath?.let { currentOpenPagePath ->
                    remapPagePathInsideFolder(
                        pagePath = currentOpenPagePath,
                        fromFolderPath = normalizedCurrentFolderPath,
                        toFolderPath = normalizedTargetFolderPath,
                    )
                }
                remapFolderReferencesInBackStack(
                    fromFolderPath = normalizedCurrentFolderPath,
                    toFolderPath = normalizedTargetFolderPath,
                )
                _uiState.update { current ->
                    current.copy(
                        pages = collections.pages ?: current.pages,
                        folders = collections.folders ?: current.folders,
                        tasks = collections.tasks ?: current.tasks,
                        today = collections.today ?: current.today,
                        isLoading = false,
                    )
                }
                if (nextOpenPagePath != null) {
                    openPage(nextOpenPagePath, addToBackStack = false)
                }
                activeSearchQuery?.let(::search)
                onResult(true)
            }.onFailure { error ->
                if (error is CancellationException) return@onFailure
                _uiState.update {
                    it.copy(isLoading = false, error = error.message ?: "Folder could not be renamed.")
                }
                onResult(false)
            }
        }
    }

    fun deleteFolder(
        folderPath: String,
        onResult: (Boolean) -> Unit = {},
    ) {
        val settings = _uiState.value.settings
        if (settings.serverUrl.isBlank()) {
            onResult(false)
            return
        }
        val normalizedFolderPath = applyScopePrefixToPagePath(folderPath, settings.scopePrefix)
        if (normalizedFolderPath.isBlank()) {
            onResult(false)
            return
        }
        val activeSearchQuery = _uiState.value.searchResults?.query?.takeIf(String::isNotBlank)

        viewModelScope.launch {
            _uiState.update { it.copy(error = null, isLoading = true) }

            runCatching {
                repository.deleteFolder(
                    url = settings.serverUrl,
                    folderPath = normalizedFolderPath,
                    scopePrefix = settings.scopePrefix,
                    bearerToken = settings.bearerToken,
                    username = settings.username,
                    password = settings.password,
                )
                fetchCollectionsSnapshot(settings)
            }.onSuccess { collections ->
                remapFolderReferencesInBackStack(
                    fromFolderPath = normalizedFolderPath,
                    toFolderPath = null,
                )
                val isOpenPageAffected = _uiState.value.openPagePath?.let { currentOpenPagePath ->
                    isPageInsideFolder(currentOpenPagePath, normalizedFolderPath)
                } == true
                val nextPagePath = if (isOpenPageAffected) {
                    popOpenPageBackStackCandidate(collections.pages)
                } else {
                    null
                }
                _uiState.update { current ->
                    current.copy(
                        openPagePath = if (isOpenPageAffected) null else current.openPagePath,
                        openPageContent = if (isOpenPageAffected) null else current.openPageContent,
                        openPageFrontmatter = if (isOpenPageAffected) null else current.openPageFrontmatter,
                        openPageTasks = if (isOpenPageAffected) emptyList() else current.openPageTasks,
                        openPageDerived = if (isOpenPageAffected) null else current.openPageDerived,
                        openPageHistory = if (isOpenPageAffected) emptyList() else current.openPageHistory,
                        isPageLoading = if (isOpenPageAffected) false else current.isPageLoading,
                        isPageSaving = if (isOpenPageAffected) false else current.isPageSaving,
                        isPageHistoryLoading = if (isOpenPageAffected) false else current.isPageHistoryLoading,
                        isPageHistoryBusy = if (isOpenPageAffected) false else current.isPageHistoryBusy,
                        pages = collections.pages ?: current.pages,
                        folders = collections.folders ?: current.folders,
                        tasks = collections.tasks ?: current.tasks,
                        today = collections.today ?: current.today,
                        isLoading = false,
                    )
                }
                if (nextPagePath != null) {
                    openPage(nextPagePath, addToBackStack = false)
                }
                activeSearchQuery?.let(::search)
                onResult(true)
            }.onFailure { error ->
                if (error is CancellationException) return@onFailure
                _uiState.update {
                    it.copy(isLoading = false, error = error.message ?: "Folder could not be deleted.")
                }
                onResult(false)
            }
        }
    }

    fun uploadTheme(
        uri: Uri,
        onResult: (ThemeRecord?) -> Unit = {},
    ) {
        val settings = _uiState.value.settings
        if (settings.serverUrl.isBlank()) {
            onResult(null)
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(error = null, isThemeBusy = true) }
            runCatching {
                val uploadSpec = withContext(Dispatchers.IO) {
                    readUploadSpec(uri)
                }
                repository.uploadTheme(
                    url = settings.serverUrl,
                    fileName = uploadSpec.fileName,
                    contentType = uploadSpec.contentType.ifBlank { "application/json" },
                    content = uploadSpec.content,
                    bearerToken = settings.bearerToken,
                    username = settings.username,
                    password = settings.password,
                )
            }.mapCatching { createdTheme ->
                val nextSettings = settings.copy(themeId = createdTheme.id)
                settingsRepository.save(nextSettings)
                ThemeMutationResult(
                    theme = createdTheme,
                    themes = repository.fetchThemes(
                        url = settings.serverUrl,
                        bearerToken = settings.bearerToken,
                        username = settings.username,
                        password = settings.password,
                    ),
                )
            }.onSuccess { result ->
                themeLibraryCacheKey = themeInventoryKey(_uiState.value.settings)
                _uiState.update {
                    it.copy(
                        themes = result.themes,
                        isThemeBusy = false,
                    )
                }
                onResult(result.theme)
            }.onFailure { error ->
                if (error is CancellationException) return@onFailure
                _uiState.update {
                    it.copy(
                        isThemeBusy = false,
                        error = error.message ?: "Theme could not be uploaded.",
                    )
                }
                onResult(null)
            }
        }
    }

    fun deleteTheme(
        themeId: String,
        onResult: (Boolean) -> Unit = {},
    ) {
        val settings = _uiState.value.settings
        val normalizedThemeId = themeId.trim()
        if (settings.serverUrl.isBlank() || normalizedThemeId.isBlank()) {
            onResult(false)
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(error = null, isThemeBusy = true) }
            runCatching {
                repository.deleteTheme(
                    url = settings.serverUrl,
                    themeId = normalizedThemeId,
                    bearerToken = settings.bearerToken,
                    username = settings.username,
                    password = settings.password,
                )
                val currentSettings = settingsRepository.settings.first()
                if (currentSettings.themeId.equals(normalizedThemeId, ignoreCase = true)) {
                    settingsRepository.save(currentSettings.copy(themeId = "system"))
                }
                repository.fetchThemes(
                    url = settings.serverUrl,
                    bearerToken = settings.bearerToken,
                    username = settings.username,
                    password = settings.password,
                )
            }.onSuccess { themes ->
                themeLibraryCacheKey = themeInventoryKey(_uiState.value.settings)
                _uiState.update {
                    it.copy(
                        themes = themes,
                        isThemeBusy = false,
                    )
                }
                onResult(true)
            }.onFailure { error ->
                if (error is CancellationException) return@onFailure
                _uiState.update {
                    it.copy(
                        isThemeBusy = false,
                        error = error.message ?: "Theme could not be deleted.",
                    )
                }
                onResult(false)
            }
        }
    }

    fun fetchDocuments() {
        val settings = _uiState.value.settings
        if (settings.serverUrl.isBlank()) {
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(error = null, isDocumentsLoading = true) }

            runCatching {
                repository.fetchDocuments(
                    url = settings.serverUrl,
                    scopePrefix = settings.scopePrefix,
                    bearerToken = settings.bearerToken,
                    username = settings.username,
                    password = settings.password,
                )
            }.onSuccess { documents ->
                _uiState.update {
                    it.copy(
                        documents = documents,
                        isDocumentsLoading = false,
                    )
                }
            }.onFailure { error ->
                if (error is CancellationException) return@onFailure
                _uiState.update {
                    it.copy(
                        isDocumentsLoading = false,
                        error = error.message ?: "Documents could not be loaded.",
                    )
                }
            }
        }
    }

    fun moveDocument(
        documentPath: String,
        nextDocumentPath: String,
        onResult: (Boolean) -> Unit = {},
    ) {
        val settings = _uiState.value.settings
        if (settings.serverUrl.isBlank()) {
            onResult(false)
            return
        }
        val normalizedDocumentPath = normalizePagePath(documentPath)
        if (normalizedDocumentPath.isBlank()) {
            onResult(false)
            return
        }
        val normalizedTargetPath = applyScopePrefixToPagePath(nextDocumentPath, settings.scopePrefix)
        if (normalizedTargetPath.isBlank()) {
            onResult(false)
            return
        }
        if (normalizedDocumentPath.equals(normalizedTargetPath, ignoreCase = true)) {
            onResult(true)
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(error = null, isDocumentsBusy = true) }

            runCatching {
                val move = repository.moveDocument(
                    url = settings.serverUrl,
                    documentPath = normalizedDocumentPath,
                    targetPath = normalizedTargetPath,
                    scopePrefix = settings.scopePrefix,
                    bearerToken = settings.bearerToken,
                    username = settings.username,
                    password = settings.password,
                )
                DocumentMoveMutationResult(
                    move = move,
                    documents = fetchDocumentsSnapshot(settings),
                    collections = if (move.rewrittenPages.isNotEmpty()) {
                        fetchCollectionsSnapshot(settings)
                    } else {
                        null
                    },
                )
            }.onSuccess { result ->
                val openPagePath = _uiState.value.openPagePath
                val shouldReloadOpenPage = openPagePath != null && result.move.rewrittenPages.any { rewrittenPagePath ->
                    normalizePagePath(rewrittenPagePath).equals(normalizePagePath(openPagePath), ignoreCase = true)
                }
                val openPagePathToReload = if (shouldReloadOpenPage) openPagePath else null
                _uiState.update { current ->
                    current.copy(
                        documents = result.documents ?: current.documents.map { document ->
                            if (normalizePagePath(document.path).equals(normalizePagePath(normalizedDocumentPath), ignoreCase = true)) {
                                result.move.document
                            } else {
                                document
                            }
                        },
                        pages = result.collections?.pages ?: current.pages,
                        folders = result.collections?.folders ?: current.folders,
                        tasks = result.collections?.tasks ?: current.tasks,
                        today = result.collections?.today ?: current.today,
                        isDocumentsLoading = false,
                        isDocumentsBusy = false,
                    )
                }
                if (openPagePathToReload != null) {
                    openPage(openPagePathToReload, addToBackStack = false)
                }
                onResult(true)
            }.onFailure { error ->
                if (error is CancellationException) return@onFailure
                _uiState.update {
                    it.copy(
                        isDocumentsBusy = false,
                        error = error.message ?: "File could not be moved.",
                    )
                }
                onResult(false)
            }
        }
    }

    fun deleteDocument(
        documentPath: String,
        onResult: (Boolean) -> Unit = {},
    ) {
        val settings = _uiState.value.settings
        if (settings.serverUrl.isBlank()) {
            onResult(false)
            return
        }
        val normalizedDocumentPath = normalizePagePath(documentPath)
        if (normalizedDocumentPath.isBlank()) {
            onResult(false)
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(error = null, isDocumentsBusy = true) }

            runCatching {
                repository.deleteDocument(
                    url = settings.serverUrl,
                    documentPath = normalizedDocumentPath,
                    scopePrefix = settings.scopePrefix,
                    bearerToken = settings.bearerToken,
                    username = settings.username,
                    password = settings.password,
                )
                fetchDocumentsSnapshot(settings)
            }.onSuccess { documents ->
                _uiState.update { current ->
                    current.copy(
                        documents = documents ?: current.documents.filterNot { document ->
                            normalizePagePath(document.path).equals(normalizedDocumentPath, ignoreCase = true)
                        },
                        isDocumentsLoading = false,
                        isDocumentsBusy = false,
                    )
                }
                onResult(true)
            }.onFailure { error ->
                if (error is CancellationException) return@onFailure
                _uiState.update {
                    it.copy(
                        isDocumentsBusy = false,
                        error = error.message ?: "File could not be deleted.",
                    )
                }
                onResult(false)
            }
        }
    }

    fun fetchOpenPageHistory() {
        val pagePath = _uiState.value.openPagePath ?: return
        val settings = _uiState.value.settings
        if (settings.serverUrl.isBlank()) {
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(error = null, isPageHistoryLoading = true) }

            runCatching {
                repository.fetchPageHistory(
                    url = settings.serverUrl,
                    pagePath = pagePath,
                    scopePrefix = settings.scopePrefix,
                    bearerToken = settings.bearerToken,
                    username = settings.username,
                    password = settings.password,
                ).revisions
            }.onSuccess { revisions ->
                _uiState.update { current ->
                    if (normalizePagePath(current.openPagePath.orEmpty()).equals(normalizePagePath(pagePath), ignoreCase = true)) {
                        current.copy(
                            openPageHistory = revisions,
                            isPageHistoryLoading = false,
                        )
                    } else {
                        current
                    }
                }
            }.onFailure { error ->
                if (error is CancellationException) return@onFailure
                _uiState.update {
                    it.copy(
                        isPageHistoryLoading = false,
                        error = error.message ?: "Page history could not be loaded.",
                    )
                }
            }
        }
    }

    fun restoreOpenPageHistory(
        revisionId: String,
        onResult: (Boolean) -> Unit = {},
    ) {
        val pagePath = _uiState.value.openPagePath ?: run {
            onResult(false)
            return
        }
        val settings = _uiState.value.settings
        if (settings.serverUrl.isBlank() || revisionId.isBlank()) {
            onResult(false)
            return
        }
        val normalizedPagePath = applyScopePrefixToPagePath(pagePath, settings.scopePrefix)
        if (normalizedPagePath.isBlank()) {
            onResult(false)
            return
        }
        val activeSearchQuery = _uiState.value.searchResults?.query?.takeIf(String::isNotBlank)

        viewModelScope.launch {
            _uiState.update { it.copy(error = null, isPageHistoryBusy = true) }

            runCatching {
                val restoredDetail = repository.restorePageHistory(
                    url = settings.serverUrl,
                    pagePath = normalizedPagePath,
                    revisionId = revisionId,
                    scopePrefix = settings.scopePrefix,
                    bearerToken = settings.bearerToken,
                    username = settings.username,
                    password = settings.password,
                )
                val mutation = buildPageMutationResult(
                    settings = settings,
                    pagePath = normalizedPagePath,
                    detail = restoredDetail,
                )
                val revisions = runCatching {
                    repository.fetchPageHistory(
                        url = settings.serverUrl,
                        pagePath = normalizedPagePath,
                        scopePrefix = settings.scopePrefix,
                        bearerToken = settings.bearerToken,
                        username = settings.username,
                        password = settings.password,
                    ).revisions
                }.getOrDefault(emptyList())
                PageHistoryRestoreResult(
                    mutation = mutation,
                    revisions = revisions,
                )
            }.onSuccess { result ->
                _uiState.update { current ->
                    current.copy(
                        openPagePath = normalizedPagePath,
                        openPageContent = result.mutation.rawMarkdown,
                        openPageFrontmatter = result.mutation.frontmatter,
                        openPageTasks = result.mutation.pageTasks,
                        openPageDerived = result.mutation.derived,
                        openPageHistory = result.revisions,
                        isPageLoading = false,
                        isPageHistoryLoading = false,
                        isPageHistoryBusy = false,
                        pages = result.mutation.pages ?: current.pages,
                        folders = result.mutation.folders ?: current.folders,
                        tasks = result.mutation.tasks ?: current.tasks,
                        today = result.mutation.today ?: current.today,
                    )
                }
                activeSearchQuery?.let(::search)
                onResult(true)
            }.onFailure { error ->
                if (error is CancellationException) return@onFailure
                _uiState.update {
                    it.copy(
                        isPageHistoryBusy = false,
                        error = error.message ?: "Revision could not be restored.",
                    )
                }
                onResult(false)
            }
        }
    }

    fun purgeOpenPageHistory(
        onResult: (Boolean) -> Unit = {},
    ) {
        val pagePath = _uiState.value.openPagePath ?: run {
            onResult(false)
            return
        }
        val settings = _uiState.value.settings
        if (settings.serverUrl.isBlank()) {
            onResult(false)
            return
        }
        val normalizedPagePath = applyScopePrefixToPagePath(pagePath, settings.scopePrefix)
        if (normalizedPagePath.isBlank()) {
            onResult(false)
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(error = null, isPageHistoryBusy = true) }

            runCatching {
                repository.deletePageHistory(
                    url = settings.serverUrl,
                    pagePath = normalizedPagePath,
                    scopePrefix = settings.scopePrefix,
                    bearerToken = settings.bearerToken,
                    username = settings.username,
                    password = settings.password,
                )
            }.onSuccess {
                _uiState.update { current ->
                    if (normalizePagePath(current.openPagePath.orEmpty()).equals(normalizePagePath(normalizedPagePath), ignoreCase = true)) {
                        current.copy(
                            openPageHistory = emptyList(),
                            isPageHistoryLoading = false,
                            isPageHistoryBusy = false,
                        )
                    } else {
                        current.copy(
                            isPageHistoryLoading = false,
                            isPageHistoryBusy = false,
                        )
                    }
                }
                onResult(true)
            }.onFailure { error ->
                if (error is CancellationException) return@onFailure
                _uiState.update {
                    it.copy(
                        isPageHistoryBusy = false,
                        error = error.message ?: "Page history could not be purged.",
                    )
                }
                onResult(false)
            }
        }
    }

    fun fetchTrashPages() {
        val settings = _uiState.value.settings
        if (settings.serverUrl.isBlank()) {
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(error = null, isTrashLoading = true) }

            runCatching {
                repository.fetchTrashPages(
                    url = settings.serverUrl,
                    scopePrefix = settings.scopePrefix,
                    bearerToken = settings.bearerToken,
                    username = settings.username,
                    password = settings.password,
                ).pages
            }.onSuccess { pages ->
                _uiState.update {
                    it.copy(
                        trashPages = pages,
                        isTrashLoading = false,
                    )
                }
            }.onFailure { error ->
                if (error is CancellationException) return@onFailure
                _uiState.update {
                    it.copy(
                        isTrashLoading = false,
                        error = error.message ?: "Trash could not be loaded.",
                    )
                }
            }
        }
    }

    fun restoreTrashPage(
        pagePath: String,
        onResult: (Boolean) -> Unit = {},
    ) {
        val settings = _uiState.value.settings
        if (settings.serverUrl.isBlank()) {
            onResult(false)
            return
        }
        val normalizedPagePath = applyScopePrefixToPagePath(pagePath, settings.scopePrefix)
        if (normalizedPagePath.isBlank()) {
            onResult(false)
            return
        }
        val activeSearchQuery = _uiState.value.searchResults?.query?.takeIf(String::isNotBlank)

        viewModelScope.launch {
            _uiState.update { it.copy(error = null, isTrashBusy = true) }

            runCatching {
                val restoredDetail = repository.restoreTrashPage(
                    url = settings.serverUrl,
                    pagePath = normalizedPagePath,
                    scopePrefix = settings.scopePrefix,
                    bearerToken = settings.bearerToken,
                    username = settings.username,
                    password = settings.password,
                )
                val mutation = buildPageMutationResult(
                    settings = settings,
                    pagePath = normalizedPagePath,
                    detail = restoredDetail,
                )
                val trashPages = runCatching {
                    repository.fetchTrashPages(
                        url = settings.serverUrl,
                        scopePrefix = settings.scopePrefix,
                        bearerToken = settings.bearerToken,
                        username = settings.username,
                        password = settings.password,
                    ).pages
                }.getOrDefault(emptyList())
                TrashRestoreResult(
                    mutation = mutation,
                    trashPages = trashPages,
                )
            }.onSuccess { result ->
                val currentPagePath = _uiState.value.openPagePath
                if (
                    currentPagePath != null &&
                    !normalizePagePath(currentPagePath).equals(normalizedPagePath, ignoreCase = true)
                ) {
                    openPageBackStack.addLast(currentPagePath)
                }
                _uiState.update { current ->
                    current.copy(
                        openPagePath = normalizedPagePath,
                        openPageContent = result.mutation.rawMarkdown,
                        openPageFrontmatter = result.mutation.frontmatter,
                        openPageTasks = result.mutation.pageTasks,
                        openPageDerived = result.mutation.derived,
                        openPageHistory = emptyList(),
                        isPageLoading = false,
                        isPageHistoryLoading = false,
                        isPageHistoryBusy = false,
                        trashPages = result.trashPages,
                        isTrashLoading = false,
                        isTrashBusy = false,
                        pages = result.mutation.pages ?: current.pages,
                        folders = result.mutation.folders ?: current.folders,
                        tasks = result.mutation.tasks ?: current.tasks,
                        today = result.mutation.today ?: current.today,
                    )
                }
                activeSearchQuery?.let(::search)
                onResult(true)
            }.onFailure { error ->
                if (error is CancellationException) return@onFailure
                _uiState.update {
                    it.copy(
                        isTrashBusy = false,
                        error = error.message ?: "Trashed page could not be restored.",
                    )
                }
                onResult(false)
            }
        }
    }

    fun permanentlyDeleteTrashPage(
        pagePath: String,
        onResult: (Boolean) -> Unit = {},
    ) {
        val settings = _uiState.value.settings
        if (settings.serverUrl.isBlank()) {
            onResult(false)
            return
        }
        val normalizedPagePath = applyScopePrefixToPagePath(pagePath, settings.scopePrefix)
        if (normalizedPagePath.isBlank()) {
            onResult(false)
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(error = null, isTrashBusy = true) }

            runCatching {
                repository.permanentlyDeleteTrashPage(
                    url = settings.serverUrl,
                    pagePath = normalizedPagePath,
                    scopePrefix = settings.scopePrefix,
                    bearerToken = settings.bearerToken,
                    username = settings.username,
                    password = settings.password,
                )
            }.onSuccess {
                remapPageReferencesInBackStack(
                    fromPagePath = normalizedPagePath,
                    toPagePath = null,
                )
                _uiState.update { current ->
                    current.copy(
                        trashPages = current.trashPages.filterNot { trashPage ->
                            normalizePagePath(trashPage.page).equals(normalizedPagePath, ignoreCase = true)
                        },
                        isTrashLoading = false,
                        isTrashBusy = false,
                    )
                }
                onResult(true)
            }.onFailure { error ->
                if (error is CancellationException) return@onFailure
                _uiState.update {
                    it.copy(
                        isTrashBusy = false,
                        error = error.message ?: "Trashed page could not be deleted.",
                    )
                }
                onResult(false)
            }
        }
    }

    fun emptyTrash(
        onResult: (Boolean) -> Unit = {},
    ) {
        val settings = _uiState.value.settings
        if (settings.serverUrl.isBlank()) {
            onResult(false)
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(error = null, isTrashBusy = true) }

            runCatching {
                repository.emptyTrash(
                    url = settings.serverUrl,
                    scopePrefix = settings.scopePrefix,
                    bearerToken = settings.bearerToken,
                    username = settings.username,
                    password = settings.password,
                )
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        trashPages = emptyList(),
                        isTrashLoading = false,
                        isTrashBusy = false,
                    )
                }
                onResult(true)
            }.onFailure { error ->
                if (error is CancellationException) return@onFailure
                _uiState.update {
                    it.copy(
                        isTrashBusy = false,
                        error = error.message ?: "Trash could not be emptied.",
                    )
                }
                onResult(false)
            }
        }
    }

    fun handleDeepLink(uri: Uri?) {
        val pagePath = when {
            uri == null -> ""
            !uri.scheme.equals("noterious", ignoreCase = true) -> ""
            !uri.host.equals("open", ignoreCase = true) -> ""
            else -> normalizePagePath(uri.getQueryParameter("page").orEmpty())
        }
        if (pagePath.isBlank()) {
            return
        }

        pendingDeepLinkPagePath = pagePath
        consumePendingDeepLinkIfPossible()
    }

    fun closePage() {
        val previousPage = openPageBackStack.removeLastOrNull()
        if (previousPage != null) {
            openPage(previousPage, addToBackStack = false)
            return
        }
        openPageJob?.cancel()
        _uiState.update {
            it.copy(
                openPagePath = null,
                openPageContent = null,
                openPageFrontmatter = null,
                openPageTasks = emptyList(),
                openPageDerived = null,
                openPageHistory = emptyList(),
                isPageLoading = false,
                isPageSaving = false,
                isPageHistoryLoading = false,
                isPageHistoryBusy = false,
            )
        }
    }

    fun closeOpenPageStack() {
        openPageBackStack.clear()
        openPageJob?.cancel()
        _uiState.update {
            it.copy(
                openPagePath = null,
                openPageContent = null,
                openPageFrontmatter = null,
                openPageTasks = emptyList(),
                openPageDerived = null,
                openPageHistory = emptyList(),
                isPageLoading = false,
                isPageSaving = false,
                isPageHistoryLoading = false,
                isPageHistoryBusy = false,
            )
        }
    }

    fun saveOpenPage(markdown: String, baseMarkdown: String, onResult: (Boolean) -> Unit = {}) {
        val pagePath = _uiState.value.openPagePath ?: run {
            onResult(false)
            return
        }
        val settings = _uiState.value.settings
        if (settings.serverUrl.isBlank()) {
            onResult(false)
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isPageSaving = true, error = null) }

            runCatching {
                val savedDetail = repository.savePage(
                    url = settings.serverUrl,
                    pagePath = pagePath,
                    rawMarkdown = markdown,
                    baseRawMarkdown = baseMarkdown,
                    scopePrefix = settings.scopePrefix,
                    bearerToken = settings.bearerToken,
                    username = settings.username,
                    password = settings.password,
                )
                val derived = runCatching {
                    repository.fetchDerivedPage(
                        url = settings.serverUrl,
                        pagePath = pagePath,
                        scopePrefix = settings.scopePrefix,
                        bearerToken = settings.bearerToken,
                        username = settings.username,
                        password = settings.password,
                    )
                }.getOrNull()
                val pages = runCatching {
                    repository.fetchPages(
                        url = settings.serverUrl,
                        scopePrefix = settings.scopePrefix,
                        bearerToken = settings.bearerToken,
                        username = settings.username,
                        password = settings.password,
                    )
                }.getOrNull()
                val folders = runCatching {
                    repository.fetchFolders(
                        url = settings.serverUrl,
                        scopePrefix = settings.scopePrefix,
                        bearerToken = settings.bearerToken,
                        username = settings.username,
                        password = settings.password,
                    )
                }.getOrNull()
                val tasks = runCatching {
                    repository.fetchTasks(
                        url = settings.serverUrl,
                        scopePrefix = settings.scopePrefix,
                        bearerToken = settings.bearerToken,
                        username = settings.username,
                        password = settings.password,
                    )
                }.getOrNull()
                SavePageResult(
                    rawMarkdown = savedDetail.rawMarkdown,
                    frontmatter = savedDetail.frontmatter,
                    pageTasks = savedDetail.tasks,
                    derived = derived,
                    pages = pages,
                    folders = folders,
                    tasks = tasks,
                    today = tasks?.let(repository::buildTodaySnapshot),
                )
            }.onSuccess { result ->
                _uiState.update { current ->
                    current.copy(
                        openPageContent = if (current.openPagePath == pagePath) result.rawMarkdown else current.openPageContent,
                        openPageFrontmatter = if (current.openPagePath == pagePath) result.frontmatter else current.openPageFrontmatter,
                        openPageTasks = if (current.openPagePath == pagePath) result.pageTasks else current.openPageTasks,
                        openPageDerived = if (current.openPagePath == pagePath) result.derived else current.openPageDerived,
                        isPageSaving = false,
                        pages = result.pages ?: current.pages,
                        folders = result.folders ?: current.folders,
                        tasks = result.tasks ?: current.tasks,
                        today = result.today ?: current.today,
                    )
                }
                onResult(true)
            }.onFailure { error ->
                if (error is CancellationException) return@onFailure
                _uiState.update {
                    it.copy(isPageSaving = false, error = error.message ?: "Page could not be saved.")
                }
                onResult(false)
            }
        }
    }

    fun uploadDocumentForOpenPage(uri: Uri, onResult: (DocumentRecord?) -> Unit = {}) {
        val pagePath = _uiState.value.openPagePath ?: run {
            onResult(null)
            return
        }
        val settings = _uiState.value.settings
        if (settings.serverUrl.isBlank()) {
            onResult(null)
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            runCatching {
                val upload = withContext(Dispatchers.IO) {
                    readUploadSpec(uri)
                }
                repository.uploadDocument(
                    url = settings.serverUrl,
                    pagePath = pagePath,
                    fileName = upload.fileName,
                    contentType = upload.contentType,
                    content = upload.content,
                    scopePrefix = settings.scopePrefix,
                    bearerToken = settings.bearerToken,
                    username = settings.username,
                    password = settings.password,
                )
            }.onSuccess { document ->
                _uiState.update { current ->
                    current.copy(
                        documents = listOf(document) + current.documents.filterNot { existing ->
                            existing.id == document.id || normalizePagePath(existing.path).equals(normalizePagePath(document.path), ignoreCase = true)
                        },
                    )
                }
                onResult(document)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(error = error.message ?: "File could not be uploaded.")
                }
                onResult(null)
            }
        }
    }

    fun patchOpenPageTask(
        taskRef: String,
        text: String? = null,
        state: String? = null,
        due: String? = null,
        remind: String? = null,
        click: String? = null,
        onResult: (Boolean) -> Unit = {},
    ) {
        val pagePath = _uiState.value.openPagePath ?: run {
            onResult(false)
            return
        }
        val settings = _uiState.value.settings
        if (settings.serverUrl.isBlank()) {
            onResult(false)
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }

            runCatching {
                repository.patchTask(
                    url = settings.serverUrl,
                    taskRef = taskRef,
                    scopePrefix = settings.scopePrefix,
                    bearerToken = settings.bearerToken,
                    username = settings.username,
                    password = settings.password,
                    text = text,
                    state = state,
                    due = due,
                    remind = remind,
                    click = click,
                )
                val detail = repository.fetchPageDetail(
                    url = settings.serverUrl,
                    pagePath = pagePath,
                    scopePrefix = settings.scopePrefix,
                    bearerToken = settings.bearerToken,
                    username = settings.username,
                    password = settings.password,
                )
                val derived = runCatching {
                    repository.fetchDerivedPage(
                        url = settings.serverUrl,
                        pagePath = pagePath,
                        scopePrefix = settings.scopePrefix,
                        bearerToken = settings.bearerToken,
                        username = settings.username,
                        password = settings.password,
                    )
                }.getOrNull()
                val tasks = runCatching {
                    repository.fetchTasks(
                        url = settings.serverUrl,
                        scopePrefix = settings.scopePrefix,
                        bearerToken = settings.bearerToken,
                        username = settings.username,
                        password = settings.password,
                    )
                }.getOrNull()
                PatchedTaskResult(
                    rawMarkdown = detail.rawMarkdown,
                    frontmatter = detail.frontmatter,
                    pageTasks = detail.tasks,
                    derived = derived,
                    tasks = tasks,
                    today = tasks?.let(repository::buildTodaySnapshot),
                )
            }.onSuccess { result ->
                _uiState.update { current ->
                    current.copy(
                        openPageContent = if (current.openPagePath == pagePath) result.rawMarkdown else current.openPageContent,
                        openPageFrontmatter = if (current.openPagePath == pagePath) result.frontmatter else current.openPageFrontmatter,
                        openPageTasks = if (current.openPagePath == pagePath) result.pageTasks else current.openPageTasks,
                        openPageDerived = if (current.openPagePath == pagePath) result.derived else current.openPageDerived,
                        tasks = result.tasks ?: current.tasks,
                        today = result.today ?: current.today,
                    )
                }
                onResult(true)
            }.onFailure { error ->
                if (error is CancellationException) return@onFailure
                _uiState.update {
                    it.copy(error = error.message ?: "Task could not be updated.")
                }
                onResult(false)
            }
        }
    }

    fun patchOpenPageFrontmatter(
        set: Map<String, JsonElement> = emptyMap(),
        remove: List<String> = emptyList(),
        onResult: (Boolean) -> Unit = {},
    ) {
        val pagePath = _uiState.value.openPagePath ?: run {
            onResult(false)
            return
        }
        val settings = _uiState.value.settings
        if (settings.serverUrl.isBlank()) {
            onResult(false)
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }

            runCatching {
                val detail = repository.patchPageFrontmatter(
                    url = settings.serverUrl,
                    pagePath = pagePath,
                    scopePrefix = settings.scopePrefix,
                    bearerToken = settings.bearerToken,
                    username = settings.username,
                    password = settings.password,
                    set = set,
                    remove = remove,
                )
                val derived = runCatching {
                    repository.fetchDerivedPage(
                        url = settings.serverUrl,
                        pagePath = pagePath,
                        scopePrefix = settings.scopePrefix,
                        bearerToken = settings.bearerToken,
                        username = settings.username,
                        password = settings.password,
                    )
                }.getOrNull()
                val pages = runCatching {
                    repository.fetchPages(
                        url = settings.serverUrl,
                        scopePrefix = settings.scopePrefix,
                        bearerToken = settings.bearerToken,
                        username = settings.username,
                        password = settings.password,
                    )
                }.getOrNull()
                val folders = runCatching {
                    repository.fetchFolders(
                        url = settings.serverUrl,
                        scopePrefix = settings.scopePrefix,
                        bearerToken = settings.bearerToken,
                        username = settings.username,
                        password = settings.password,
                    )
                }.getOrNull()
                val tasks = runCatching {
                    repository.fetchTasks(
                        url = settings.serverUrl,
                        scopePrefix = settings.scopePrefix,
                        bearerToken = settings.bearerToken,
                        username = settings.username,
                        password = settings.password,
                    )
                }.getOrNull()
                SavePageResult(
                    rawMarkdown = detail.rawMarkdown,
                    frontmatter = detail.frontmatter,
                    pageTasks = detail.tasks,
                    derived = derived,
                    pages = pages,
                    folders = folders,
                    tasks = tasks,
                    today = tasks?.let(repository::buildTodaySnapshot),
                )
            }.onSuccess { result ->
                _uiState.update { current ->
                    current.copy(
                        openPageContent = if (current.openPagePath == pagePath) result.rawMarkdown else current.openPageContent,
                        openPageFrontmatter = if (current.openPagePath == pagePath) result.frontmatter else current.openPageFrontmatter,
                        openPageTasks = if (current.openPagePath == pagePath) result.pageTasks else current.openPageTasks,
                        openPageDerived = if (current.openPagePath == pagePath) result.derived else current.openPageDerived,
                        pages = result.pages ?: current.pages,
                        folders = result.folders ?: current.folders,
                        tasks = result.tasks ?: current.tasks,
                        today = result.today ?: current.today,
                    )
                }
                onResult(true)
            }.onFailure { error ->
                if (error is CancellationException) return@onFailure
                _uiState.update {
                    it.copy(error = error.message ?: "Frontmatter could not be updated.")
                }
                onResult(false)
            }
        }
    }

    fun patchTask(
        taskRef: String,
        text: String? = null,
        state: String? = null,
        due: String? = null,
        remind: String? = null,
        click: String? = null,
        onResult: (Boolean) -> Unit = {},
    ) {
        val settings = _uiState.value.settings
        if (settings.serverUrl.isBlank()) {
            onResult(false)
            return
        }
        val activeSearchQuery = _uiState.value.searchResults?.query?.takeIf(String::isNotBlank)

        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }

            runCatching {
                repository.patchTask(
                    url = settings.serverUrl,
                    taskRef = taskRef,
                    scopePrefix = settings.scopePrefix,
                    bearerToken = settings.bearerToken,
                    username = settings.username,
                    password = settings.password,
                    text = text,
                    state = state,
                    due = due,
                    remind = remind,
                    click = click,
                )
            }.onSuccess {
                refresh(trigger = "task-change")
                activeSearchQuery?.let(::search)
                onResult(true)
            }.onFailure { error ->
                if (error is CancellationException) return@onFailure
                _uiState.update {
                    it.copy(error = error.message ?: "Task could not be updated.")
                }
                onResult(false)
            }
        }
    }

    fun deleteTask(
        taskRef: String,
        onResult: (Boolean) -> Unit = {},
    ) {
        val settings = _uiState.value.settings
        if (settings.serverUrl.isBlank()) {
            onResult(false)
            return
        }
        val activeSearchQuery = _uiState.value.searchResults?.query?.takeIf(String::isNotBlank)

        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }

            runCatching {
                repository.deleteTask(
                    url = settings.serverUrl,
                    taskRef = taskRef,
                    scopePrefix = settings.scopePrefix,
                    bearerToken = settings.bearerToken,
                    username = settings.username,
                    password = settings.password,
                )
            }.onSuccess {
                refresh(trigger = "task-delete")
                activeSearchQuery?.let(::search)
                onResult(true)
            }.onFailure { error ->
                if (error is CancellationException) return@onFailure
                _uiState.update {
                    it.copy(error = error.message ?: "Task could not be deleted.")
                }
                onResult(false)
            }
        }
    }

    private var searchJob: Job? = null

    fun search(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = null, isSearching = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(300)
            val settings = _uiState.value.settings
            if (settings.serverUrl.isBlank()) return@launch
            _uiState.update { it.copy(isSearching = true) }
            runCatching {
                repository.search(
                    url = settings.serverUrl,
                    query = query,
                    scopePrefix = settings.scopePrefix,
                    bearerToken = settings.bearerToken,
                    username = settings.username,
                    password = settings.password,
                )
            }.onSuccess { results ->
                _uiState.update { it.copy(searchResults = results, isSearching = false) }
            }.onFailure { error ->
                if (error is CancellationException) return@onFailure
                _uiState.update { it.copy(isSearching = false, error = error.message ?: "Search failed.") }
            }
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _uiState.update { it.copy(searchResults = null, isSearching = false) }
    }

    fun loadSavedQuery(
        name: String,
        onResult: (SavedQueryRecord?) -> Unit = {},
    ) {
        val trimmedName = name.trim()
        val settings = _uiState.value.settings
        if (trimmedName.isBlank() || settings.serverUrl.isBlank()) {
            onResult(null)
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            runCatching {
                repository.fetchSavedQuery(
                    url = settings.serverUrl,
                    name = trimmedName,
                    scopePrefix = settings.scopePrefix,
                    bearerToken = settings.bearerToken,
                    username = settings.username,
                    password = settings.password,
                )
            }.onSuccess { savedQuery ->
                onResult(savedQuery)
            }.onFailure { error ->
                if (error is CancellationException) return@onFailure
                _uiState.update {
                    it.copy(error = error.message ?: "Saved query could not be loaded.")
                }
                onResult(null)
            }
        }
    }

    fun generateQueryCopilot(
        intent: String,
        currentQuery: String = "",
        onResult: (QueryCopilotResponse?) -> Unit = {},
    ) {
        val trimmedIntent = intent.trim()
        val settings = _uiState.value.settings
        if (trimmedIntent.isBlank() || settings.serverUrl.isBlank()) {
            onResult(null)
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            runCatching {
                repository.generateQueryCopilot(
                    url = settings.serverUrl,
                    intent = trimmedIntent,
                    currentQuery = currentQuery.trim(),
                    previewLimit = 10,
                    scopePrefix = settings.scopePrefix,
                    bearerToken = settings.bearerToken,
                    username = settings.username,
                    password = settings.password,
                )
            }.onSuccess { response ->
                onResult(response)
            }.onFailure { error ->
                if (error is CancellationException) return@onFailure
                _uiState.update {
                    it.copy(error = error.message ?: "AI query generation failed.")
                }
                onResult(null)
            }
        }
    }

    fun runQueryWorkbench(query: String, previewLimit: Int = 20) {
        queryWorkbenchJob?.cancel()
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank()) {
            _uiState.update {
                it.copy(
                    queryWorkbench = null,
                    lastExecutedQuery = "",
                    isQueryWorkbenchLoading = false,
                )
            }
            return
        }

        val settings = _uiState.value.settings
        if (settings.serverUrl.isBlank()) {
            _uiState.update { it.copy(error = "Configure the server URL first.") }
            return
        }

        queryWorkbenchJob = viewModelScope.launch {
            _uiState.update { it.copy(isQueryWorkbenchLoading = true, error = null) }
            runCatching {
                repository.runQueryWorkbench(
                    url = settings.serverUrl,
                    query = trimmedQuery,
                    previewLimit = previewLimit,
                    scopePrefix = settings.scopePrefix,
                    bearerToken = settings.bearerToken,
                    username = settings.username,
                    password = settings.password,
                )
            }.onSuccess { result ->
                _uiState.update {
                    it.copy(
                        queryWorkbench = result,
                        lastExecutedQuery = trimmedQuery,
                        isQueryWorkbenchLoading = false,
                    )
                }
            }.onFailure { error ->
                if (error is CancellationException) return@onFailure
                _uiState.update {
                    it.copy(
                        isQueryWorkbenchLoading = false,
                        error = error.message ?: "Query could not be executed.",
                    )
                }
            }
        }
    }

    fun clearQueryWorkbench() {
        queryWorkbenchJob?.cancel()
        _uiState.update {
            it.copy(
                queryWorkbench = null,
                lastExecutedQuery = "",
                isQueryWorkbenchLoading = false,
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun reloadOpenPageIfNeeded() {
        val pagePath = _uiState.value.openPagePath ?: return
        openPage(pagePath, addToBackStack = false)
    }

    private fun consumePendingDeepLinkIfPossible() {
        val pagePath = pendingDeepLinkPagePath ?: return
        val settings = _uiState.value.settings
        if (!settingsLoaded || settings.serverUrl.isBlank()) {
            return
        }

        pendingDeepLinkPagePath = null
        openPageBackStack.clear()
        openPage(pagePath, addToBackStack = false)
    }

    private fun reconcileEventStream() {
        val settings = _uiState.value.settings
        val streamKey = eventStreamKey(settings)
        if (!appInForeground || streamKey == null) {
            stopEventStream()
            return
        }
        if (eventStreamJob?.isActive == true && activeEventStreamKey == streamKey) return

        stopEventStream()
        activeEventStreamKey = streamKey
        eventStreamJob = viewModelScope.launch {
            _uiState.update { it.copy(isLiveSyncConnected = false, liveSyncError = null) }
            while (isActive && activeEventStreamKey == streamKey) {
                val currentSettings = _uiState.value.settings
                runCatching {
                    repository.consumeEvents(
                        url = currentSettings.serverUrl,
                        scopePrefix = currentSettings.scopePrefix,
                        bearerToken = currentSettings.bearerToken,
                        username = currentSettings.username,
                        password = currentSettings.password,
                        onConnected = {
                            _uiState.update { it.copy(isLiveSyncConnected = true, liveSyncError = null) }
                        },
                    ) { event ->
                        handleLiveEvent(event)
                    }
                }.onFailure { error ->
                    if (!isActive || activeEventStreamKey != streamKey) return@onFailure
                    _uiState.update {
                        it.copy(isLiveSyncConnected = false, liveSyncError = error.message ?: "Live-Updates getrennt.")
                    }
                }
                _uiState.update { it.copy(isLiveSyncConnected = false) }
                if (!isActive || activeEventStreamKey != streamKey) break
                delay(2_000)
            }
        }
    }

    private fun stopEventStream() {
        activeEventStreamKey = null
        repository.closeEventStream()
        eventStreamJob?.cancel()
        eventStreamJob = null
        pendingLiveRefreshJob?.cancel()
        pendingLiveRefreshJob = null
        _uiState.update { it.copy(isLiveSyncConnected = false, liveSyncError = null) }
    }

    private fun handleLiveEvent(event: NoteriousRepository.ServerEvent) {
        when (event.type) {
            "page.changed",
            "page.deleted",
            "task.changed",
            "task.deleted",
            "derived.changed",
            "query.changed",
            "query-block.changed"
            -> scheduleLiveRefresh()
        }
    }

    private fun scheduleLiveRefresh() {
        pendingLiveRefreshJob?.cancel()
        pendingLiveRefreshJob = viewModelScope.launch {
            delay(750)
            if (!_uiState.value.isLoading) {
                refresh(trigger = "sse")
            }
        }
    }

    private fun eventStreamKey(settings: AppSettings): String? {
        if (settings.serverUrl.isBlank() || !canAttemptBackgroundSync(settings)) return null
        return listOf(
            settings.serverUrl.trim(),
            settings.scopePrefix.trim(),
            settings.username.trim(),
            settings.password,
            settings.bearerToken.trim(),
        ).joinToString("\u0000")
    }

    private suspend fun fetchCollectionsSnapshot(settings: AppSettings): CollectionsRefreshResult {
        val pages = runCatching {
            repository.fetchPages(
                url = settings.serverUrl,
                scopePrefix = settings.scopePrefix,
                bearerToken = settings.bearerToken,
                username = settings.username,
                password = settings.password,
            )
        }.getOrNull()
        val folders = runCatching {
            repository.fetchFolders(
                url = settings.serverUrl,
                scopePrefix = settings.scopePrefix,
                bearerToken = settings.bearerToken,
                username = settings.username,
                password = settings.password,
            )
        }.getOrNull()
        val tasks = runCatching {
            repository.fetchTasks(
                url = settings.serverUrl,
                scopePrefix = settings.scopePrefix,
                bearerToken = settings.bearerToken,
                username = settings.username,
                password = settings.password,
            )
        }.getOrNull()
        return CollectionsRefreshResult(
            pages = pages,
            folders = folders,
            tasks = tasks,
            today = tasks?.let(repository::buildTodaySnapshot),
        )
    }

    private suspend fun fetchCollectionsSnapshotForSetup(settings: AppSettings): CollectionsRefreshResult {
        val pages = repository.fetchPages(
            url = settings.serverUrl,
            scopePrefix = settings.scopePrefix,
            bearerToken = settings.bearerToken,
            username = settings.username,
            password = settings.password,
        )
        val folders = runCatching {
            repository.fetchFolders(
                url = settings.serverUrl,
                scopePrefix = settings.scopePrefix,
                bearerToken = settings.bearerToken,
                username = settings.username,
                password = settings.password,
            )
        }.getOrNull()
        val tasks = runCatching {
            repository.fetchTasks(
                url = settings.serverUrl,
                scopePrefix = settings.scopePrefix,
                bearerToken = settings.bearerToken,
                username = settings.username,
                password = settings.password,
            )
        }.getOrNull()
        return CollectionsRefreshResult(
            pages = pages,
            folders = folders,
            tasks = tasks,
            today = tasks?.let(repository::buildTodaySnapshot),
        )
    }

    private suspend fun fetchVaultsSnapshot(settings: AppSettings): List<VaultRecord>? {
        return runCatching {
            repository.fetchVaults(
                url = settings.serverUrl,
                bearerToken = settings.bearerToken,
                username = settings.username,
                password = settings.password,
            )
        }.getOrNull()
    }

    private suspend fun fetchDocumentsSnapshot(settings: AppSettings): List<DocumentRecord>? {
        return runCatching {
            repository.fetchDocuments(
                url = settings.serverUrl,
                scopePrefix = settings.scopePrefix,
                bearerToken = settings.bearerToken,
                username = settings.username,
                password = settings.password,
            )
        }.getOrNull()
    }

    private fun templatePagesInventoryKey(settings: AppSettings): String {
        return listOf(
            settings.serverUrl.trim(),
            settings.username.trim(),
            settings.password,
            settings.bearerToken.trim(),
        ).joinToString("\u0000")
    }

    private fun themeInventoryKey(settings: AppSettings): String {
        return listOf(
            settings.serverUrl.trim(),
            settings.username.trim(),
            settings.password,
            settings.bearerToken.trim(),
        ).joinToString("\u0000")
    }

    private fun settingsDetailsInventoryKey(settings: AppSettings): String {
        return listOf(
            settings.serverUrl.trim(),
            settings.scopePrefix.trim(),
            settings.username.trim(),
            settings.password,
            settings.bearerToken.trim(),
        ).joinToString("\u0000")
    }

    private fun canAttemptBackgroundSync(settings: AppSettings): Boolean {
        return settings.bearerToken.isNotBlank() ||
            settings.username.isBlank() ||
            settings.password.isNotBlank()
    }

    private fun initialSetupErrorMessage(
        error: Throwable,
        username: String,
        password: String,
        bearerToken: String,
    ): String {
        val rawMessage = error.message.orEmpty()
        return when {
            rawMessage.contains("HTTP 401") ||
                rawMessage.contains("HTTP 403") ||
                rawMessage.startsWith("Login failed:") ||
                rawMessage.startsWith("API login failed:") -> when {
                bearerToken.isNotBlank() -> "Bearer token was rejected. Check it and try again."
                username.isBlank() && password.isBlank() -> "This server requires authentication. Enter username and password or a bearer token."
                else -> "Username or password was rejected. Check them and try again."
            }

            rawMessage.isNotBlank() -> rawMessage
            else -> "Connection failed."
        }
    }

    private suspend fun buildPageMutationResult(
        settings: AppSettings,
        pagePath: String,
        detail: ApiPageDetail,
    ): PageMutationResult {
        val derived = runCatching {
            repository.fetchDerivedPage(
                url = settings.serverUrl,
                pagePath = pagePath,
                scopePrefix = settings.scopePrefix,
                bearerToken = settings.bearerToken,
                username = settings.username,
                password = settings.password,
            )
        }.getOrNull()
        val collections = fetchCollectionsSnapshot(settings)
        return PageMutationResult(
            rawMarkdown = detail.rawMarkdown,
            frontmatter = detail.frontmatter,
            pageTasks = detail.tasks,
            derived = derived,
            pages = collections.pages,
            folders = collections.folders,
            tasks = collections.tasks,
            today = collections.today,
        )
    }

    private fun isPageInsideFolder(pagePath: String, folderPath: String): Boolean {
        val normalizedPagePath = normalizePagePath(pagePath)
        val normalizedFolderPath = normalizePagePath(folderPath)
        if (normalizedPagePath.isBlank() || normalizedFolderPath.isBlank()) {
            return false
        }
        return normalizedPagePath.startsWith("$normalizedFolderPath/")
    }

    private fun remapPagePathInsideFolder(
        pagePath: String,
        fromFolderPath: String,
        toFolderPath: String?,
    ): String? {
        val normalizedPagePath = normalizePagePath(pagePath)
        val normalizedFromFolderPath = normalizePagePath(fromFolderPath)
        if (!isPageInsideFolder(normalizedPagePath, normalizedFromFolderPath)) {
            return null
        }
        val relativeSuffix = normalizedPagePath.removePrefix("$normalizedFromFolderPath/")
        val normalizedTargetFolderPath = toFolderPath?.let(::normalizePagePath).orEmpty()
        return when {
            normalizedTargetFolderPath.isBlank() -> relativeSuffix
            relativeSuffix.isBlank() -> normalizedTargetFolderPath
            else -> "$normalizedTargetFolderPath/$relativeSuffix"
        }
    }

    private fun remapPageReferencesInBackStack(fromPagePath: String, toPagePath: String?) {
        val normalizedFromPagePath = normalizePagePath(fromPagePath)
        val normalizedToPagePath = toPagePath?.let(::normalizePagePath).orEmpty()
        if (normalizedFromPagePath.isBlank()) return

        val remappedEntries = openPageBackStack.mapNotNull { pagePath ->
            if (normalizePagePath(pagePath).equals(normalizedFromPagePath, ignoreCase = true)) {
                normalizedToPagePath.takeIf(String::isNotBlank)
            } else {
                pagePath
            }
        }
        openPageBackStack.clear()
        remappedEntries.forEach(openPageBackStack::addLast)
    }

    private fun remapFolderReferencesInBackStack(fromFolderPath: String, toFolderPath: String?) {
        val normalizedFromFolderPath = normalizePagePath(fromFolderPath)
        if (normalizedFromFolderPath.isBlank()) return

        val remappedEntries = openPageBackStack.mapNotNull { pagePath ->
            remapPagePathInsideFolder(
                pagePath = pagePath,
                fromFolderPath = normalizedFromFolderPath,
                toFolderPath = toFolderPath,
            ) ?: pagePath.takeUnless { isPageInsideFolder(it, normalizedFromFolderPath) }
        }
        openPageBackStack.clear()
        remappedEntries.forEach(openPageBackStack::addLast)
    }

    private fun popOpenPageBackStackCandidate(pages: List<ApiPageSummary>?): String? {
        val availablePages = pages
            ?.map(ApiPageSummary::path)
            ?.map(::normalizePagePath)
            ?.toSet()
        while (openPageBackStack.isNotEmpty()) {
            val candidate = normalizePagePath(openPageBackStack.removeLast())
            if (candidate.isBlank()) {
                continue
            }
            if (availablePages == null || candidate in availablePages) {
                return candidate
            }
        }
        return null
    }

    private fun readUploadSpec(uri: Uri): UploadSpec {
        val resolver = getApplication<Application>().contentResolver
        val fileName = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            val columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (columnIndex >= 0 && cursor.moveToFirst()) {
                cursor.getString(columnIndex)
            } else {
                null
            }
        }?.takeIf(String::isNotBlank)
            ?: uri.lastPathSegment?.substringAfterLast('/')
            ?: "upload.bin"
        val contentType = resolver.getType(uri).orEmpty()
        val content = resolver.openInputStream(uri)?.use { input ->
            input.readBytes()
        } ?: throw IllegalStateException("File could not be read.")
        return UploadSpec(
            fileName = fileName,
            contentType = contentType,
            content = content,
        )
    }

    private data class SavePageResult(
        val rawMarkdown: String,
        val frontmatter: JsonObject,
        val pageTasks: List<ApiTaskItem>,
        val derived: DerivedPageResponse?,
        val pages: List<ApiPageSummary>?,
        val folders: List<String>?,
        val tasks: List<TaskItem>?,
        val today: TodaySnapshot?,
    )

    private data class OpenPageLoadResult(
        val rawMarkdown: String,
        val frontmatter: JsonObject,
        val pageTasks: List<ApiTaskItem>,
        val derived: DerivedPageResponse?,
    )

    private data class PatchedTaskResult(
        val rawMarkdown: String,
        val frontmatter: JsonObject,
        val pageTasks: List<ApiTaskItem>,
        val derived: DerivedPageResponse?,
        val tasks: List<TaskItem>?,
        val today: TodaySnapshot?,
    )

    private data class PageMutationResult(
        val rawMarkdown: String,
        val frontmatter: JsonObject,
        val pageTasks: List<ApiTaskItem>,
        val derived: DerivedPageResponse?,
        val pages: List<ApiPageSummary>?,
        val folders: List<String>?,
        val tasks: List<TaskItem>?,
        val today: TodaySnapshot?,
    )

    private data class VaultMutationResult(
        val vault: VaultRecord,
        val settings: AppSettings,
        val vaults: List<VaultRecord>?,
        val collections: CollectionsRefreshResult,
    )

    private data class ThemeMutationResult(
        val theme: ThemeRecord,
        val themes: List<ThemeRecord>,
    )

    private data class PageHistoryRestoreResult(
        val mutation: PageMutationResult,
        val revisions: List<PageRevisionRecord>,
    )

    private data class TrashRestoreResult(
        val mutation: PageMutationResult,
        val trashPages: List<TrashPageRecord>,
    )

    private data class DocumentMoveMutationResult(
        val move: NoteriousRepository.DocumentMoveResult,
        val documents: List<DocumentRecord>?,
        val collections: CollectionsRefreshResult?,
    )

    private data class CollectionsRefreshResult(
        val pages: List<ApiPageSummary>?,
        val folders: List<String>?,
        val tasks: List<TaskItem>?,
        val today: TodaySnapshot?,
    )

    private data class UploadSpec(
        val fileName: String,
        val contentType: String,
        val content: ByteArray,
    )
}
