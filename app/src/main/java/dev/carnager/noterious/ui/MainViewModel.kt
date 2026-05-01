package dev.carnager.noterious.ui

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.carnager.noterious.data.AppSettings
import dev.carnager.noterious.data.NoteriousRepository
import dev.carnager.noterious.data.SettingsRepository
import dev.carnager.noterious.model.ApiTaskItem
import dev.carnager.noterious.model.ApiPageSummary
import dev.carnager.noterious.model.DerivedPageResponse
import dev.carnager.noterious.model.DocumentRecord
import dev.carnager.noterious.model.SearchResponse
import dev.carnager.noterious.model.TaskItem
import dev.carnager.noterious.model.TodaySnapshot
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
import kotlinx.serialization.json.JsonObject

data class MainUiState(
    val settings: AppSettings = AppSettings(),
    val vaults: List<VaultRecord> = emptyList(),
    val pages: List<ApiPageSummary> = emptyList(),
    val tasks: List<TaskItem> = emptyList(),
    val today: TodaySnapshot = TodaySnapshot(),
    val openPagePath: String? = null,
    val openPageContent: String? = null,
    val openPageFrontmatter: JsonObject? = null,
    val openPageTasks: List<ApiTaskItem> = emptyList(),
    val openPageDerived: DerivedPageResponse? = null,
    val isPageLoading: Boolean = false,
    val isPageSaving: Boolean = false,
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
    private val openPageBackStack = ArrayDeque<String>()

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                settingsLoaded = true
                _uiState.update { it.copy(settings = settings) }
                reconcileEventStream()
                if (appInForeground) {
                    loadForegroundDataIfNeeded(trigger = "settings-loaded")
                }
            }
        }
    }

    private fun loadForegroundDataIfNeeded(trigger: String) {
        val state = _uiState.value
        if (state.settings.serverUrl.isBlank()) return
        if (state.pages.isEmpty()) {
            refresh(trigger = trigger)
        }
        if (state.vaults.isEmpty()) {
            fetchVaults()
        }
    }

    fun refresh(trigger: String = "manual") {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            if (settings.serverUrl.isBlank()) {
                _uiState.update { it.copy(error = "Konfiguriere zuerst die Server-URL.") }
                return@launch
            }
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching {
                val pages = repository.fetchPages(
                    url = settings.serverUrl,
                    scopePrefix = settings.scopePrefix,
                    bearerToken = settings.bearerToken,
                    username = settings.username,
                    password = settings.password,
                )
                val tasks = repository.fetchTasks(
                    url = settings.serverUrl,
                    scopePrefix = settings.scopePrefix,
                    bearerToken = settings.bearerToken,
                    username = settings.username,
                    password = settings.password,
                )
                val today = repository.buildTodaySnapshot(tasks)
                Triple(pages, tasks, today)
            }.onSuccess { (pages, tasks, today) ->
                _uiState.update {
                    it.copy(pages = pages, tasks = tasks, today = today, isLoading = false, error = null)
                }
                reloadOpenPageIfNeeded()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(isLoading = false, error = error.message ?: "Sync fehlgeschlagen.")
                }
            }
        }
    }

    fun fetchVaults() {
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            if (settings.serverUrl.isBlank()) return@launch
            runCatching {
                repository.fetchVaults(
                    url = settings.serverUrl,
                    bearerToken = settings.bearerToken,
                    username = settings.username,
                    password = settings.password,
                )
            }.onSuccess { vaults ->
                _uiState.update { it.copy(vaults = vaults) }
            }
        }
    }

    fun selectVault(vault: VaultRecord) {
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            val scopePrefix = scopePrefixForVault(vault)
            settingsRepository.save(settings.copy(scopePrefix = scopePrefix))
            refresh(trigger = "vault-switch")
        }
    }

    fun saveSettings(
        serverUrl: String,
        scopePrefix: String,
        username: String,
        password: String,
        bearerToken: String,
    ) {
        viewModelScope.launch {
            settingsRepository.save(
                AppSettings(
                    serverUrl = serverUrl,
                    scopePrefix = scopePrefix,
                    username = username,
                    password = password,
                    bearerToken = bearerToken,
                ),
            )
            refresh(trigger = "settings")
        }
    }

    fun onAppForegrounded() {
        appInForeground = true
        reconcileEventStream()
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
                    isPageLoading = true,
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
                    it.copy(isPageLoading = false, error = error.message ?: "Seite konnte nicht geladen werden.")
                }
            }
        }
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
                isPageLoading = false,
                isPageSaving = false,
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
                isPageLoading = false,
                isPageSaving = false,
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
                        tasks = result.tasks ?: current.tasks,
                        today = result.today ?: current.today,
                    )
                }
                onResult(true)
            }.onFailure { error ->
                if (error is CancellationException) return@onFailure
                _uiState.update {
                    it.copy(isPageSaving = false, error = error.message ?: "Seite konnte nicht gespeichert werden.")
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
                onResult(document)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(error = error.message ?: "Datei konnte nicht hochgeladen werden.")
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
                    it.copy(error = error.message ?: "Task konnte nicht aktualisiert werden.")
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
                _uiState.update { it.copy(isSearching = false, error = error.message ?: "Suche fehlgeschlagen.") }
            }
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _uiState.update { it.copy(searchResults = null, isSearching = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun reloadOpenPageIfNeeded() {
        val pagePath = _uiState.value.openPagePath ?: return
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
            "page.changed", "page.deleted", "task.changed", "task.deleted" -> scheduleLiveRefresh()
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
        if (settings.serverUrl.isBlank()) return null
        return listOf(
            settings.serverUrl.trim(),
            settings.scopePrefix.trim(),
            settings.username.trim(),
            settings.password,
            settings.bearerToken.trim(),
        ).joinToString("\u0000")
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
        } ?: throw IllegalStateException("Datei konnte nicht gelesen werden.")
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

    private data class UploadSpec(
        val fileName: String,
        val contentType: String,
        val content: ByteArray,
    )
}
