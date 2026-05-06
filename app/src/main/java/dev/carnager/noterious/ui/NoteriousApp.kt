package dev.carnager.noterious.ui

import android.content.Intent
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import android.text.format.DateFormat
import android.webkit.MimeTypeMap
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Task
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.carnager.noterious.model.ApiPageSummary
import dev.carnager.noterious.model.ApiTaskItem
import dev.carnager.noterious.model.BacklinkRecord
import dev.carnager.noterious.model.DerivedPageResponse
import dev.carnager.noterious.model.DocumentRecord
import dev.carnager.noterious.model.PageRevisionRecord
import dev.carnager.noterious.model.QueryBlock
import dev.carnager.noterious.model.QueryCopilotResponse
import dev.carnager.noterious.model.QueryResult
import dev.carnager.noterious.model.QueryWorkbenchResult
import dev.carnager.noterious.model.SearchTaskResult
import dev.carnager.noterious.model.ServerMetaResponse
import dev.carnager.noterious.model.ServerSettingsResponse
import dev.carnager.noterious.model.TaskItem
import dev.carnager.noterious.model.ThemeRecord
import dev.carnager.noterious.model.TrashPageRecord
import dev.carnager.noterious.model.UserSettingsPayload
import dev.carnager.noterious.model.VaultRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.io.File
import java.util.Locale

private enum class Tab { Pages, Tasks, Search, Settings }

private enum class StartupTab(val wireValue: String, val label: String, val tab: Tab) {
    Pages("pages", "Pages", Tab.Pages),
    Tasks("tasks", "Tasks", Tab.Tasks),
}

private fun startupTabForValue(value: String?): StartupTab {
    return StartupTab.entries.firstOrNull { tab ->
        tab.wireValue.equals(value.orEmpty().trim(), ignoreCase = true)
    } ?: StartupTab.Pages
}

private enum class TaskListFilter(val label: String) {
    Open("Open"),
    Done("Done"),
    All("All"),
    WithDue("With due date"),
    WithReminder("Reminder"),
    Today("Today"),
    Overdue("Overdue"),
}

private enum class FrontmatterKind(val label: String) {
    Text("Text"),
    List("List"),
    Tags("Tags"),
    Bool("Boolean"),
    Date("Date"),
    DateTime("Date & time"),
    Notification("Notification"),
}

private sealed interface NotePreviewItem {
    data class MarkdownBlock(
        val blockIndex: Int,
        val block: NoteEditorBlock,
        val startLine: Int,
        val endLine: Int,
    ) : NotePreviewItem
    data class QueryResult(
        val blockIndex: Int,
        val startLine: Int,
        val block: QueryBlock,
    ) : NotePreviewItem
}

private data class TableCellSelection(
    val columnIndex: Int,
    val rowIndex: Int? = null,
)

private data class TableEditorState(
    val blockIndex: Int,
    val cell: TableCellSelection,
)

private data class TaskTextEditorState(
    val taskRef: String,
    val lineNumber: Int,
)

private data class TaskScheduleEditorState(
    val taskRef: String,
    val lineNumber: Int,
)

private data class TextBlockEditorState(
    val blockIndex: Int,
    val lineNumber: Int,
)

private enum class BlockInsertPlacement { Above, Below }

private data class BlockActionMenuState(
    val blockIndex: Int,
    val lineNumber: Int,
    val placement: BlockInsertPlacement = BlockInsertPlacement.Below,
)

private data class PendingBlockInsertAnchor(
    val blockIndex: Int,
    val placement: BlockInsertPlacement,
)

private data class FrontmatterEntry(
    val key: String,
    val value: kotlinx.serialization.json.JsonElement,
    val kind: FrontmatterKind,
)

private data class FrontmatterDraftState(
    val originalKey: String? = null,
    val key: String = "",
    val kind: FrontmatterKind = FrontmatterKind.Text,
    val text: String = "",
    val items: List<String> = emptyList(),
    val pendingItem: String = "",
    val boolValue: Boolean = false,
)

private data class ImageDownloadPayload(
    val bytes: ByteArray,
    val fileName: String,
    val mimeType: String,
)

private data class TextDocumentExport(
    val suggestedName: String,
    val content: String,
)

private val screenContentBottomPadding = 24.dp

private data class TaskResultCardModel(
    val ref: String,
    val page: String,
    val line: Int?,
    val text: String,
    val done: Boolean,
    val due: String?,
    val remind: String?,
    val click: String?,
    val supportingText: String?,
    val snippet: String? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteriousApp(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by rememberSaveable { mutableStateOf(Tab.Pages) }
    var appliedStartupTab by rememberSaveable { mutableStateOf<String?>(null) }
    var browseCurrentFolder by rememberSaveable { mutableStateOf("") }
    var browseTagFilter by rememberSaveable { mutableStateOf("") }
    var tasksFilterText by rememberSaveable { mutableStateOf("") }
    var tasksQuickFilterName by rememberSaveable { mutableStateOf(TaskListFilter.Open.name) }
    var searchText by rememberSaveable { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    var showSlashMenu by remember { mutableStateOf(false) }
    var showCommandPalette by remember { mutableStateOf(false) }
    var showScopePicker by remember { mutableStateOf(false) }
    var showTrashSheet by remember { mutableStateOf(false) }
    var showDocumentsSheet by remember { mutableStateOf(false) }
    var showQuerySheet by remember { mutableStateOf(false) }
    var queryWorkbenchDraft by rememberSaveable { mutableStateOf("") }
    var pendingTextExport by remember { mutableStateOf<TextDocumentExport?>(null) }

    val uploadThemeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) {
            return@rememberLauncherForActivityResult
        }
        viewModel.uploadTheme(uri) { theme ->
            if (theme == null) {
                return@uploadTheme
            }
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Theme \"${theme.name.ifBlank { theme.id }}\" uploaded.")
            }
        }
    }
    val exportDocumentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
        val export = pendingTextExport
        pendingTextExport = null
        if (uri == null || export == null) {
            return@rememberLauncherForActivityResult
        }
        coroutineScope.launch {
            val message = runCatching {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { writer ->
                        writer.write(export.content)
                    } ?: error("Document could not be created.")
                }
                "\"${export.suggestedName}\" saved."
            }.getOrElse { error ->
                error.message ?: "Document could not be saved."
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(uiState.error) {
        if (uiState.openPagePath == null) {
            uiState.error?.let { msg ->
                snackbarHostState.showSnackbar(msg)
                viewModel.clearError()
            }
        }
    }

    LaunchedEffect(uiState.settingsLoaded, uiState.settings.startupTab) {
        if (!uiState.settingsLoaded) {
            return@LaunchedEffect
        }
        val startupTab = startupTabForValue(uiState.settings.startupTab)
        if (appliedStartupTab == null) {
            selectedTab = startupTab.tab
        }
        appliedStartupTab = startupTab.wireValue
    }

    LaunchedEffect(
        uiState.settingsLoaded,
        uiState.settings.serverUrl,
        uiState.settings.username,
        uiState.settings.password,
        uiState.settings.bearerToken,
    ) {
        if (uiState.settingsLoaded) {
            viewModel.ensureThemeLibraryLoaded()
        }
    }

    LaunchedEffect(selectedTab) {
        if (selectedTab == Tab.Settings) {
            viewModel.ensureSettingsDetailsLoaded()
        }
    }

    fun navigateToTab(tab: Tab) {
        showSlashMenu = false
        if (uiState.openPagePath != null) {
            viewModel.closeOpenPageStack()
        }
        selectedTab = tab
    }

    if (showSlashMenu) {
        SlashMenuSheet(
            onDismiss = { showSlashMenu = false },
            onOpenPage = {
                showSlashMenu = false
                viewModel.ensureTemplatePagesLoaded()
                showCommandPalette = true
            },
            onOpenDocument = {
                showSlashMenu = false
                viewModel.fetchDocuments()
                showDocumentsSheet = true
            },
            onSearch = {
                navigateToTab(Tab.Search)
            },
            onRunQuery = {
                showSlashMenu = false
                showQuerySheet = true
            },
            onOpenTrash = {
                showSlashMenu = false
                viewModel.fetchTrashPages()
                showTrashSheet = true
            },
            onSettings = {
                navigateToTab(Tab.Settings)
            },
        )
    }

    if (showTrashSheet) {
        TrashSheet(
            pages = uiState.trashPages,
            scopePrefix = uiState.settings.scopePrefix,
            isLoading = uiState.isTrashLoading,
            isBusy = uiState.isTrashBusy,
            onDismiss = {
                if (!uiState.isTrashBusy) {
                    showTrashSheet = false
                }
            },
            onRefresh = { viewModel.fetchTrashPages() },
            onRestore = { pagePath ->
                viewModel.restoreTrashPage(pagePath) { success ->
                    if (success) {
                        showTrashSheet = false
                    }
                }
            },
            onDelete = { pagePath ->
                viewModel.permanentlyDeleteTrashPage(pagePath)
            },
            onEmptyTrash = {
                viewModel.emptyTrash()
            },
        )
    }

    if (showDocumentsSheet) {
        DocumentLibrarySheet(
            documents = uiState.documents,
            scopePrefix = uiState.settings.scopePrefix,
            isLoading = uiState.isDocumentsLoading,
            isBusy = uiState.isDocumentsBusy,
            onDismiss = {
                if (!uiState.isDocumentsBusy) {
                    showDocumentsSheet = false
                }
            },
            onRefresh = { viewModel.fetchDocuments() },
            onOpenDocument = { documentPath ->
                val url = documentDownloadUrl(uiState.settings.serverUrl, documentPath)
                if (url.isNotBlank()) {
                    uriHandler.openUri(url)
                }
            },
            onRenameDocument = { documentPath, nextDocumentPath, onResult ->
                viewModel.moveDocument(documentPath, nextDocumentPath, onResult)
            },
            onDeleteDocument = { documentPath, onResult ->
                viewModel.deleteDocument(documentPath, onResult)
            },
        )
    }

    if (showQuerySheet) {
        QueryWorkbenchSheet(
            queryText = queryWorkbenchDraft,
            executedQuery = uiState.lastExecutedQuery,
            workbench = uiState.queryWorkbench,
            scopePrefix = uiState.settings.scopePrefix,
            isRunning = uiState.isQueryWorkbenchLoading,
            onDismiss = {
                if (!uiState.isQueryWorkbenchLoading) {
                    showQuerySheet = false
                }
            },
            onQueryTextChange = { queryWorkbenchDraft = it },
            onRun = {
                viewModel.runQueryWorkbench(queryWorkbenchDraft)
            },
            onClear = {
                queryWorkbenchDraft = ""
                viewModel.clearQueryWorkbench()
            },
            onOpenPage = { pagePath ->
                showQuerySheet = false
                viewModel.openPage(pagePath)
            },
        )
    }

    // Command palette overlay
    if (showCommandPalette) {
        CommandPaletteSheet(
            pages = uiState.pages,
            templatePages = if (uiState.templatePages.isEmpty()) uiState.pages else uiState.templatePages,
            scopePrefix = uiState.settings.scopePrefix,
            onOpenPage = {
                showCommandPalette = false
                viewModel.openPage(it)
            },
            onCreatePage = {
                showCommandPalette = false
                viewModel.createPage(it)
            },
            onCreateTemplatePage = { template, pagePath ->
                showCommandPalette = false
                viewModel.createPageFromTemplate(template, pagePath)
            },
            onDismiss = { showCommandPalette = false },
        )
    }

    if (uiState.openPagePath != null) {
        PageViewerScreen(
            pagePath = uiState.openPagePath!!,
            content = uiState.openPageContent,
            frontmatter = uiState.openPageFrontmatter,
            pageTasks = uiState.openPageTasks,
            derived = uiState.openPageDerived,
            pageHistory = uiState.openPageHistory,
            settings = uiState.settings,
            error = uiState.error,
            isLoading = uiState.isPageLoading,
            isSaving = uiState.isPageSaving,
            isPageHistoryLoading = uiState.isPageHistoryLoading,
            isPageHistoryBusy = uiState.isPageHistoryBusy,
            onBack = { viewModel.closePage() },
            onOpenPage = { viewModel.openPage(it) },
            onClearError = { viewModel.clearError() },
            onSavePage = { markdown, baseMarkdown, onResult ->
                viewModel.saveOpenPage(markdown, baseMarkdown, onResult)
            },
            onRenamePage = { nextPagePath, onResult ->
                viewModel.renameOpenPage(nextPagePath, onResult)
            },
            onDeletePage = { onResult ->
                viewModel.deleteOpenPage(onResult)
            },
            onLoadPageHistory = { viewModel.fetchOpenPageHistory() },
            onRestorePageHistory = { revisionId, onResult ->
                viewModel.restoreOpenPageHistory(revisionId, onResult)
            },
            onPurgePageHistory = { onResult ->
                viewModel.purgeOpenPageHistory(onResult)
            },
            onShowGlobalMenu = { showSlashMenu = true },
            onPatchTask = { taskRef, text, state, due, remind, click, onResult ->
                viewModel.patchOpenPageTask(
                    taskRef = taskRef,
                    text = text,
                    state = state,
                    due = due,
                    remind = remind,
                    click = click,
                    onResult = onResult,
                )
            },
            onPatchFrontmatter = { set, remove, onResult ->
                viewModel.patchOpenPageFrontmatter(
                    set = set,
                    remove = remove,
                    onResult = onResult,
                )
            },
            onUploadDocument = { uri, onResult ->
                viewModel.uploadDocumentForOpenPage(uri, onResult)
            },
            onGenerateQueryCopilot = { intent, onResult ->
                viewModel.generateQueryCopilot(intent, onResult = onResult)
            },
        )
        return
    }

    fun openScopePicker() {
        if (uiState.settings.serverUrl.isBlank()) return
        if (uiState.vaults.isEmpty()) {
            viewModel.fetchVaults()
        }
        showScopePicker = true
    }

    val currentScopeLabel = remember(uiState.settings.scopePrefix, uiState.vaults) {
        displayCurrentScopeLabel(uiState.settings.scopePrefix, uiState.vaults)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = uiState.settings.serverUrl.isNotBlank(), onClick = ::openScopePicker)
                            .padding(vertical = 4.dp),
                    ) {
                        Text("Noterious")
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = currentScopeLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = "Choose scope",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                actions = {
                    if (uiState.isLiveSyncConnected) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Live",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    IconButton(
                        onClick = ::openScopePicker,
                        enabled = uiState.settings.serverUrl.isNotBlank(),
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = "Change scope")
                    }
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Sync, contentDescription = "Refresh")
                    }
                    IconButton(onClick = { showSlashMenu = true }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == Tab.Pages,
                    onClick = { selectedTab = Tab.Pages },
                    icon = { Icon(Icons.Default.Folder, contentDescription = null) },
                    label = { Text("Pages") },
                )
                NavigationBarItem(
                    selected = selectedTab == Tab.Tasks,
                    onClick = { selectedTab = Tab.Tasks },
                    icon = { Icon(Icons.Default.Task, contentDescription = null) },
                    label = { Text("Tasks") },
                )
                NavigationBarItem(
                    selected = selectedTab == Tab.Search,
                    onClick = { selectedTab = Tab.Search },
                    icon = { Icon(Icons.Default.Search, contentDescription = null) },
                    label = { Text("Search") },
                )
                NavigationBarItem(
                    selected = selectedTab == Tab.Settings,
                    onClick = { selectedTab = Tab.Settings },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Settings") },
                )
            }
        },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            if (uiState.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
                    (slideInHorizontally { it * direction } + fadeIn())
                        .togetherWith(slideOutHorizontally { -it * direction } + fadeOut())
                },
                label = "tab",
            ) { tab ->
                when (tab) {
                    Tab.Pages -> BrowseScreen(
                        pages = uiState.pages,
                        folders = uiState.folders,
                        scopePrefix = uiState.settings.scopePrefix,
                        currentFolder = browseCurrentFolder,
                        selectedTag = browseTagFilter,
                        onCurrentFolderChange = { browseCurrentFolder = it },
                        onSelectedTagChange = { browseTagFilter = it },
                        onOpenPage = { viewModel.openPage(it) },
                        onCreatePage = { pagePath, onResult ->
                            viewModel.createPage(pagePath, onResult = onResult)
                        },
                        onCreateFolder = { folderPath, onResult ->
                            viewModel.createFolder(folderPath, onResult)
                        },
                        onRenamePage = { pagePath, nextPagePath, onResult ->
                            viewModel.renamePage(pagePath, nextPagePath, onResult)
                        },
                        onDeletePage = { pagePath, onResult ->
                            viewModel.deletePage(pagePath, onResult)
                        },
                        onRenameFolder = { folderPath, nextFolderPath, onResult ->
                            viewModel.renameFolder(folderPath, nextFolderPath, onResult)
                        },
                        onDeleteFolder = { folderPath, onResult ->
                            viewModel.deleteFolder(folderPath, onResult)
                        },
                    )
                    Tab.Tasks -> TasksScreen(
                        tasks = uiState.tasks,
                        scopePrefix = uiState.settings.scopePrefix,
                        filterText = tasksFilterText,
                        taskFilter = runCatching { TaskListFilter.valueOf(tasksQuickFilterName) }.getOrDefault(TaskListFilter.All),
                        onFilterTextChange = { tasksFilterText = it },
                        onTaskFilterChange = { tasksQuickFilterName = it.name },
                        onOpenPage = { viewModel.openPage(it) },
                        onPatchTask = { taskRef, text, state, due, remind, click, onResult ->
                            viewModel.patchTask(
                                taskRef = taskRef,
                                text = text,
                                state = state,
                                due = due,
                                remind = remind,
                                click = click,
                                onResult = onResult,
                            )
                        },
                        onDeleteTask = { taskRef, onResult ->
                            viewModel.deleteTask(taskRef, onResult)
                        },
                    )
                    Tab.Search -> SearchScreen(
                        uiState = uiState,
                        scopePrefix = uiState.settings.scopePrefix,
                        searchText = searchText,
                        onSearchTextChange = {
                            searchText = it
                            if (it.isBlank()) {
                                viewModel.clearSearch()
                            } else {
                                viewModel.search(it)
                            }
                        },
                        onClear = {
                            searchText = ""
                            viewModel.clearSearch()
                        },
                        onOpenPage = { viewModel.openPage(it) },
                        onOpenQuery = { queryName ->
                            viewModel.loadSavedQuery(queryName) { savedQuery ->
                                val queryText = savedQuery?.query?.trim().orEmpty()
                                if (queryText.isBlank()) {
                                    return@loadSavedQuery
                                }
                                queryWorkbenchDraft = queryText
                                showQuerySheet = true
                                viewModel.runQueryWorkbench(queryText)
                            }
                        },
                        onPatchTask = { taskRef, text, state, due, remind, click, onResult ->
                            viewModel.patchTask(
                                taskRef = taskRef,
                                text = text,
                                state = state,
                                due = due,
                                remind = remind,
                                click = click,
                                onResult = onResult,
                            )
                        },
                        onDeleteTask = { taskRef, onResult ->
                            viewModel.deleteTask(taskRef, onResult)
                        },
                    )
                    Tab.Settings -> SettingsScreen(
                        settings = uiState.settings,
                        vaults = uiState.vaults,
                        userSettings = uiState.userSettings,
                        serverSettings = uiState.serverSettings,
                        serverMeta = uiState.serverMeta,
                        themes = uiState.themes,
                        isDetailsLoading = uiState.isSettingsDetailsLoading,
                        isThemesLoading = uiState.isThemesLoading,
                        isThemeBusy = uiState.isThemeBusy,
                        isVaultBusy = uiState.isLoading,
                        isUserSettingsSaving = uiState.isUserSettingsSaving,
                        onSave = { url, scope, user, pass, token, startupTab ->
                            viewModel.saveSettings(url, scope, user, pass, token, startupTab)
                        },
                        onRefreshDetails = {
                            viewModel.ensureSettingsDetailsLoaded(force = true)
                        },
                        onRefreshThemes = {
                            viewModel.ensureThemeLibraryLoaded(force = true)
                        },
                        onSaveThemeSelection = { themeId ->
                            viewModel.saveThemeSelection(themeId)
                        },
                        onUploadTheme = {
                            uploadThemeLauncher.launch("application/json")
                        },
                        onDeleteTheme = { themeId, onResult ->
                            viewModel.deleteTheme(themeId, onResult)
                        },
                        onRefreshVaults = {
                            viewModel.fetchVaults()
                        },
                        onCreateVault = { name, onResult ->
                            viewModel.createVault(name, onResult)
                        },
                        onRenameVault = { vault, nextName, onResult ->
                            viewModel.renameVault(vault, nextName, onResult)
                        },
                        onSelectVault = { vault ->
                            viewModel.selectVault(vault)
                        },
                        onExportBackupManifest = {
                            uiState.serverMeta?.let { meta ->
                                pendingTextExport = TextDocumentExport(
                                    suggestedName = backupManifestFilename(meta),
                                    content = buildBackupManifest(meta),
                                )
                                exportDocumentLauncher.launch(backupManifestFilename(meta))
                            }
                        },
                        onExportBackupScript = {
                            uiState.serverMeta?.let { meta ->
                                pendingTextExport = TextDocumentExport(
                                    suggestedName = backupScriptFilename(meta),
                                    content = buildBackupScript(meta),
                                )
                                exportDocumentLauncher.launch(backupScriptFilename(meta))
                            }
                        },
                        onSaveUserSettings = { topicUrl, token, onResult ->
                            viewModel.saveUserNotificationSettings(topicUrl, token, onResult)
                        },
                        onChangePassword = { currentPassword, newPassword, onResult ->
                            viewModel.changePassword(currentPassword, newPassword, onResult)
                        },
                        onLogout = { onResult ->
                            viewModel.logout(onResult)
                        },
                    )
                }
            }
        }
    }

    if (showScopePicker) {
        ScopePickerSheet(
            vaults = uiState.vaults,
            currentScopePrefix = uiState.settings.scopePrefix,
            onDismiss = { showScopePicker = false },
            onSelectAll = {
                showScopePicker = false
                viewModel.saveSettings(
                    serverUrl = uiState.settings.serverUrl,
                    scopePrefix = "",
                    username = uiState.settings.username,
                    password = uiState.settings.password,
                    bearerToken = uiState.settings.bearerToken,
                    startupTab = uiState.settings.startupTab,
                )
            },
            onSelectVault = { vault ->
                showScopePicker = false
                viewModel.selectVault(vault)
            },
            onManageVaults = {
                showScopePicker = false
                navigateToTab(Tab.Settings)
            },
        )
    }
}

// ─── Scope Picker ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScopePickerSheet(
    vaults: List<VaultRecord>,
    currentScopePrefix: String,
    onDismiss: () -> Unit,
    onSelectAll: () -> Unit,
    onSelectVault: (VaultRecord) -> Unit,
    onManageVaults: () -> Unit,
) {
    val normalizedCurrentScope = remember(currentScopePrefix) {
        normalizeScopePrefix(currentScopePrefix)
    }
    val selectableVaults = remember(vaults) {
        vaults
            .sortedBy { vault -> displayScopeName(vault).lowercase() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "Scope",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Choose which vault subtree the app should show.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))

            ScopePickerItem(
                title = "All scopes",
                subtitle = selectableVaults.takeIf { it.isNotEmpty() }?.size?.let { "$it scopes" },
                selected = normalizedCurrentScope.isBlank(),
                onClick = onSelectAll,
            )

            if (selectableVaults.isEmpty()) {
                Text(
                    text = if (vaults.isEmpty()) "Loading scopes..." else "No additional scopes available.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
                )
            } else {
                selectableVaults.forEach { vault ->
                    ScopePickerItem(
                        title = displayScopeName(vault),
                        subtitle = null,
                        selected = normalizedCurrentScope == scopePrefixForVault(vault),
                        onClick = { onSelectVault(vault) },
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            TextButton(
                onClick = onManageVaults,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Manage vaults")
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ScopePickerItem(
    title: String,
    subtitle: String?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = if (selected) Icons.Default.CheckCircle else Icons.Default.Folder,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                subtitle?.takeIf(String::isNotBlank)?.let { value ->
                    Text(
                        text = value,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// ─── Slash Menu ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SlashMenuSheet(
    onDismiss: () -> Unit,
    onOpenPage: () -> Unit,
    onOpenDocument: () -> Unit,
    onSearch: () -> Unit,
    onRunQuery: () -> Unit,
    onOpenTrash: () -> Unit,
    onSettings: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            SlashMenuItem(icon = Icons.Default.Description, label = "Open Page", onClick = onOpenPage)
            SlashMenuItem(icon = Icons.Default.FolderOpen, label = "Open Document", onClick = onOpenDocument)
            SlashMenuItem(icon = Icons.Default.Search, label = "Search", onClick = onSearch)
            SlashMenuItem(icon = Icons.Default.Tune, label = "Run Query", onClick = onRunQuery)
            SlashMenuItem(icon = Icons.Default.Delete, label = "Trash", onClick = onOpenTrash)
            SlashMenuItem(icon = Icons.Default.Settings, label = "Settings", onClick = onSettings)
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SlashMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        val tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outline
        val textColor = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = textColor)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QueryWorkbenchSheet(
    queryText: String,
    executedQuery: String,
    workbench: QueryWorkbenchResult?,
    scopePrefix: String,
    isRunning: Boolean,
    onDismiss: () -> Unit,
    onQueryTextChange: (String) -> Unit,
    onRun: () -> Unit,
    onClear: () -> Unit,
    onOpenPage: (String) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var queryValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = queryText,
                selection = androidx.compose.ui.text.TextRange(queryText.length),
            ),
        )
    }
    val previewBlock = remember(workbench) { workbench?.let(::queryWorkbenchPreviewBlock) }
    val preview = workbench?.preview
    val hasDraftChanges = remember(queryText, executedQuery) {
        executedQuery.isNotBlank() && queryText.trim() != executedQuery.trim()
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        if (queryText.isBlank()) {
            keyboardController?.show()
        }
    }

    LaunchedEffect(queryText) {
        if (queryText != queryValue.text) {
            queryValue = TextFieldValue(
                text = queryText,
                selection = androidx.compose.ui.text.TextRange(queryText.length),
            )
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .imePadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Run query",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Define a query and preview the matching rows in the current scope.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = queryValue,
                onValueChange = {
                    queryValue = it
                    onQueryTextChange(it.text)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                enabled = !isRunning,
                label = { Text("Query") },
                placeholder = { Text("pages where ...") },
                minLines = 4,
                maxLines = 8,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                TextButton(
                    onClick = onClear,
                    enabled = !isRunning && (queryText.isNotBlank() || workbench != null),
                ) {
                    Text("Clear")
                }
                Button(
                    onClick = {
                        keyboardController?.hide()
                        onRun()
                    },
                    enabled = !isRunning && queryText.isNotBlank(),
                ) {
                    if (isRunning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text("Run")
                    }
                }
            }
            if (isRunning) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            if (hasDraftChanges) {
                Text(
                    text = "Draft changed. Run again to refresh the preview.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            when {
                previewBlock != null -> {
                    QueryBlockCard(
                        block = previewBlock,
                        scopePrefix = scopePrefix,
                        onLinkClick = onOpenPage,
                        onEditQuery = null,
                        onLongPress = null,
                    )
                    if (preview?.valid == true && preview.truncated) {
                        Text(
                            text = "Showing ${preview.rows.size} of ${preview.count} results.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                workbench != null && !isRunning -> {
                    Text(
                        text = queryWorkbenchFallbackMessage(workbench),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss, enabled = !isRunning) {
                    Text("Close")
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

// ─── Command Palette ────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommandPaletteSheet(
    pages: List<ApiPageSummary>,
    templatePages: List<ApiPageSummary>,
    scopePrefix: String,
    onOpenPage: (String) -> Unit,
    onCreatePage: (String) -> Unit,
    onCreateTemplatePage: (NoteTemplate, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    val normalizedDraftPath = remember(query) { normalizePagePath(query) }
    val scopedDraftPath = remember(normalizedDraftPath, scopePrefix) {
        applyScopePrefixToPagePath(normalizedDraftPath, scopePrefix)
    }
    val exactMatch = remember(pages, scopedDraftPath) {
        scopedDraftPath.isNotBlank() && pages.any {
            normalizePagePath(it.path).equals(scopedDraftPath, ignoreCase = true)
        }
    }
    val exactMatchPage = remember(pages, scopedDraftPath) {
        pages.firstOrNull {
            normalizePagePath(it.path).equals(scopedDraftPath, ignoreCase = true)
        }
    }
    val createTargetLabel = remember(scopedDraftPath, scopePrefix) {
        displayPagePath(scopedDraftPath, scopePrefix).ifBlank { scopedDraftPath }
    }
    val templates = remember(templatePages, scopePrefix) {
        noteTemplatesFromPages(templatePages, scopePrefix)
    }
    val templateEntries = remember(templates, normalizedDraftPath, pages, scopePrefix) {
        if (normalizedDraftPath.isBlank()) {
            emptyList()
        } else {
            templates.mapNotNull { template ->
                val targetPath = buildPagePathFromTemplate(template, normalizedDraftPath)
                val scopedTargetPath = applyScopePrefixToPagePath(targetPath, scopePrefix)
                if (
                    targetPath.isBlank() ||
                    scopedTargetPath.isBlank() ||
                    pages.any { page ->
                        normalizePagePath(page.path).equals(scopedTargetPath, ignoreCase = true)
                    }
                ) {
                    null
                } else {
                    Triple(template, targetPath, scopedTargetPath)
                }
            }
        }
    }
    val filtered = remember(pages, query) {
        if (query.isBlank()) {
            pages.sortedByDescending { it.updatedAt }.take(30)
        } else {
            val lower = query.lowercase()
            pages.filter { it.path.lowercase().contains(lower) || it.title.lowercase().contains(lower) }
                .take(50)
        }
    }
    fun submitSelection() {
        keyboardController?.hide()
        when {
            exactMatchPage != null -> onOpenPage(exactMatchPage.path)
            normalizedDraftPath.isNotBlank() -> onCreatePage(normalizedDraftPath)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Open or create note...") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { submitSelection() }),
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier.height(400.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                if (normalizedDraftPath.isNotBlank() && !exactMatch) {
                    item(key = "create:$scopedDraftPath") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { submitSelection() }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Create note",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = createTargetLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
                items(
                    items = templateEntries,
                    key = { (template, targetPath, _) -> "template:${template.id}:$targetPath" },
                ) { (template, targetPath, scopedTargetPath) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                keyboardController?.hide()
                                onCreateTemplatePage(template, targetPath)
                            }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Create ${template.name}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = displayPagePath(scopedTargetPath, scopePrefix).ifBlank { scopedTargetPath },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = templateFieldSummary(template),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                items(filtered, key = { it.path }) { page ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenPage(page.path) }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            Icons.Default.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                page.title.ifBlank { page.path.substringAfterLast('/') },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                displayPagePath(page.path, scopePrefix),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ─── Page Viewer ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun PageViewerScreen(
    pagePath: String,
    content: String?,
    frontmatter: JsonObject?,
    pageTasks: List<ApiTaskItem>,
    derived: DerivedPageResponse?,
    pageHistory: List<PageRevisionRecord>,
    settings: dev.carnager.noterious.data.AppSettings,
    error: String?,
    isLoading: Boolean,
    isSaving: Boolean,
    isPageHistoryLoading: Boolean,
    isPageHistoryBusy: Boolean,
    onBack: () -> Unit,
    onOpenPage: (String) -> Unit,
    onClearError: () -> Unit,
    onSavePage: (String, String, (Boolean) -> Unit) -> Unit,
    onRenamePage: (String, (Boolean) -> Unit) -> Unit,
    onDeletePage: ((Boolean) -> Unit) -> Unit,
    onLoadPageHistory: () -> Unit,
    onRestorePageHistory: (String, (Boolean) -> Unit) -> Unit,
    onPurgePageHistory: ((Boolean) -> Unit) -> Unit,
    onShowGlobalMenu: () -> Unit,
    onPatchTask: (taskRef: String, text: String?, state: String?, due: String?, remind: String?, click: String?, onResult: (Boolean) -> Unit) -> Unit,
    onPatchFrontmatter: (set: Map<String, kotlinx.serialization.json.JsonElement>, remove: List<String>, onResult: (Boolean) -> Unit) -> Unit,
    onUploadDocument: (Uri, (DocumentRecord?) -> Unit) -> Unit,
    onGenerateQueryCopilot: (intent: String, onResult: (QueryCopilotResponse?) -> Unit) -> Unit,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val snackbarHostState = remember { SnackbarHostState() }
    val noteContentBottomPadding = 104.dp
    val taskReminderClickTarget = remember(pagePath) { noteriousPageDeepLink(pagePath) }
    val displayPath = remember(pagePath, settings.scopePrefix) {
        displayPagePath(pagePath, settings.scopePrefix).ifBlank { pagePath }
    }
    val displayTitle = frontmatter?.get("title")
        ?.jsonPrimitiveOrNull()
        ?.content
        ?.takeIf(String::isNotBlank)
        ?: pageTitleFromPath(displayPath)
    val visibleFrontmatterEntries = remember(frontmatter) { frontmatterVisibleEntries(frontmatter) }
    val visibleFrontmatterByKey = remember(visibleFrontmatterEntries) {
        visibleFrontmatterEntries.associateBy(FrontmatterEntry::key)
    }

    var editorMode by rememberSaveable(pagePath) { mutableStateOf(NoteEditorMode.Preview) }
    var draftMarkdown by rememberSaveable(pagePath) { mutableStateOf(content.orEmpty()) }
    var rawEditorValue by rememberSaveable(pagePath, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(content.orEmpty()))
    }
    var baseMarkdown by rememberSaveable(pagePath) { mutableStateOf(content.orEmpty()) }
    var isUploadingDocument by rememberSaveable(pagePath) { mutableStateOf(false) }
    var selectedBlockIndex by rememberSaveable(pagePath) { mutableStateOf(-1) }
    var guiBlocks by remember(pagePath) { mutableStateOf(parseNoteEditorBlocks(content.orEmpty())) }
    var tableEditorState by remember(pagePath) { mutableStateOf<TableEditorState?>(null) }
    var textBlockEditorState by remember(pagePath) { mutableStateOf<TextBlockEditorState?>(null) }
    var taskTextEditorState by remember(pagePath) { mutableStateOf<TaskTextEditorState?>(null) }
    var taskScheduleEditorState by remember(pagePath) { mutableStateOf<TaskScheduleEditorState?>(null) }
    var pendingTaskRef by remember(pagePath) { mutableStateOf<String?>(null) }
    var blockActionMenuState by remember(pagePath) { mutableStateOf<BlockActionMenuState?>(null) }
    var pendingBlockInsertAnchor by remember(pagePath) { mutableStateOf<PendingBlockInsertAnchor?>(null) }
    var imageActionTarget by remember(pagePath) { mutableStateOf<MarkdownImageTarget?>(null) }
    var pendingImageSaveTarget by remember(pagePath) { mutableStateOf<MarkdownImageTarget?>(null) }
    var isFrontmatterSheetVisible by rememberSaveable(pagePath) { mutableStateOf(false) }
    var frontmatterDraftState by remember(pagePath) { mutableStateOf<FrontmatterDraftState?>(null) }
    var isPatchingFrontmatter by rememberSaveable(pagePath) { mutableStateOf(false) }
    var isPageHistorySheetVisible by rememberSaveable(pagePath) { mutableStateOf(false) }
    var isPageActionsSheetVisible by rememberSaveable(pagePath) { mutableStateOf(false) }
    var isRenamePageSheetVisible by rememberSaveable(pagePath) { mutableStateOf(false) }
    var isQueryInsertSheetVisible by rememberSaveable(pagePath) { mutableStateOf(false) }
    var queryInsertDraft by rememberSaveable(pagePath) { mutableStateOf("") }
    var isGeneratingQuery by rememberSaveable(pagePath) { mutableStateOf(false) }
    var isPerformingPageAction by rememberSaveable(pagePath) { mutableStateOf(false) }

    LaunchedEffect(error) {
        error?.let { message ->
            snackbarHostState.showSnackbar(message)
            onClearError()
        }
    }

    LaunchedEffect(pagePath, content) {
        if (editorMode == NoteEditorMode.Preview && content != null) {
            baseMarkdown = content
            draftMarkdown = content
            rawEditorValue = TextFieldValue(content)
            guiBlocks = parseNoteEditorBlocks(content)
            selectedBlockIndex = if (guiBlocks.isNotEmpty()) 0 else -1
        }
        tableEditorState = null
        textBlockEditorState = null
        taskTextEditorState = null
        taskScheduleEditorState = null
        pendingTaskRef = null
        blockActionMenuState = null
        pendingBlockInsertAnchor = null
        imageActionTarget = null
        pendingImageSaveTarget = null
        isPageHistorySheetVisible = false
        isPageActionsSheetVisible = false
        isRenamePageSheetVisible = false
        isQueryInsertSheetVisible = false
        queryInsertDraft = ""
        isGeneratingQuery = false
        isPerformingPageAction = false
    }

    LaunchedEffect(editorMode) {
        if (editorMode == NoteEditorMode.Raw) {
            focusRequester.requestFocus()
        }
    }

    fun syncGuiBlocksFromMarkdown(markdown: String) {
        val parsed = parseNoteEditorBlocks(markdown)
        guiBlocks = parsed
        selectedBlockIndex = when {
            parsed.isEmpty() -> -1
            selectedBlockIndex in parsed.indices -> selectedBlockIndex
            else -> 0
        }
    }

    fun syncDraftFromBlocks(nextBlocks: List<NoteEditorBlock>) {
        guiBlocks = nextBlocks
        val frontmatter = splitMarkdownFrontmatter(draftMarkdown).frontmatter
        draftMarkdown = combineMarkdownDocument(frontmatter, serializeNoteEditorBlocks(nextBlocks))
        val selection = androidx.compose.ui.text.TextRange(
            rawEditorValue.selection.start.coerceIn(0, draftMarkdown.length),
            rawEditorValue.selection.end.coerceIn(0, draftMarkdown.length),
        )
        rawEditorValue = TextFieldValue(draftMarkdown, selection = selection)
        selectedBlockIndex = when {
            nextBlocks.isEmpty() -> -1
            selectedBlockIndex in nextBlocks.indices -> selectedBlockIndex
            else -> nextBlocks.lastIndex
        }
    }

    fun currentMarkdownDraft(): String {
        return when (editorMode) {
            NoteEditorMode.Preview -> draftMarkdown
            NoteEditorMode.Edit -> draftMarkdown
            NoteEditorMode.Raw -> rawEditorValue.text
        }
    }

    fun enterEditMode() {
        draftMarkdown = currentMarkdownDraft()
        syncGuiBlocksFromMarkdown(draftMarkdown)
        imageActionTarget = null
        blockActionMenuState = null
        editorMode = NoteEditorMode.Edit
    }

    fun enterPreviewMode() {
        draftMarkdown = currentMarkdownDraft()
        syncGuiBlocksFromMarkdown(draftMarkdown)
        imageActionTarget = null
        blockActionMenuState = null
        editorMode = NoteEditorMode.Preview
    }

    fun openRawModeAtLine(line: Int? = null) {
        draftMarkdown = currentMarkdownDraft()
        val offset = line?.let {
            rawOffsetForLineNumber(draftMarkdown, it).coerceIn(0, draftMarkdown.length)
        } ?: rawEditorValue.selection.start.coerceIn(0, draftMarkdown.length)
        rawEditorValue = TextFieldValue(
            text = draftMarkdown,
            selection = androidx.compose.ui.text.TextRange(offset),
        )
        blockActionMenuState = null
        editorMode = NoteEditorMode.Raw
    }

    fun saveDraft(onSuccess: (() -> Unit)? = null) {
        val markdownToSave = currentMarkdownDraft()
        onSavePage(markdownToSave, baseMarkdown) { success ->
            if (success) {
                baseMarkdown = markdownToSave
                draftMarkdown = markdownToSave
                syncGuiBlocksFromMarkdown(markdownToSave)
                if (editorMode == NoteEditorMode.Raw) {
                    rawEditorValue = rawEditorValue.copy(text = markdownToSave)
                }
                onSuccess?.invoke()
            }
        }
    }

    fun cancelEditing() {
        draftMarkdown = baseMarkdown
        rawEditorValue = TextFieldValue(baseMarkdown)
        syncGuiBlocksFromMarkdown(baseMarkdown)
        blockActionMenuState = null
        isQueryInsertSheetVisible = false
        queryInsertDraft = ""
        isGeneratingQuery = false
        pendingBlockInsertAnchor = null
        tableEditorState = null
        textBlockEditorState = null
        taskTextEditorState = null
        taskScheduleEditorState = null
        editorMode = NoteEditorMode.Preview
    }

    fun openFrontmatterSheet() {
        if (editorMode != NoteEditorMode.Preview || currentMarkdownDraft() != baseMarkdown) {
            return
        }
        imageActionTarget = null
        blockActionMenuState = null
        tableEditorState = null
        textBlockEditorState = null
        taskTextEditorState = null
        taskScheduleEditorState = null
        frontmatterDraftState = null
        isFrontmatterSheetVisible = true
    }

    fun dismissFrontmatterSheet() {
        if (isPatchingFrontmatter) {
            return
        }
        frontmatterDraftState = null
        isFrontmatterSheetVisible = false
    }

    fun startFrontmatterAdd() {
        frontmatterDraftState = frontmatterDraftStateForEntry()
    }

    fun startFrontmatterEdit(entry: FrontmatterEntry) {
        frontmatterDraftState = frontmatterDraftStateForEntry(entry)
    }

    fun deleteFrontmatterProperty(key: String) {
        if (key.isBlank() || isPatchingFrontmatter) {
            return
        }
        isPatchingFrontmatter = true
        onPatchFrontmatter(emptyMap(), listOf(key)) { success ->
            isPatchingFrontmatter = false
            if (success && frontmatterDraftState?.originalKey == key) {
                frontmatterDraftState = null
            }
        }
    }

    fun saveFrontmatterDraft() {
        val draft = frontmatterDraftState ?: return
        val key = draft.key.trim()
        if (key.isBlank()) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Frontmatter key is required.")
            }
            return
        }
        if (isInternalFrontmatterMetadataKey(key)) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Internal metadata keys are reserved.")
            }
            return
        }

        val originalEntry = draft.originalKey?.let(visibleFrontmatterByKey::get)
        val setPayload = mapOf(key to frontmatterDraftJsonElement(draft, originalEntry?.value))
        val removePayload = buildList {
            val originalKey = draft.originalKey
            if (!originalKey.isNullOrBlank() && originalKey != key) {
                add(originalKey)
            }
        }

        isPatchingFrontmatter = true
        onPatchFrontmatter(setPayload, removePayload) { success ->
            isPatchingFrontmatter = false
            if (success) {
                frontmatterDraftState = null
            }
        }
    }

    fun normalizeTableBlock(table: NoteEditorBlock.Table): NoteEditorBlock.Table {
        val normalizedHeaders = when {
            table.headers.size >= 2 -> table.headers.mapIndexed { index, header ->
                header.ifBlank { "Column ${index + 1}" }
            }
            else -> {
                val baseHeaders = table.headers + List(2 - table.headers.size) { "" }
                baseHeaders.mapIndexed { index, header ->
                    header.ifBlank { "Column ${index + 1}" }
                }
            }
        }
        val normalizedRows = table.rows.map { row ->
            normalizedHeaders.indices.map { columnIndex ->
                row.getOrElse(columnIndex) { "" }
            }
        }
        return table.copy(headers = normalizedHeaders, rows = normalizedRows)
    }

    fun insertBlockAtAnchor(block: NoteEditorBlock, anchor: PendingBlockInsertAnchor?) {
        val existingBlocks = guiBlocks
        val insertIndex = when {
            existingBlocks.isEmpty() -> 0
            anchor == null -> existingBlocks.size
            anchor.blockIndex !in existingBlocks.indices -> {
                if (anchor.placement == BlockInsertPlacement.Above) {
                    0
                } else {
                    existingBlocks.size
                }
            }
            anchor.placement == BlockInsertPlacement.Above -> anchor.blockIndex
            else -> anchor.blockIndex + 1
        }.coerceIn(0, existingBlocks.size)
        val nextBlocks = existingBlocks.toMutableList().apply {
            add(insertIndex, block)
        }
        syncDraftFromBlocks(nextBlocks)
        selectedBlockIndex = insertIndex.coerceAtMost(nextBlocks.lastIndex)
        pendingBlockInsertAnchor = null
    }

    fun isEditableTextBlock(block: NoteEditorBlock): Boolean {
        return when (block) {
            is NoteEditorBlock.Heading,
            is NoteEditorBlock.Paragraph,
            is NoteEditorBlock.BulletItem,
            is NoteEditorBlock.NumberedItem,
            is NoteEditorBlock.BlockQuote,
            is NoteEditorBlock.CodeFence,
            -> true

            else -> false
        }
    }

    fun updateOpenTable(transform: (NoteEditorBlock.Table) -> Pair<NoteEditorBlock.Table, TableCellSelection>) {
        val editorState = tableEditorState ?: return
        val currentTable = guiBlocks.getOrNull(editorState.blockIndex) as? NoteEditorBlock.Table ?: return
        val (updatedTable, nextSelection) = transform(normalizeTableBlock(currentTable))
        val normalizedTable = normalizeTableBlock(updatedTable)
        syncDraftFromBlocks(guiBlocks.updated(editorState.blockIndex, normalizedTable))
        selectedBlockIndex = editorState.blockIndex
        tableEditorState = editorState.copy(
            cell = nextSelection.copy(
                columnIndex = nextSelection.columnIndex.coerceIn(0, normalizedTable.headers.lastIndex.coerceAtLeast(0)),
                rowIndex = nextSelection.rowIndex?.coerceIn(0, normalizedTable.rows.lastIndex.coerceAtLeast(0)),
            ),
        )
    }

    fun openTableEditor(blockIndex: Int, rowIndex: Int?, columnIndex: Int) {
        if (editorMode == NoteEditorMode.Raw) {
            return
        }
        syncGuiBlocksFromMarkdown(currentMarkdownDraft())
        blockActionMenuState = null
        textBlockEditorState = null
        selectedBlockIndex = blockIndex
        tableEditorState = TableEditorState(
            blockIndex = blockIndex,
            cell = TableCellSelection(
                columnIndex = columnIndex.coerceAtLeast(0),
                rowIndex = rowIndex,
            ),
        )
    }

    fun openTextBlockEditor(blockIndex: Int, lineNumber: Int) {
        if (editorMode != NoteEditorMode.Edit) {
            return
        }
        syncGuiBlocksFromMarkdown(currentMarkdownDraft())
        val block = guiBlocks.getOrNull(blockIndex) ?: return
        if (!isEditableTextBlock(block)) return
        blockActionMenuState = null
        tableEditorState = null
        selectedBlockIndex = blockIndex
        textBlockEditorState = TextBlockEditorState(
            blockIndex = blockIndex,
            lineNumber = lineNumber,
        )
    }

    fun updateTextBlock(blockIndex: Int, updater: (NoteEditorBlock) -> NoteEditorBlock) {
        val currentBlock = guiBlocks.getOrNull(blockIndex) ?: return
        val nextBlock = updater(currentBlock)
        syncDraftFromBlocks(guiBlocks.updated(blockIndex, nextBlock))
        selectedBlockIndex = blockIndex
    }

    fun openBlockActions(blockIndex: Int, lineNumber: Int) {
        if (editorMode != NoteEditorMode.Edit) {
            return
        }
        syncGuiBlocksFromMarkdown(currentMarkdownDraft())
        selectedBlockIndex = blockIndex
        tableEditorState = null
        textBlockEditorState = null
        isQueryInsertSheetVisible = false
        isGeneratingQuery = false
        pendingBlockInsertAnchor = null
        blockActionMenuState = BlockActionMenuState(
            blockIndex = blockIndex,
            lineNumber = lineNumber,
        )
    }

    fun dismissQueryInsertSheet() {
        if (isGeneratingQuery) {
            return
        }
        isQueryInsertSheetVisible = false
        queryInsertDraft = ""
        pendingBlockInsertAnchor = null
    }

    fun openQueryInsertSheet() {
        val target = blockActionMenuState ?: return
        pendingBlockInsertAnchor = PendingBlockInsertAnchor(
            blockIndex = target.blockIndex,
            placement = target.placement,
        )
        blockActionMenuState = null
        queryInsertDraft = ""
        isGeneratingQuery = false
        isQueryInsertSheetVisible = true
    }

    fun generateAndInsertQueryBlock(intent: String) {
        val normalizedIntent = intent.trim()
        if (normalizedIntent.isBlank()) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Describe what the query should do.")
            }
            return
        }
        isGeneratingQuery = true
        onGenerateQueryCopilot(normalizedIntent) { response ->
            isGeneratingQuery = false
            if (response == null) {
                return@onGenerateQueryCopilot
            }
            val generatedQuery = response.formattedQuery.trim().ifBlank {
                response.query.trim()
            }
            if (generatedQuery.isBlank()) {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        response.error.ifBlank { "AI returned no query." },
                    )
                }
                return@onGenerateQueryCopilot
            }
            insertBlockAtAnchor(
                block = NoteEditorBlock.CodeFence(
                    text = generatedQuery,
                    language = "query",
                ),
                anchor = pendingBlockInsertAnchor,
            )
            isQueryInsertSheetVisible = false
            queryInsertDraft = ""
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    if (response.valid) {
                        "Inserted AI-generated query block."
                    } else {
                        "Inserted AI query draft with validation warnings."
                    },
                )
            }
        }
    }

    fun applyBlockInsertCommand(command: NoteSlashCommand) {
        val target = blockActionMenuState ?: return
        pendingBlockInsertAnchor = PendingBlockInsertAnchor(
            blockIndex = target.blockIndex,
            placement = target.placement,
        )
        insertBlockAtAnchor(
            block = newBlockForCommand(command),
            anchor = pendingBlockInsertAnchor,
        )
        blockActionMenuState = null
    }

    fun deleteBlockAtIndex(blockIndex: Int) {
        if (blockIndex !in guiBlocks.indices) return
        val nextBlocks = guiBlocks.toMutableList().apply {
            removeAt(blockIndex)
        }
        syncDraftFromBlocks(nextBlocks)
        selectedBlockIndex = when {
            nextBlocks.isEmpty() -> -1
            blockIndex > nextBlocks.lastIndex -> nextBlocks.lastIndex
            else -> blockIndex
        }
        tableEditorState = null
        textBlockEditorState = null
        blockActionMenuState = null
        pendingBlockInsertAnchor = null
    }

    fun patchTask(
        taskRef: String,
        text: String? = null,
        state: String? = null,
        due: String? = null,
        remind: String? = null,
        click: String? = null,
        onSuccess: (() -> Unit)? = null,
    ) {
        if (pendingTaskRef != null) return
        pendingTaskRef = taskRef
        onPatchTask(taskRef, text, state, due, remind, click) { success ->
            pendingTaskRef = null
            if (success) {
                taskTextEditorState = null
                taskScheduleEditorState = null
                onSuccess?.invoke()
            }
        }
    }

    fun insertUploadedDocument(document: DocumentRecord) {
        val markdownLink = markdownLinkForDocument(document, pagePath)
        when (editorMode) {
            NoteEditorMode.Raw -> {
                val nextValue = replaceSelectionWithSnippet(
                    value = rawEditorValue,
                    replacement = markdownLink,
                    cursorOffset = markdownLink.length,
                )
                rawEditorValue = nextValue
                draftMarkdown = nextValue.text
            }
            NoteEditorMode.Preview, NoteEditorMode.Edit -> {
                syncGuiBlocksFromMarkdown(currentMarkdownDraft())
                val block = if (documentEmbedsInline(document)) {
                    NoteEditorBlock.Image(
                        alt = document.name.ifBlank { pageTitleFromPath(document.path) },
                        target = relativeDocumentPath(pagePath, document.path),
                    )
                } else {
                    NoteEditorBlock.Paragraph(markdownLink)
                }
                insertBlockAtAnchor(block, pendingBlockInsertAnchor)
            }
        }
    }

    val uploadDocumentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) {
            pendingBlockInsertAnchor = null
            return@rememberLauncherForActivityResult
        }
        isUploadingDocument = true
        onUploadDocument(uri) { document ->
            isUploadingDocument = false
            if (document != null) {
                insertUploadedDocument(document)
            } else {
                pendingBlockInsertAnchor = null
            }
        }
    }

    suspend fun downloadImagePayload(image: MarkdownImageTarget): ImageDownloadPayload? {
        val request = resolveImageRequest(
            target = image.target,
            currentPagePath = pagePath,
            settings = settings,
        ) ?: return null
        val content = downloadContent(request, settings.bearerToken) ?: return null
        val fileName = suggestImageFileName(
            currentPagePath = pagePath,
            image = image,
            mimeType = content.contentType,
        )
        return ImageDownloadPayload(
            bytes = content.bytes,
            fileName = fileName,
            mimeType = normalizeMimeType(content.contentType, fileName),
        )
    }

    val saveImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
        val image = pendingImageSaveTarget
        pendingImageSaveTarget = null
        if (uri == null || image == null) return@rememberLauncherForActivityResult
        coroutineScope.launch {
            val payload = downloadImagePayload(image)
            if (payload == null) {
                snackbarHostState.showSnackbar("Image could not be saved.")
                return@launch
            }
            val saved = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(payload.bytes)
                    } != null
                }.getOrDefault(false)
            }
            snackbarHostState.showSnackbar(
                if (saved) "Image saved." else "Image could not be saved.",
            )
        }
    }

    fun openImageActions(image: MarkdownImageTarget) {
        blockActionMenuState = null
        imageActionTarget = image
    }

    fun saveImage(image: MarkdownImageTarget) {
        imageActionTarget = null
        pendingImageSaveTarget = image
        saveImageLauncher.launch(
            suggestImageFileName(
                currentPagePath = pagePath,
                image = image,
                mimeType = null,
            ),
        )
    }

    fun shareImage(image: MarkdownImageTarget) {
        imageActionTarget = null
        coroutineScope.launch {
            val payload = downloadImagePayload(image)
            if (payload == null) {
                snackbarHostState.showSnackbar("Image could not be shared.")
                return@launch
            }
            val shareUri = withContext(Dispatchers.IO) {
                runCatching {
                    writeSharedImageFile(context, payload)
                }.getOrNull()
            }
            if (shareUri == null) {
                snackbarHostState.showSnackbar("Image could not be shared.")
                return@launch
            }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = payload.mimeType
                putExtra(Intent.EXTRA_STREAM, shareUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            runCatching {
                context.startActivity(Intent.createChooser(shareIntent, "Share image"))
            }.onFailure {
                snackbarHostState.showSnackbar("Image could not be shared.")
            }
        }
    }

    fun handleLink(target: String) {
        val trimmedTarget = target.trim()
        when {
            trimmedTarget.isBlank() || trimmedTarget.startsWith('#') -> Unit
            Regex("^[a-z]+:", RegexOption.IGNORE_CASE).containsMatchIn(trimmedTarget) -> {
                uriHandler.openUri(trimmedTarget)
            }
            trimmedTarget.startsWith("/") -> {
                val baseUrl = normalizeServerBaseUrl(settings.serverUrl)
                if (baseUrl.isNotBlank()) {
                    uriHandler.openUri("$baseUrl$trimmedTarget")
                }
            }
            else -> {
                val resolvedTarget = resolvePageLinkTarget(
                    currentPagePath = pagePath,
                    target = trimmedTarget,
                    scopePrefix = settings.scopePrefix,
                )
                if (resolvedTarget.isBlank()) return
                if (looksLikeDocumentPath(resolvedTarget)) {
                    val url = documentDownloadUrl(settings.serverUrl, resolvedTarget)
                    if (url.isNotBlank()) {
                        uriHandler.openUri(url)
                    }
                    return
                }
                onOpenPage(resolvedTarget)
            }
        }
    }

    fun handleNoteBack() {
        val hasUnsavedDraft = currentMarkdownDraft() != baseMarkdown
        when {
            frontmatterDraftState != null -> frontmatterDraftState = null
            isFrontmatterSheetVisible -> dismissFrontmatterSheet()
            isPageHistorySheetVisible -> isPageHistorySheetVisible = false
            isRenamePageSheetVisible -> isRenamePageSheetVisible = false
            isPageActionsSheetVisible -> isPageActionsSheetVisible = false
            isQueryInsertSheetVisible && !isGeneratingQuery -> dismissQueryInsertSheet()
            blockActionMenuState != null -> blockActionMenuState = null
            imageActionTarget != null -> imageActionTarget = null
            textBlockEditorState != null -> textBlockEditorState = null
            taskTextEditorState != null -> taskTextEditorState = null
            taskScheduleEditorState != null -> taskScheduleEditorState = null
            tableEditorState != null -> tableEditorState = null
            editorMode != NoteEditorMode.Preview -> enterPreviewMode()
            hasUnsavedDraft -> {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Use Done or Cancel before leaving this note.")
                }
            }
            else -> onBack()
        }
    }

    val renderedMarkdown = currentMarkdownDraft()
    val isDirty = renderedMarkdown != baseMarkdown
    val isEditMode = editorMode == NoteEditorMode.Edit
    val backlinks = if (isDirty || editorMode != NoteEditorMode.Preview) emptyList() else derived?.backlinks.orEmpty()
    val queryBlocks = if (isDirty || isEditMode) emptyList() else derived?.queryBlocks.orEmpty()
    val previewItems = remember(renderedMarkdown, queryBlocks) {
        buildNotePreviewItems(renderedMarkdown, queryBlocks)
    }
    val activePageTasks = if (!isDirty) pageTasks else emptyList()
    val pageTaskByRef = remember(activePageTasks) {
        activePageTasks
            .mapNotNull { task ->
                task.ref.trim().takeIf(String::isNotBlank)?.let { ref -> ref to task }
            }
            .toMap()
    }
    val bodyLineOffset = remember(baseMarkdown) {
        markdownBodyLineOffset(baseMarkdown)
    }
    val pageTaskByPreviewLine = remember(activePageTasks, bodyLineOffset) {
        activePageTasks.mapNotNull { task ->
            val absoluteLine = task.line ?: return@mapNotNull null
            val previewLine = absoluteLine - bodyLineOffset
            if (previewLine <= 0) {
                null
            } else {
                previewLine to task
            }
        }.toMap()
    }
    val openTaskText = taskTextEditorState?.let { state ->
        pageTaskByRef[state.taskRef]
    }
    val openTaskSchedule = taskScheduleEditorState?.let { state ->
        pageTaskByRef[state.taskRef]
    }
    val openTextBlock = textBlockEditorState?.let { state ->
        guiBlocks.getOrNull(state.blockIndex)?.takeIf(::isEditableTextBlock)
    }
    val openTable = tableEditorState?.let { state ->
        (guiBlocks.getOrNull(state.blockIndex) as? NoteEditorBlock.Table)?.let(::normalizeTableBlock)
    }
    val openTableLine = tableEditorState?.let { state ->
        previewItems.firstOrNull { item ->
            when (item) {
                is NotePreviewItem.MarkdownBlock -> item.blockIndex == state.blockIndex
                is NotePreviewItem.QueryResult -> item.blockIndex == state.blockIndex
            }
        }?.let { item ->
            when (item) {
                is NotePreviewItem.MarkdownBlock -> item.startLine
                is NotePreviewItem.QueryResult -> item.startLine
            }
        }
    }

    BackHandler(onBack = ::handleNoteBack)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onShowGlobalMenu,
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = MaterialTheme.colorScheme.onPrimary)
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        displayTitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = ::handleNoteBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    when {
                        editorMode == NoteEditorMode.Edit -> {
                            TextButton(onClick = { cancelEditing() }) {
                                Text("Cancel")
                            }
                            TextButton(
                                onClick = {
                                    if (isDirty) {
                                        saveDraft { enterPreviewMode() }
                                    } else {
                                        enterPreviewMode()
                                    }
                                },
                                enabled = !isSaving,
                            ) {
                                if (isSaving) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                } else {
                                    Text("Done")
                                }
                            }
                        }
                        editorMode == NoteEditorMode.Raw -> {
                            TextButton(onClick = { cancelEditing() }) {
                                Text("Cancel")
                            }
                            TextButton(
                                onClick = {
                                    if (isDirty) {
                                        saveDraft { enterPreviewMode() }
                                    } else {
                                        enterPreviewMode()
                                    }
                                },
                                enabled = !isSaving,
                            ) {
                                if (isSaving) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                } else {
                                    Text("Done")
                                }
                            }
                        }
                        isDirty -> {
                            TextButton(onClick = { cancelEditing() }) {
                                Text("Cancel")
                            }
                            TextButton(
                                onClick = {
                                    saveDraft { enterPreviewMode() }
                                },
                                enabled = !isSaving,
                            ) {
                                if (isSaving) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                } else {
                                    Text("Done")
                                }
                            }
                        }
                        else -> {
                            IconButton(
                                onClick = { openFrontmatterSheet() },
                                enabled = !isSaving && content != null,
                            ) {
                                Icon(Icons.Default.Tune, contentDescription = "Frontmatter")
                            }
                            IconButton(
                                onClick = {
                                    isPageHistorySheetVisible = true
                                    onLoadPageHistory()
                                },
                                enabled = !isSaving && content != null && !isPageHistoryBusy,
                            ) {
                                Icon(Icons.Default.History, contentDescription = "History")
                            }
                            TextButton(
                                onClick = { isPageActionsSheetVisible = true },
                                enabled = !isSaving && content != null,
                            ) {
                                Text("Note")
                            }
                            TextButton(
                                onClick = { enterEditMode() },
                                enabled = content != null || draftMarkdown.isNotBlank(),
                            ) {
                                Text("Edit")
                            }
                            TextButton(
                                onClick = { openRawModeAtLine() },
                                enabled = !isSaving,
                            ) {
                                Text("Raw")
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { innerPadding ->
        when {
            isLoading -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            editorMode == NoteEditorMode.Raw -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .imePadding()
                        .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = noteContentBottomPadding),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = displayPath,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = rawEditorValue,
                        onValueChange = {
                            rawEditorValue = it
                            draftMarkdown = it.text
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .focusRequester(focusRequester),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                        enabled = !isSaving,
                    )
                }
            }
            else -> {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = noteContentBottomPadding),
                ) {
                    Text(
                        text = displayPath,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    if (!isDirty && visibleFrontmatterEntries.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        FrontmatterPanel(
                            entries = visibleFrontmatterEntries,
                            scopePrefix = settings.scopePrefix,
                            onOpenPage = onOpenPage,
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    if (isEditMode) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = "Edit mode: tap a text block to edit it. Long press a block for add, upload, or delete actions.",
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                    NotePreviewSurface(
                        items = previewItems,
                        editInteractionsEnabled = isEditMode,
                        taskByPreviewLine = pageTaskByPreviewLine,
                        taskInteractionsEnabled = !isDirty,
                        pendingTaskRef = pendingTaskRef,
                        currentPagePath = pagePath,
                        settings = settings,
                        scopePrefix = settings.scopePrefix,
                        onSelectBlock = { selectedBlockIndex = it },
                        onLinkClick = ::handleLink,
                        onImageClick = ::openImageActions,
                        onEditTextBlock = if (isEditMode) ::openTextBlockEditor else null,
                        onOpenBlockActions = if (isEditMode) ::openBlockActions else null,
                        onEditQuery = if (isEditMode) {
                            { line -> openRawModeAtLine(line) }
                        } else {
                            null
                        },
                        onEditTableCell = { blockIndex, rowIndex, columnIndex ->
                            openTableEditor(blockIndex, rowIndex, columnIndex)
                        },
                        onToggleTask = { task ->
                            task.ref.takeIf(String::isNotBlank)?.let { ref ->
                                patchTask(
                                    taskRef = ref,
                                    state = if (task.done) "todo" else "done",
                                )
                            }
                        },
                        onEditTaskText = { task ->
                            task.ref.takeIf(String::isNotBlank)?.let { ref ->
                                taskTextEditorState = TaskTextEditorState(
                                    taskRef = ref,
                                    lineNumber = task.line ?: 0,
                                )
                            }
                        },
                        onEditTaskSchedule = { task ->
                            task.ref.takeIf(String::isNotBlank)?.let { ref ->
                                taskScheduleEditorState = TaskScheduleEditorState(
                                    taskRef = ref,
                                    lineNumber = task.line ?: 0,
                                )
                            }
                        },
                    )

                    if (isDirty && renderedMarkdown.contains("```query")) {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Query results update after save.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    if (backlinks.isNotEmpty()) {
                        Spacer(Modifier.height(20.dp))
                        BacklinksSection(
                            backlinks = backlinks,
                            scopePrefix = settings.scopePrefix,
                            onOpenPage = onOpenPage,
                        )
                    }
                }
            }
        }
    }

    if (tableEditorState != null && openTable != null) {
        val editorState = tableEditorState!!
        TableEditorSheet(
            table = openTable,
            lineNumber = openTableLine,
            selection = editorState.cell,
            onDismiss = { tableEditorState = null },
            onSelectCell = { selection ->
                selectedBlockIndex = editorState.blockIndex
                tableEditorState = editorState.copy(cell = selection)
            },
            onValueChange = { value ->
                updateOpenTable { table ->
                    if (editorState.cell.rowIndex == null) {
                        val nextHeaders = table.headers.toMutableList()
                        nextHeaders[editorState.cell.columnIndex] = value
                        table.copy(headers = nextHeaders) to editorState.cell
                    } else {
                        val nextRows = table.rows.mapIndexed { rowIndex, row ->
                            if (rowIndex != editorState.cell.rowIndex) row else {
                                row.mapIndexed { columnIndex, cell ->
                                    if (columnIndex == editorState.cell.columnIndex) value else cell
                                }
                            }
                        }
                        table.copy(rows = nextRows) to editorState.cell
                    }
                }
            },
            onAddRowBelow = {
                updateOpenTable { table ->
                    val insertIndex = ((editorState.cell.rowIndex ?: -1) + 1).coerceIn(0, table.rows.size)
                    val nextRows = table.rows.toMutableList()
                    nextRows.add(insertIndex, List(table.headers.size) { "" })
                    table.copy(rows = nextRows) to editorState.cell.copy(rowIndex = insertIndex)
                }
            },
            onAddColumnRight = {
                updateOpenTable { table ->
                    val insertIndex = (editorState.cell.columnIndex + 1).coerceIn(0, table.headers.size)
                    val nextHeaders = table.headers.toMutableList()
                    nextHeaders.add(insertIndex, "Column ${insertIndex + 1}")
                    val nextRows = table.rows.map { row ->
                        row.toMutableList().apply {
                            add(insertIndex, "")
                        }
                    }
                    table.copy(headers = nextHeaders, rows = nextRows) to editorState.cell.copy(columnIndex = insertIndex)
                }
            },
            onDeleteRow = if (editorState.cell.rowIndex != null) {
                {
                    updateOpenTable { table ->
                        val rowIndex = editorState.cell.rowIndex
                        if (rowIndex !in table.rows.indices) {
                            table to editorState.cell
                        } else {
                            val nextRows = table.rows.toMutableList()
                            nextRows.removeAt(rowIndex)
                            val nextSelection = editorState.cell.copy(
                                rowIndex = when {
                                    nextRows.isEmpty() -> null
                                    rowIndex > nextRows.lastIndex -> nextRows.lastIndex
                                    else -> rowIndex
                                },
                            )
                            table.copy(rows = nextRows) to nextSelection
                        }
                    }
                }
            } else {
                null
            },
            onDeleteColumn = if (openTable.headers.size > 2) {
                {
                    updateOpenTable { table ->
                        val columnIndex = editorState.cell.columnIndex.coerceIn(0, table.headers.lastIndex)
                        val nextHeaders = table.headers.filterIndexed { index, _ -> index != columnIndex }
                        val nextRows = table.rows.map { row ->
                            row.filterIndexed { index, _ -> index != columnIndex }
                        }
                        val nextSelection = editorState.cell.copy(
                            columnIndex = columnIndex.coerceAtMost(nextHeaders.lastIndex),
                        )
                        table.copy(headers = nextHeaders, rows = nextRows) to nextSelection
                    }
                }
            } else {
                null
            },
        )
    }

    if (textBlockEditorState != null && openTextBlock != null) {
        val editorState = textBlockEditorState!!
        TextBlockEditorSheet(
            block = openTextBlock,
            lineNumber = editorState.lineNumber.takeIf { it > 0 },
            onDismiss = { textBlockEditorState = null },
            onSave = { updatedBlock ->
                updateTextBlock(editorState.blockIndex) { updatedBlock }
                textBlockEditorState = null
            },
        )
    }

    if (taskTextEditorState != null && openTaskText != null) {
        val editorState = taskTextEditorState!!
        TaskTextEditorSheet(
            task = openTaskText,
            lineNumber = editorState.lineNumber.takeIf { it > 0 },
            isSaving = pendingTaskRef == openTaskText.ref,
            onDismiss = { taskTextEditorState = null },
            onSave = { nextText ->
                patchTask(
                    taskRef = openTaskText.ref,
                    text = nextText,
                )
            },
        )
    }

    if (taskScheduleEditorState != null && openTaskSchedule != null) {
        val editorState = taskScheduleEditorState!!
        TaskScheduleSheet(
            task = openTaskSchedule,
            lineNumber = editorState.lineNumber.takeIf { it > 0 },
            isSaving = pendingTaskRef == openTaskSchedule.ref,
            onDismiss = { taskScheduleEditorState = null },
            onSave = { dueValue, remindValue ->
                val clickValue = when {
                    remindValue.isNotBlank() && openTaskSchedule.click.isNullOrBlank() -> taskReminderClickTarget
                    remindValue.isBlank() && openTaskSchedule.click == taskReminderClickTarget -> ""
                    else -> null
                }
                patchTask(
                    taskRef = openTaskSchedule.ref,
                    due = dueValue,
                    remind = remindValue,
                    click = clickValue,
                )
            },
        )
    }

    if (isFrontmatterSheetVisible) {
        FrontmatterEditorSheet(
            entries = visibleFrontmatterEntries,
            draft = frontmatterDraftState,
            isSaving = isPatchingFrontmatter,
            onDismiss = { dismissFrontmatterSheet() },
            onAddProperty = { startFrontmatterAdd() },
            onEditProperty = ::startFrontmatterEdit,
            onDeleteProperty = ::deleteFrontmatterProperty,
            onDraftChange = { frontmatterDraftState = it },
            onDismissDraft = { frontmatterDraftState = null },
            onSaveDraft = { saveFrontmatterDraft() },
        )
    }

    imageActionTarget?.let { image ->
        ImageActionSheet(
            image = image,
            currentPagePath = pagePath,
            onDismiss = { imageActionTarget = null },
            onSave = { saveImage(image) },
            onShare = { shareImage(image) },
        )
    }

    if (isQueryInsertSheetVisible) {
        QueryInsertSheet(
            intentText = queryInsertDraft,
            isGenerating = isGeneratingQuery,
            onDismiss = ::dismissQueryInsertSheet,
            onIntentTextChange = { queryInsertDraft = it },
            onUseExample = { queryInsertDraft = it },
            onGenerate = { generateAndInsertQueryBlock(queryInsertDraft) },
        )
    }

    blockActionMenuState?.let { target ->
        BlockActionMenuSheet(
            lineNumber = target.lineNumber,
            canDelete = target.blockIndex in guiBlocks.indices,
            placement = target.placement,
            isUploadingDocument = isUploadingDocument,
            onDismiss = { blockActionMenuState = null },
            onPlacementChange = { placement ->
                blockActionMenuState = target.copy(placement = placement)
            },
            onInsertQuery = { openQueryInsertSheet() },
            onUploadFile = {
                pendingBlockInsertAnchor = PendingBlockInsertAnchor(
                    blockIndex = target.blockIndex,
                    placement = target.placement,
                )
                blockActionMenuState = null
                uploadDocumentLauncher.launch("*/*")
            },
            onDelete = if (target.blockIndex in guiBlocks.indices) {
                { deleteBlockAtIndex(target.blockIndex) }
            } else {
                null
            },
            onApplyCommand = { command ->
                applyBlockInsertCommand(command)
            },
        )
    }

    if (isPageActionsSheetVisible) {
        NoteActionsSheet(
            pagePath = displayPath,
            isBusy = isPerformingPageAction || isPageHistoryBusy,
            onDismiss = {
                if (!isPerformingPageAction && !isPageHistoryBusy) {
                    isPageActionsSheetVisible = false
                }
            },
            onHistory = {
                isPageActionsSheetVisible = false
                isPageHistorySheetVisible = true
                onLoadPageHistory()
            },
            onRename = {
                isPageActionsSheetVisible = false
                isRenamePageSheetVisible = true
            },
            onDelete = {
                isPerformingPageAction = true
                onDeletePage { success ->
                    isPerformingPageAction = false
                    if (success) {
                        isPageActionsSheetVisible = false
                    }
                }
            },
        )
    }

    if (isPageHistorySheetVisible) {
        PageHistorySheet(
            pagePath = displayPath,
            revisions = pageHistory,
            isLoading = isPageHistoryLoading,
            isBusy = isPageHistoryBusy,
            onDismiss = {
                if (!isPageHistoryBusy) {
                    isPageHistorySheetVisible = false
                }
            },
            onRefresh = onLoadPageHistory,
            onRestore = { revisionId ->
                onRestorePageHistory(revisionId) { success ->
                    if (success) {
                        isPageHistorySheetVisible = false
                    }
                }
            },
            onPurge = {
                onPurgePageHistory { }
            },
        )
    }

    if (isRenamePageSheetVisible) {
        RenameNoteSheet(
            pagePath = displayPath,
            isSaving = isPerformingPageAction,
            onDismiss = {
                if (!isPerformingPageAction) {
                    isRenamePageSheetVisible = false
                }
            },
            onSave = { nextPagePath ->
                isPerformingPageAction = true
                onRenamePage(nextPagePath) { success ->
                    isPerformingPageAction = false
                    if (success) {
                        isRenamePageSheetVisible = false
                    }
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteActionsSheet(
    pagePath: String,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onHistory: (() -> Unit)? = null,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Note actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = pagePath,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (onHistory != null) {
                Button(
                    onClick = onHistory,
                    enabled = !isBusy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Revision history")
                }
            }
            Button(
                onClick = onRename,
                enabled = !isBusy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Rename or move note")
            }
            TextButton(
                onClick = onDelete,
                enabled = !isBusy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isBusy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        text = "Move note to trash",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RenameNoteSheet(
    pagePath: String,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var pagePathValue by remember(pagePath) {
        mutableStateOf(
            TextFieldValue(
                text = pagePath,
                selection = androidx.compose.ui.text.TextRange(pagePath.length),
            ),
        )
    }

    LaunchedEffect(pagePath) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Rename or move note",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Enter a nested path to move this note within the current scope.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = pagePathValue,
                onValueChange = { pagePathValue = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                enabled = !isSaving,
                label = { Text("Note path") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (!isSaving) {
                        onSave(pagePathValue.text.trim())
                    }
                }),
                singleLine = true,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                TextButton(onClick = onDismiss, enabled = !isSaving) {
                    Text("Cancel")
                }
                Button(
                    onClick = { onSave(pagePathValue.text.trim()) },
                    enabled = !isSaving,
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text("Done")
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateNoteSheet(
    pagePath: String,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var pagePathValue by remember(pagePath) {
        mutableStateOf(
            TextFieldValue(
                text = pagePath,
                selection = androidx.compose.ui.text.TextRange(pagePath.length),
            ),
        )
    }

    LaunchedEffect(pagePath) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "New note",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Enter a name or nested path to create a note within the current scope.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = pagePathValue,
                onValueChange = { pagePathValue = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                enabled = !isSaving,
                label = { Text("Note path") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (!isSaving) {
                        onSave(pagePathValue.text.trim())
                    }
                }),
                singleLine = true,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                TextButton(onClick = onDismiss, enabled = !isSaving) {
                    Text("Cancel")
                }
                Button(
                    onClick = { onSave(pagePathValue.text.trim()) },
                    enabled = !isSaving,
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text("Create")
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QueryInsertSheet(
    intentText: String,
    isGenerating: Boolean,
    onDismiss: () -> Unit,
    onIntentTextChange: (String) -> Unit,
    onUseExample: (String) -> Unit,
    onGenerate: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var intentValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = intentText,
                selection = androidx.compose.ui.text.TextRange(intentText.length),
            ),
        )
    }

    LaunchedEffect(intentText) {
        if (intentValue.text != intentText) {
            intentValue = TextFieldValue(
                text = intentText,
                selection = androidx.compose.ui.text.TextRange(intentText.length),
            )
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Generate query",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Describe what the query should do. The server AI drafts a fenced query block and inserts it into the note.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AssistChip(
                    onClick = { onUseExample("show open tasks due this week") },
                    enabled = !isGenerating,
                    label = { Text("Open tasks") },
                )
                AssistChip(
                    onClick = { onUseExample("show recently updated project pages") },
                    enabled = !isGenerating,
                    label = { Text("Recent pages") },
                )
            }
            OutlinedTextField(
                value = intentValue,
                onValueChange = {
                    intentValue = it
                    onIntentTextChange(it.text)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                enabled = !isGenerating,
                label = { Text("Intent") },
                placeholder = {
                    Text(
                        text = "show all contacts with birthday reminders",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                minLines = 3,
                maxLines = 6,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (!isGenerating && intentValue.text.isNotBlank()) {
                            keyboardController?.hide()
                            onGenerate()
                        }
                    },
                ),
            )
            if (isGenerating) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                TextButton(onClick = onDismiss, enabled = !isGenerating) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        keyboardController?.hide()
                        onGenerate()
                    },
                    enabled = !isGenerating && intentText.isNotBlank(),
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text("Generate")
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FolderActionsSheet(
    folderPath: String,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onCreateSubfolder: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Folder actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = folderPath,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onCreateSubfolder,
                enabled = !isBusy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Create subfolder")
            }
            Button(
                onClick = onRename,
                enabled = !isBusy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Rename or move folder")
            }
            Text(
                text = "Deleting a folder moves markdown notes inside it to trash. Empty folders and non-note files are deleted immediately.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(
                onClick = onDelete,
                enabled = !isBusy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isBusy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        text = "Delete folder",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FolderPathSheet(
    title: String,
    supportingText: String,
    folderPath: String,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var folderPathValue by remember(folderPath) {
        mutableStateOf(
            TextFieldValue(
                text = folderPath,
                selection = androidx.compose.ui.text.TextRange(folderPath.length),
            ),
        )
    }

    LaunchedEffect(folderPath) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = folderPathValue,
                onValueChange = { folderPathValue = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                enabled = !isSaving,
                label = { Text("Folder path") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (!isSaving) {
                        onSave(folderPathValue.text.trim())
                    }
                }),
                singleLine = true,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                TextButton(onClick = onDismiss, enabled = !isSaving) {
                    Text("Cancel")
                }
                Button(
                    onClick = { onSave(folderPathValue.text.trim()) },
                    enabled = !isSaving,
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text("Done")
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

private fun buildNotePreviewItems(markdown: String, queryBlocks: List<QueryBlock>): List<NotePreviewItem> {
    val parsedBlocks = parseNoteEditorBlocksWithLines(markdown)
    if (parsedBlocks.isEmpty()) {
        return emptyList()
    }
    val bodyLines = splitMarkdownFrontmatter(markdown).body.replace("\r\n", "\n").split('\n')

    val queuedQueries = mutableMapOf<String, ArrayDeque<QueryBlock>>()
    queryBlocks.forEach { queryBlock ->
        val key = normalizeMarkdownSource(queryBlock.source)
        val queue = queuedQueries.getOrPut(key) { ArrayDeque() }
        queue.addLast(queryBlock)
    }

    return buildList {
        parsedBlocks.forEachIndexed { blockIndex, parsedBlock ->
            val block = parsedBlock.block
            val queryFence = block as? NoteEditorBlock.CodeFence
            if (queryFence?.language.equals("query", ignoreCase = true)) {
                val querySource = normalizeMarkdownSource(
                    bodyLines
                        .subList(
                            (parsedBlock.startLine - 1).coerceAtLeast(0),
                            parsedBlock.endLine.coerceAtMost(bodyLines.size),
                        )
                        .joinToString("\n"),
                )
                val matchingQuery = queuedQueries[querySource]?.removeFirstOrNull()
                if (matchingQuery != null) {
                    add(
                        NotePreviewItem.QueryResult(
                            blockIndex = blockIndex,
                            startLine = parsedBlock.startLine,
                            block = matchingQuery,
                        ),
                    )
                    return@forEachIndexed
                }
            }

            add(
                NotePreviewItem.MarkdownBlock(
                    blockIndex = blockIndex,
                    block = block,
                    startLine = parsedBlock.startLine,
                    endLine = parsedBlock.endLine,
                ),
            )
        }
    }
}

private fun normalizeMarkdownSource(source: String): String {
    return source.replace("\r\n", "\n").trim()
}

private sealed interface QueryResultLinkSpec {
    val hiddenColumns: Set<String>

    data class Synthetic(
        val column: String,
        val label: String,
        val text: String,
        override val hiddenColumns: Set<String>,
    ) : QueryResultLinkSpec

    data class Column(
        val columns: Set<String>,
        override val hiddenColumns: Set<String>,
    ) : QueryResultLinkSpec
}

private fun queryRowValue(row: Map<String, kotlinx.serialization.json.JsonElement>?, column: String): String {
    val element = row?.get(column) ?: return ""
    return jsonElementDisplayValue(element).trim()
}

private fun queryRowPagePath(row: Map<String, kotlinx.serialization.json.JsonElement>?): String {
    val directPath = queryRowValue(row, "path")
    if (directPath.isNotBlank()) {
        return directPath
    }
    return queryRowValue(row, "__pagePath")
}

private fun queryResultLinkSpec(
    columns: List<String>,
    row: Map<String, kotlinx.serialization.json.JsonElement>?,
): QueryResultLinkSpec? {
    if (row == null) return null
    val pagePath = queryRowPagePath(row)
    if (pagePath.isBlank()) return null

    if ("vorname" in columns && "nachname" in columns) {
        val first = queryRowValue(row, "vorname")
        val last = queryRowValue(row, "nachname")
        val label = listOf(first, last).filter(String::isNotBlank).joinToString(" ").trim()
        if (label.isNotBlank()) {
            return QueryResultLinkSpec.Synthetic(
                column = "__page_link__",
                label = "Name",
                text = label,
                hiddenColumns = setOf("path", "vorname", "nachname"),
            )
        }
    }

    val directCandidates = listOf("title", "name", "vorname", "nachname")
    directCandidates.forEach { candidate ->
        if (candidate in columns && queryRowValue(row, candidate).isNotBlank()) {
            return QueryResultLinkSpec.Column(
                columns = if (candidate == "title" || candidate == "name") {
                    setOf(candidate)
                } else {
                    listOf("nachname", "vorname")
                        .filter { field -> field in columns && queryRowValue(row, field).isNotBlank() }
                        .toSet()
                },
                hiddenColumns = if ("path" in columns) setOf("path") else emptySet(),
            )
        }
    }

    return null
}

private fun queryResultDisplayColumns(
    columns: List<String>,
    rows: List<Map<String, kotlinx.serialization.json.JsonElement>>,
): List<String> {
    val linkSpec = queryResultLinkSpec(columns, rows.firstOrNull())
    return when (linkSpec) {
        null -> columns
        is QueryResultLinkSpec.Synthetic -> {
            listOf(linkSpec.column) + columns.filterNot { it in linkSpec.hiddenColumns }
        }
        is QueryResultLinkSpec.Column -> {
            columns.filterNot { it in linkSpec.hiddenColumns }
        }
    }
}

private fun queryResultColumnLabel(
    column: String,
    columns: List<String>,
    row: Map<String, kotlinx.serialization.json.JsonElement>?,
): String {
    val linkSpec = queryResultLinkSpec(columns, row)
    return if (linkSpec is QueryResultLinkSpec.Synthetic && column == linkSpec.column) {
        linkSpec.label
    } else {
        column
    }
}

private fun queryResultDisplayCellText(
    column: String,
    row: Map<String, kotlinx.serialization.json.JsonElement>,
    columns: List<String>,
    scopePrefix: String,
    linkColor: Color,
): androidx.compose.ui.text.AnnotatedString {
    val linkSpec = queryResultLinkSpec(columns, row)
    val pagePath = queryRowPagePath(row)
    if (pagePath.isNotBlank()) {
        when (linkSpec) {
            is QueryResultLinkSpec.Synthetic -> {
                if (column == linkSpec.column) {
                    return parseInlineMarkdown("[[$pagePath|${linkSpec.text}]]", linkColor)
                }
            }
            is QueryResultLinkSpec.Column -> {
                if (column in linkSpec.columns) {
                    return parseInlineMarkdown(
                        "[[$pagePath|${queryRowValue(row, column)}]]",
                        linkColor,
                    )
                }
            }
            null -> Unit
        }
        if (column == "text" || (column == "task" && queryRowValue(row, "__taskRef").isNotBlank())) {
            return parseInlineMarkdown("[[$pagePath|${queryRowValue(row, column)}]]", linkColor)
        }
    }
    return queryCellAnnotatedText(column, row[column] ?: JsonNull, scopePrefix, linkColor)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NotePreviewSurface(
    items: List<NotePreviewItem>,
    editInteractionsEnabled: Boolean,
    taskByPreviewLine: Map<Int, ApiTaskItem>,
    taskInteractionsEnabled: Boolean,
    pendingTaskRef: String?,
    currentPagePath: String,
    settings: dev.carnager.noterious.data.AppSettings,
    scopePrefix: String,
    onSelectBlock: (Int) -> Unit,
    onLinkClick: (String) -> Unit,
    onImageClick: ((MarkdownImageTarget) -> Unit)?,
    onEditTextBlock: ((Int, Int) -> Unit)?,
    onOpenBlockActions: ((Int, Int) -> Unit)?,
    onEditQuery: ((Int) -> Unit)?,
    onEditTableCell: ((Int, Int?, Int) -> Unit)?,
    onToggleTask: (ApiTaskItem) -> Unit,
    onEditTaskText: (ApiTaskItem) -> Unit,
    onEditTaskSchedule: (ApiTaskItem) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (items.isEmpty()) {
            if (editInteractionsEnabled && onOpenBlockActions != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenBlockActions(-1, 1) },
                ) {
                    Text(
                        text = "Add the first block.",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Text(
                    text = "This note is empty.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Column
        }

        items.forEach { item ->
            val blockIndex = when (item) {
                is NotePreviewItem.MarkdownBlock -> item.blockIndex
                is NotePreviewItem.QueryResult -> item.blockIndex
            }
            val lineNumber = when (item) {
                is NotePreviewItem.MarkdownBlock -> item.startLine
                is NotePreviewItem.QueryResult -> item.startLine
            }

            when (item) {
                is NotePreviewItem.MarkdownBlock -> {
                    when (val block = item.block) {
                        is NoteEditorBlock.Table -> {
                            InteractiveTablePreview(
                                table = block,
                                onCellClick = if (onEditTableCell != null) {
                                    { cell ->
                                        onSelectBlock(item.blockIndex)
                                        onEditTableCell(item.blockIndex, cell.rowIndex, cell.columnIndex)
                                    }
                                } else {
                                    null
                                },
                                onCellLongPress = if (editInteractionsEnabled && onOpenBlockActions != null) {
                                    {
                                        onSelectBlock(item.blockIndex)
                                        onOpenBlockActions(item.blockIndex, lineNumber)
                                    }
                                } else {
                                    null
                                },
                            )
                        }
                        is NoteEditorBlock.TaskItem -> {
                            val task = taskByPreviewLine[item.startLine]
                            TaskPreviewRow(
                                text = task?.text?.ifBlank { stripTaskInlineFields(block.text) }
                                    ?: stripTaskInlineFields(block.text),
                                checked = task?.done ?: block.checked,
                                due = task?.due,
                                remind = task?.remind,
                                interactive = taskInteractionsEnabled && task?.ref?.isNotBlank() == true,
                                isBusy = pendingTaskRef != null && task?.ref == pendingTaskRef,
                                onToggle = if (taskInteractionsEnabled && task?.ref?.isNotBlank() == true) {
                                    { onToggleTask(task) }
                                } else {
                                    null
                                },
                                onEditText = if (taskInteractionsEnabled && task?.ref?.isNotBlank() == true) {
                                    { onEditTaskText(task) }
                                } else {
                                    null
                                },
                                onEditSchedule = if (taskInteractionsEnabled && task?.ref?.isNotBlank() == true) {
                                    { onEditTaskSchedule(task) }
                                } else {
                                    null
                                },
                                onLongPress = if (editInteractionsEnabled && onOpenBlockActions != null) {
                                    {
                                        onSelectBlock(item.blockIndex)
                                        onOpenBlockActions(item.blockIndex, lineNumber)
                                    }
                                } else {
                                    null
                                },
                                onLinkClick = if (taskInteractionsEnabled || editInteractionsEnabled) null else onLinkClick,
                            )
                        }
                        is NoteEditorBlock.Heading,
                        is NoteEditorBlock.Paragraph,
                        is NoteEditorBlock.BulletItem,
                        is NoteEditorBlock.NumberedItem,
                        is NoteEditorBlock.BlockQuote,
                        is NoteEditorBlock.CodeFence,
                        -> {
                            val blockModifier = if (editInteractionsEnabled) {
                                Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            onSelectBlock(blockIndex)
                                            onEditTextBlock?.invoke(blockIndex, lineNumber)
                                        },
                                        onLongClick = if (onOpenBlockActions != null) {
                                            {
                                                onSelectBlock(blockIndex)
                                                onOpenBlockActions(blockIndex, lineNumber)
                                            }
                                        } else {
                                            null
                                        },
                                    )
                            } else {
                                Modifier.fillMaxWidth()
                            }
                            MarkdownContent(
                                markdown = blockToMarkdown(block),
                                currentPagePath = currentPagePath,
                                settings = settings,
                                modifier = blockModifier,
                                onLinkClick = if (editInteractionsEnabled) null else onLinkClick,
                                onImageClick = if (editInteractionsEnabled) null else onImageClick,
                            )
                        }
                        is NoteEditorBlock.Image -> {
                            val imageTarget = remember(block.alt, block.target) {
                                MarkdownImageTarget(
                                    alt = block.alt,
                                    target = block.target,
                                )
                            }
                            val blockModifier = if (editInteractionsEnabled) {
                                Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = { onImageClick?.invoke(imageTarget) },
                                        onLongClick = if (onOpenBlockActions != null) {
                                            {
                                                onSelectBlock(blockIndex)
                                                onOpenBlockActions(blockIndex, lineNumber)
                                            }
                                        } else {
                                            null
                                        },
                                    )
                            } else {
                                Modifier.fillMaxWidth()
                            }
                            MarkdownContent(
                                markdown = blockToMarkdown(block),
                                currentPagePath = currentPagePath,
                                settings = settings,
                                modifier = blockModifier,
                                onLinkClick = if (editInteractionsEnabled) null else onLinkClick,
                                onImageClick = if (editInteractionsEnabled) null else onImageClick,
                            )
                        }
                    }
                }
                is NotePreviewItem.QueryResult -> QueryBlockCard(
                    block = item.block,
                    scopePrefix = scopePrefix,
                    onLinkClick = onLinkClick,
                    onEditQuery = if (editInteractionsEnabled && onEditQuery != null) {
                        { onEditQuery(item.block.line) }
                    } else {
                        null
                    },
                    onLongPress = if (editInteractionsEnabled && onOpenBlockActions != null) {
                        {
                            onSelectBlock(item.blockIndex)
                            onOpenBlockActions(item.blockIndex, lineNumber)
                        }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TaskPreviewRow(
    text: String,
    checked: Boolean,
    due: String?,
    remind: String?,
    interactive: Boolean,
    isBusy: Boolean,
    onToggle: (() -> Unit)?,
    onEditText: (() -> Unit)?,
    onEditSchedule: (() -> Unit)?,
    onLongPress: (() -> Unit)?,
    onLinkClick: ((String) -> Unit)?,
) {
    val linkColor = MaterialTheme.colorScheme.primary
    val metadata = remember(due, remind) { taskScheduleLabel(due, remind) }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = { onToggle?.invoke() },
                enabled = interactive && onToggle != null && !isBusy,
                modifier = Modifier.size(32.dp),
            ) {
                if (isBusy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(
                        imageVector = if (checked) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = if (checked) "Task als offen markieren" else "Task abschliessen",
                        tint = if (checked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if ((interactive && onEditText != null && !isBusy) || onLongPress != null) {
                            Modifier.combinedClickable(
                                enabled = !isBusy,
                                onClick = {
                                    if (interactive && onEditText != null) {
                                        onEditText()
                                    }
                                },
                                onLongClick = onLongPress,
                            )
                        } else {
                            Modifier
                        },
                    ),
            ) {
                MarkdownText(
                    text = parseInlineMarkdown(text.ifBlank { "Task" }, linkColor),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        textDecoration = if (checked) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                    ),
                    color = if (checked) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    onLinkClick = if (interactive || onLongPress != null) null else onLinkClick,
                )
            }

            IconButton(
                onClick = { onEditSchedule?.invoke() },
                enabled = interactive && onEditSchedule != null && !isBusy,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = if (!due.isNullOrBlank() || !remind.isNullOrBlank()) {
                        Icons.Default.Alarm
                    } else {
                        Icons.Default.AlarmOff
                    },
                    contentDescription = "Task-Termine",
                    tint = if (!due.isNullOrBlank() || !remind.isNullOrBlank()) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
                )
            }
        }

        if (metadata != null) {
            Text(
                text = metadata,
                modifier = Modifier.padding(start = 40.dp, end = 40.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskTextEditorSheet(
    task: ApiTaskItem,
    lineNumber: Int?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var textValue by remember(task.ref, task.text) {
        mutableStateOf(
            TextFieldValue(
                text = task.text,
                selection = androidx.compose.ui.text.TextRange(task.text.length),
            ),
        )
    }

    LaunchedEffect(task.ref) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = if (lineNumber != null) "Task · line $lineNumber" else "Task",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            OutlinedTextField(
                value = textValue,
                onValueChange = { textValue = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                enabled = !isSaving,
                label = { Text("Task text") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (!isSaving) {
                        onSave(textValue.text.trim())
                    }
                }),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                TextButton(onClick = onDismiss, enabled = !isSaving) {
                    Text("Cancel")
                }
                Button(
                    onClick = { onSave(textValue.text.trim()) },
                    enabled = !isSaving,
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text("Done")
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

private fun textEditorTitle(block: NoteEditorBlock): String {
    return when (block) {
        is NoteEditorBlock.Heading -> "Heading ${block.level}"
        is NoteEditorBlock.Paragraph -> "Paragraph"
        is NoteEditorBlock.BulletItem -> "Bullet Item"
        is NoteEditorBlock.NumberedItem -> "Numbered Item"
        is NoteEditorBlock.BlockQuote -> "Quote"
        is NoteEditorBlock.CodeFence -> "Code Block"
        else -> "Block"
    }
}

private fun textEditorLabel(block: NoteEditorBlock): String {
    return when (block) {
        is NoteEditorBlock.CodeFence -> "Code"
        else -> "Text"
    }
}

private fun textEditorInitialText(block: NoteEditorBlock): String {
    return when (block) {
        is NoteEditorBlock.Heading -> block.text
        is NoteEditorBlock.Paragraph -> block.text
        is NoteEditorBlock.BulletItem -> block.text
        is NoteEditorBlock.NumberedItem -> block.text
        is NoteEditorBlock.BlockQuote -> block.text
        is NoteEditorBlock.CodeFence -> block.text
        else -> ""
    }
}

private fun textEditorInitialLanguage(block: NoteEditorBlock): String {
    return when (block) {
        is NoteEditorBlock.CodeFence -> block.language
        else -> ""
    }
}

private fun textEditorUsesMultiline(block: NoteEditorBlock): Boolean {
    return when (block) {
        is NoteEditorBlock.Paragraph,
        is NoteEditorBlock.BlockQuote,
        is NoteEditorBlock.CodeFence,
        -> true

        else -> false
    }
}

private fun updatedTextEditorBlock(
    block: NoteEditorBlock,
    text: String,
    language: String,
): NoteEditorBlock {
    return when (block) {
        is NoteEditorBlock.Heading -> block.copy(text = text.trim())
        is NoteEditorBlock.Paragraph -> block.copy(text = text)
        is NoteEditorBlock.BulletItem -> block.copy(text = text.trim())
        is NoteEditorBlock.NumberedItem -> block.copy(text = text.trim())
        is NoteEditorBlock.BlockQuote -> block.copy(text = text)
        is NoteEditorBlock.CodeFence -> block.copy(text = text, language = language.trim())
        else -> block
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TextBlockEditorSheet(
    block: NoteEditorBlock,
    lineNumber: Int?,
    onDismiss: () -> Unit,
    onSave: (NoteEditorBlock) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var textValue by remember(block) {
        val initialText = textEditorInitialText(block)
        mutableStateOf(
            TextFieldValue(
                text = initialText,
                selection = androidx.compose.ui.text.TextRange(initialText.length),
            ),
        )
    }
    var languageValue by remember(block) { mutableStateOf(textEditorInitialLanguage(block)) }
    val usesMultiline = remember(block) { textEditorUsesMultiline(block) }

    LaunchedEffect(block) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = if (lineNumber != null) {
                    "${textEditorTitle(block)} · line $lineNumber"
                } else {
                    textEditorTitle(block)
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (block is NoteEditorBlock.CodeFence) {
                OutlinedTextField(
                    value = languageValue,
                    onValueChange = { languageValue = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Language") },
                    singleLine = true,
                )
            }
            OutlinedTextField(
                value = textValue,
                onValueChange = { textValue = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                label = { Text(textEditorLabel(block)) },
                singleLine = !usesMultiline,
                minLines = if (usesMultiline) 4 else 1,
                maxLines = if (usesMultiline) 10 else 1,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        onSave(
                            updatedTextEditorBlock(
                                block = block,
                                text = textValue.text,
                                language = languageValue,
                            ),
                        )
                    },
                ) {
                    Text("Done")
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun TaskScheduleSheet(
    task: ApiTaskItem,
    lineNumber: Int?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (due: String, remind: String) -> Unit,
) {
    val context = LocalContext.current
    var dueValue by remember(task.ref, task.due) { mutableStateOf(task.due.orEmpty()) }
    var remindValue by remember(task.ref, task.remind) { mutableStateOf(normalizeTaskReminderValue(task.remind)) }
    var openDatePicker by remember(task.ref) { mutableStateOf(false) }
    var openTimePicker by remember(task.ref) { mutableStateOf(false) }

    if (openDatePicker) {
        val initialDate = parseTaskDateValue(dueValue) ?: LocalDate.now()
        androidx.compose.runtime.DisposableEffect(context, initialDate) {
            val dialog = DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    dueValue = canonicalDate(year, month + 1, dayOfMonth)
                    openDatePicker = false
                },
                initialDate.year,
                initialDate.monthValue - 1,
                initialDate.dayOfMonth,
            )
            dialog.setOnDismissListener { openDatePicker = false }
            dialog.show()
            onDispose {
                dialog.setOnDismissListener(null)
                dialog.dismiss()
            }
        }
    }

    if (openTimePicker) {
        val initialTime = parseTaskReminderTimeValue(remindValue) ?: LocalTime.of(9, 0)
        androidx.compose.runtime.DisposableEffect(context, initialTime) {
            val dialog = TimePickerDialog(
                context,
                { _, hourOfDay, minute ->
                    remindValue = canonicalTime(hourOfDay, minute)
                    openTimePicker = false
                },
                initialTime.hour,
                initialTime.minute,
                DateFormat.is24HourFormat(context),
            )
            dialog.setOnDismissListener { openTimePicker = false }
            dialog.show()
            onDispose {
                dialog.setOnDismissListener(null)
                dialog.dismiss()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = if (lineNumber != null) "Task schedule · line $lineNumber" else "Task schedule",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = task.text.ifBlank { "Task" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            TaskScheduleField(
                label = "Due date",
                value = dueValue.takeIf(String::isNotBlank)?.let(::formatTaskDueValue) ?: "None",
                onSet = { openDatePicker = true },
                onClear = if (dueValue.isNotBlank()) {
                    { dueValue = "" }
                } else {
                    null
                },
                enabled = !isSaving,
            )
            TaskScheduleField(
                label = "Reminder",
                value = remindValue.takeIf(String::isNotBlank)?.let(::formatTaskReminderValue) ?: "None",
                onSet = { openTimePicker = true },
                onClear = if (remindValue.isNotBlank()) {
                    { remindValue = "" }
                } else {
                    null
                },
                enabled = !isSaving,
            )
            Text(
                text = if (dueValue.isBlank()) {
                    "Reminder uses time only and becomes useful once the task has a due date."
                } else {
                    "Reminder matches the due date and stores only the time, like the web client."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                TextButton(onClick = onDismiss, enabled = !isSaving) {
                    Text("Cancel")
                }
                Button(
                    onClick = { onSave(dueValue, remindValue) },
                    enabled = !isSaving,
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text("Done")
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImageActionSheet(
    image: MarkdownImageTarget,
    currentPagePath: String,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit,
) {
    val displayName = remember(image, currentPagePath) {
        suggestImageFileName(
            currentPagePath = currentPagePath,
            image = image,
            mimeType = null,
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Image",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = image.alt.ifBlank { displayName },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (image.alt.isNotBlank()) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save")
            }
            Button(
                onClick = onShare,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Share")
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun TaskScheduleField(
    label: String,
    value: String,
    onSet: () -> Unit,
    onClear: (() -> Unit)?,
    enabled: Boolean,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AssistChip(
                    onClick = onSet,
                    enabled = enabled,
                    label = { Text("Set") },
                )
                AssistChip(
                    onClick = { onClear?.invoke() },
                    enabled = enabled && onClear != null,
                    label = { Text("Clear") },
                )
            }
        }
    }
}

private val taskBracketFieldRegex = Regex("\\[(due|remind|who|click|completed):\\s*[^\\]]*\\]", RegexOption.IGNORE_CASE)
private val taskInlineFieldRegex = Regex("\\b(due|remind|who|click)::\\s*.*?(?=(\\s+\\b(due|remind|who|click)::)|$)", RegexOption.IGNORE_CASE)
private val taskRemindTagRegex = Regex("(^|\\s)#remind\\b", RegexOption.IGNORE_CASE)

private fun stripTaskInlineFields(text: String): String {
    return text
        .replace(taskBracketFieldRegex, " ")
        .replace(taskInlineFieldRegex, " ")
        .replace(taskRemindTagRegex, " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}

private fun taskFieldValue(text: String, field: String): String? {
    val normalizedField = field.trim().lowercase()
    if (normalizedField.isBlank()) return null

    val bracketMatch = Regex(
        pattern = "\\[${Regex.escape(normalizedField)}:\\s*([^\\]]*?)\\]",
        option = RegexOption.IGNORE_CASE,
    ).find(text)
    if (bracketMatch != null) {
        return bracketMatch.groupValues[1]
            .trim()
            .trim('"')
            .takeIf(String::isNotBlank)
    }

    val inlineMatch = Regex(
        pattern = "\\b${Regex.escape(normalizedField)}::\\s*(.*?)(?=(\\s+\\b(due|remind|who|click|completed)::)|$)",
        option = RegexOption.IGNORE_CASE,
    ).find(text)
    return inlineMatch?.groupValues?.getOrNull(1)?.trim()?.takeIf(String::isNotBlank)
}

private fun taskScheduleLabel(due: String?, remind: String?): String? {
    val parts = buildList {
        due?.takeIf(String::isNotBlank)?.let {
            add("Due ${formatTaskDueValue(it)}")
        }
        remind?.takeIf(String::isNotBlank)?.let {
            add("Remind ${formatTaskReminderValue(it)}")
        }
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}

private fun canonicalDate(year: Int, month: Int, day: Int): String {
    return String.format(Locale.US, "%04d-%02d-%02d", year, month, day)
}

private fun canonicalTime(hour: Int, minute: Int): String {
    return String.format(Locale.US, "%02d:%02d", hour, minute)
}

private fun markdownBodyLineOffset(markdown: String): Int {
    val normalized = markdown.replace("\r\n", "\n")
    if (!normalized.startsWith("---\n")) {
        return 0
    }
    val closing = normalized.indexOf("\n---\n", startIndex = 4)
    if (closing == -1) {
        return 0
    }
    var bodyStart = closing + 5
    while (bodyStart < normalized.length && normalized[bodyStart] == '\n') {
        bodyStart += 1
    }
    return normalized.substring(0, bodyStart).count { it == '\n' }
}

private fun parseTaskDateValue(raw: String?): LocalDate? {
    val value = raw.orEmpty().trim().takeIf(String::isNotBlank) ?: return null
    return runCatching { LocalDate.parse(value.take(10)) }.getOrNull()
}

private fun parseTaskReminderTimeValue(raw: String?): LocalTime? {
    val value = normalizeTaskReminderValue(raw)
    return value.takeIf(String::isNotBlank)?.let {
        runCatching { LocalTime.parse(it) }.getOrNull()
    }
}

private fun normalizeTaskReminderValue(raw: String?): String {
    val value = raw.orEmpty().trim()
    if (value.isBlank()) {
        return ""
    }
    val timeOnly = when {
        Regex("^\\d{2}:\\d{2}$").matches(value) -> value
        value.length >= 16 && (value[10] == ' ' || value[10] == 'T') -> value.substring(11, 16)
        value.takeLast(5).let { Regex("^\\d{2}:\\d{2}$").matches(it) } -> value.takeLast(5)
        else -> value
    }
    return timeOnly
}

private fun formatTaskDueValue(raw: String): String {
    val date = parseTaskDateValue(raw) ?: return raw
    return runCatching {
        date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    }.getOrDefault(raw)
}

private fun formatTaskReminderValue(raw: String): String {
    val value = normalizeTaskReminderValue(raw)
    val time = parseTaskReminderTimeValue(value) ?: return value
    return runCatching {
        time.format(DateTimeFormatter.ofPattern("HH:mm"))
    }.getOrDefault(value)
}

private fun firstContentLine(rawMarkdown: String): String {
    return rawMarkdown
        .lineSequence()
        .map(String::trim)
        .firstOrNull(String::isNotBlank)
        ?: "Empty note"
}

private fun formatServerDateTimeValue(raw: String): String {
    val instant = runCatching { Instant.parse(raw) }.getOrNull() ?: return raw
    return runCatching {
        instant
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT))
    }.getOrDefault(raw)
}

@Composable
private fun LineAnchor(
    lineNumber: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .width(44.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            },
        ),
        shape = RoundedCornerShape(10.dp),
    ) {
        Text(
            text = lineNumber.toString(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun InteractiveTablePreview(
    table: NoteEditorBlock.Table,
    selectedCell: TableCellSelection? = null,
    onCellClick: ((TableCellSelection) -> Unit)? = null,
    onCellLongPress: (() -> Unit)? = null,
    onSelectedCellValueChange: ((String) -> Unit)? = null,
) {
    val linkColor = MaterialTheme.colorScheme.primary
    val columnCount = remember(table) {
        maxOf(table.headers.size, table.rows.maxOfOrNull { it.size } ?: 0, 2)
    }
    val headers = remember(table, columnCount) {
        List(columnCount) { index ->
            table.headers.getOrElse(index) { "Column ${index + 1}" }.ifBlank { "Column ${index + 1}" }
        }
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            Column(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                Row(
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
                            RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                        )
                        .padding(vertical = 6.dp),
                ) {
                    headers.forEachIndexed { columnIndex, header ->
                        val cell = TableCellSelection(columnIndex = columnIndex, rowIndex = null)
                        TablePreviewCell(
                            text = parseInlineMarkdown(header, linkColor),
                            selected = selectedCell == cell,
                            modifier = Modifier.width(160.dp),
                            onClick = if (onCellClick != null) {
                                { onCellClick(cell) }
                            } else {
                                null
                            },
                            onLongPress = onCellLongPress,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            editorValue = if (selectedCell == cell && onSelectedCellValueChange != null) {
                                header
                            } else {
                                null
                            },
                            onEditorValueChange = if (selectedCell == cell) onSelectedCellValueChange else null,
                            editorKey = cell,
                        )
                    }
                }

                if (table.rows.isEmpty()) {
                    Row(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.65f))
                            .padding(vertical = 8.dp),
                    ) {
                        headers.indices.forEach { columnIndex ->
                            val cell = TableCellSelection(columnIndex = columnIndex, rowIndex = null)
                            TablePreviewCell(
                                text = parseInlineMarkdown(
                                    if (columnIndex == 0) {
                                        if (onCellClick != null) "Tap a header to edit." else "No rows yet."
                                    } else {
                                        ""
                                    },
                                    linkColor,
                                ),
                                selected = selectedCell == cell,
                                modifier = Modifier.width(160.dp),
                                onClick = if (onCellClick != null) {
                                    { onCellClick(cell) }
                                } else {
                                    null
                                },
                                onLongPress = onCellLongPress,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                editorKey = cell,
                            )
                        }
                    }
                } else {
                    table.rows.forEachIndexed { rowIndex, row ->
                        Row(
                            modifier = Modifier
                                .background(
                                    if (rowIndex % 2 == 0) {
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    },
                                )
                                .padding(vertical = 8.dp),
                        ) {
                            headers.indices.forEach { columnIndex ->
                                val cell = TableCellSelection(columnIndex = columnIndex, rowIndex = rowIndex)
                                TablePreviewCell(
                                    text = parseInlineMarkdown(row.getOrElse(columnIndex) { "" }.ifBlank { "\u2014" }, linkColor),
                                    selected = selectedCell == cell,
                                    modifier = Modifier.width(160.dp),
                                    onClick = if (onCellClick != null) {
                                        { onCellClick(cell) }
                                    } else {
                                        null
                                    },
                                    onLongPress = onCellLongPress,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    editorValue = if (selectedCell == cell && onSelectedCellValueChange != null) {
                                        row.getOrElse(columnIndex) { "" }
                                    } else {
                                        null
                                    },
                                    onEditorValueChange = if (selectedCell == cell) onSelectedCellValueChange else null,
                                    editorKey = cell,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TablePreviewCell(
    text: androidx.compose.ui.text.AnnotatedString,
    selected: Boolean,
    modifier: Modifier,
    onClick: (() -> Unit)?,
    onLongPress: (() -> Unit)?,
    style: androidx.compose.ui.text.TextStyle,
    color: Color,
    fontWeight: FontWeight? = null,
    editorValue: String? = null,
    onEditorValueChange: ((String) -> Unit)? = null,
    editorKey: Any? = null,
) {
    val backgroundColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
    } else {
        Color.Transparent
    }
    val cellModifier = modifier
        .padding(horizontal = 4.dp)
        .background(backgroundColor, RoundedCornerShape(8.dp))
        .padding(horizontal = 8.dp, vertical = 8.dp)

    if (selected && editorValue != null && onEditorValueChange != null && editorKey != null) {
        InlineTableCellEditor(
            value = editorValue,
            onValueChange = onEditorValueChange,
            modifier = cellModifier,
            style = style,
            color = color,
            fontWeight = fontWeight,
            editorKey = editorKey,
        )
    } else {
        Box(
            modifier = cellModifier.then(
                if (onClick != null || onLongPress != null) {
                    Modifier.combinedClickable(
                        onClick = { onClick?.invoke() },
                        onLongClick = onLongPress,
                    )
                } else {
                    Modifier
                },
            ),
        ) {
            MarkdownText(
                text = text,
                style = style,
                color = color,
                fontWeight = fontWeight,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun InlineTableCellEditor(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
    style: androidx.compose.ui.text.TextStyle,
    color: Color,
    fontWeight: FontWeight?,
    editorKey: Any,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var fieldValue by remember(editorKey) {
        mutableStateOf(
            TextFieldValue(
                text = value,
                selection = androidx.compose.ui.text.TextRange(value.length),
            ),
        )
    }

    LaunchedEffect(value) {
        if (value != fieldValue.text) {
            val cursor = fieldValue.selection.end.coerceIn(0, value.length)
            fieldValue = TextFieldValue(
                text = value,
                selection = androidx.compose.ui.text.TextRange(cursor),
            )
        }
    }

    LaunchedEffect(editorKey) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    Box(modifier = modifier) {
        BasicTextField(
            value = fieldValue,
            onValueChange = {
                fieldValue = it
                onValueChange(it.text)
            },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            textStyle = style.copy(
                color = color,
                fontWeight = fontWeight ?: style.fontWeight,
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
            singleLine = true,
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (fieldValue.text.isEmpty()) {
                        Text(
                            text = " ",
                            style = style,
                            color = Color.Transparent,
                            fontWeight = fontWeight,
                        )
                    }
                    innerTextField()
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun TableEditorSheet(
    table: NoteEditorBlock.Table,
    lineNumber: Int?,
    selection: TableCellSelection,
    onDismiss: () -> Unit,
    onSelectCell: (TableCellSelection) -> Unit,
    onValueChange: (String) -> Unit,
    onAddRowBelow: () -> Unit,
    onAddColumnRight: () -> Unit,
    onDeleteRow: (() -> Unit)?,
    onDeleteColumn: (() -> Unit)?,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = if (lineNumber != null) "Table · line $lineNumber" else "Table",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Tap a cell to type directly into it. Use the controls below to change the table structure.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            InteractiveTablePreview(
                table = table,
                selectedCell = selection,
                onCellClick = onSelectCell,
                onSelectedCellValueChange = onValueChange,
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AssistChip(
                    onClick = onAddRowBelow,
                    label = { Text("Add Row Below") },
                )
                AssistChip(
                    onClick = onAddColumnRight,
                    label = { Text("Add Column Right") },
                )
                AssistChip(
                    onClick = { onDeleteRow?.invoke() },
                    enabled = onDeleteRow != null,
                    label = { Text("Delete Row") },
                )
                AssistChip(
                    onClick = { onDeleteColumn?.invoke() },
                    enabled = onDeleteColumn != null,
                    label = { Text("Delete Column") },
                )
            }

            TextButton(onClick = onDismiss) {
                Text("Done")
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun BlockActionMenuSheet(
    lineNumber: Int,
    canDelete: Boolean,
    placement: BlockInsertPlacement,
    isUploadingDocument: Boolean,
    onDismiss: () -> Unit,
    onPlacementChange: (BlockInsertPlacement) -> Unit,
    onInsertQuery: () -> Unit,
    onUploadFile: () -> Unit,
    onDelete: (() -> Unit)?,
    onApplyCommand: (NoteSlashCommand) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                text = if (canDelete) {
                    "Block actions · line $lineNumber"
                } else {
                    "Add first block"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = if (canDelete) {
                    "Tap a block to edit it. Long press opens this panel for inserting above or below."
                } else {
                    "Choose the first block for this note."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            )
            if (canDelete) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 8.dp),
                ) {
                    AssistChip(
                        onClick = { onPlacementChange(BlockInsertPlacement.Above) },
                        label = { Text(if (placement == BlockInsertPlacement.Above) "Above *" else "Above") },
                    )
                    AssistChip(
                        onClick = { onPlacementChange(BlockInsertPlacement.Below) },
                        label = { Text(if (placement == BlockInsertPlacement.Below) "Below *" else "Below") },
                    )
                }
            }
            SlashMenuItem(
                icon = Icons.Default.FolderOpen,
                label = if (isUploadingDocument) "Uploading..." else "Upload File",
                enabled = !isUploadingDocument,
                onClick = onUploadFile,
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SlashMenuItem(icon = Icons.Default.Description, label = "Paragraph", onClick = { onApplyCommand(NoteSlashCommand.Paragraph) })
            SlashMenuItem(icon = Icons.Default.Description, label = "Heading 1", onClick = { onApplyCommand(NoteSlashCommand.Heading1) })
            SlashMenuItem(icon = Icons.Default.Description, label = "Heading 2", onClick = { onApplyCommand(NoteSlashCommand.Heading2) })
            SlashMenuItem(icon = Icons.Default.Description, label = "Heading 3", onClick = { onApplyCommand(NoteSlashCommand.Heading3) })
            SlashMenuItem(icon = Icons.Default.Task, label = "Task", onClick = { onApplyCommand(NoteSlashCommand.Task) })
            SlashMenuItem(icon = Icons.Default.Menu, label = "Bullet List", onClick = { onApplyCommand(NoteSlashCommand.Bullet) })
            SlashMenuItem(icon = Icons.Default.Menu, label = "Numbered List", onClick = { onApplyCommand(NoteSlashCommand.Numbered) })
            SlashMenuItem(icon = Icons.Default.Description, label = "Quote", onClick = { onApplyCommand(NoteSlashCommand.Quote) })
            SlashMenuItem(icon = Icons.Default.Tune, label = "Query", onClick = onInsertQuery)
            SlashMenuItem(icon = Icons.Default.Description, label = "Code Block", onClick = { onApplyCommand(NoteSlashCommand.Code) })
            SlashMenuItem(icon = Icons.Default.Description, label = "Table", onClick = { onApplyCommand(NoteSlashCommand.Table) })
            SlashMenuItem(icon = Icons.Default.Description, label = "Image", onClick = { onApplyCommand(NoteSlashCommand.Image) })
            if (onDelete != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                SlashMenuItem(
                    icon = Icons.Default.Close,
                    label = "Delete Block",
                    onClick = onDelete,
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

private fun replaceSelectionWithSnippet(
    value: TextFieldValue,
    replacement: String,
    cursorOffset: Int,
): TextFieldValue {
    val selectionStart = minOf(value.selection.start, value.selection.end).coerceIn(0, value.text.length)
    val selectionEnd = maxOf(value.selection.start, value.selection.end).coerceIn(0, value.text.length)
    val updatedText = buildString {
        append(value.text.substring(0, selectionStart))
        append(replacement)
        append(value.text.substring(selectionEnd))
    }
    val cursor = (selectionStart + cursorOffset).coerceIn(0, updatedText.length)
    return TextFieldValue(
        text = updatedText,
        selection = androidx.compose.ui.text.TextRange(cursor),
    )
}

// ─── Frontmatter Panel ──────────────────────────────────────────────────

private val frontmatterDateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

private fun isInternalFrontmatterMetadataKey(key: String): Boolean {
    return isInternalTemplateMetadataKey(key)
}

private fun isTagPropertyKey(key: String?): Boolean {
    return key?.trim()?.equals("tags", ignoreCase = true) == true
}

private fun isNotificationClickKey(key: String?): Boolean {
    val normalized = key.orEmpty().trim().lowercase()
    return normalized == "click" || normalized.endsWith("_click") || normalized.endsWith("-click")
}

private fun isNotificationPropertyKey(key: String?): Boolean {
    val normalized = key.orEmpty().trim().lowercase()
    if (normalized.isBlank() || isNotificationClickKey(normalized)) {
        return false
    }
    return normalized == "notification" ||
        normalized == "notify" ||
        normalized == "remind" ||
        normalized == "reminder" ||
        Regex("(^|[_-])(notify|notification|remind|reminder)([_-]|$)", RegexOption.IGNORE_CASE).containsMatchIn(normalized)
}

private fun frontmatterKindHints(frontmatter: JsonObject?): Map<String, FrontmatterKind> {
    val source = frontmatter ?: return emptyMap()
    val hints = mutableMapOf<String, FrontmatterKind>()

    fun register(metadataKeys: List<String>, kind: FrontmatterKind) {
        metadataKeys.forEach { metadataKey ->
            jsonElementStringValues(source[metadataKey]).forEach { key ->
                val normalizedKey = key.trim()
                if (normalizedKey.isNotBlank()) {
                    hints[normalizedKey] = kind
                }
            }
        }
    }

    register(listOf(propertyListKey, templateListKey), FrontmatterKind.List)
    register(listOf(propertyTagsKey, templateTagsKey), FrontmatterKind.Tags)
    register(listOf(propertyBoolKey, templateBoolKey), FrontmatterKind.Bool)
    register(listOf(propertyDateKey, templateDateKey), FrontmatterKind.Date)
    register(listOf(propertyDateTimeKey, templateDateTimeKey), FrontmatterKind.DateTime)
    register(listOf(propertyNotificationKey, templateNotificationKey), FrontmatterKind.Notification)
    return hints
}

private fun inferFrontmatterKind(
    value: kotlinx.serialization.json.JsonElement?,
    key: String? = null,
    hintedKind: FrontmatterKind? = null,
): FrontmatterKind {
    if (value is JsonArray) {
        return if (hintedKind == FrontmatterKind.Tags || isTagPropertyKey(key)) {
            FrontmatterKind.Tags
        } else {
            FrontmatterKind.List
        }
    }

    val primitive = value as? JsonPrimitive
    if (primitive?.booleanOrNull != null) {
        return FrontmatterKind.Bool
    }

    val textValue = frontmatterEditableText(value).trim()
    if (Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(textValue)) {
        return if (hintedKind == FrontmatterKind.Notification || isNotificationPropertyKey(key)) {
            FrontmatterKind.Notification
        } else {
            FrontmatterKind.Date
        }
    }
    if (Regex("^\\d{4}-\\d{2}-\\d{2}[ T]\\d{2}:\\d{2}").containsMatchIn(textValue)) {
        return if (hintedKind == FrontmatterKind.Notification || isNotificationPropertyKey(key)) {
            FrontmatterKind.Notification
        } else {
            FrontmatterKind.DateTime
        }
    }
    if (textValue.isBlank() && hintedKind != null) {
        return hintedKind
    }
    if (textValue.isBlank() && isNotificationPropertyKey(key)) {
        return FrontmatterKind.Notification
    }
    if (textValue.isBlank() && isTagPropertyKey(key)) {
        return FrontmatterKind.Tags
    }
    return FrontmatterKind.Text
}

private fun frontmatterVisibleEntries(frontmatter: JsonObject?): List<FrontmatterEntry> {
    val source = frontmatter ?: return emptyList()
    val kindHints = frontmatterKindHints(source)
    return source.entries
        .filterNot { (key, value) -> isInternalFrontmatterMetadataKey(key) || value is JsonNull }
        .sortedBy { it.key.lowercase() }
        .map { (key, value) ->
            FrontmatterEntry(
                key = key,
                value = value,
                kind = inferFrontmatterKind(value, key, kindHints[key]),
            )
        }
}

private fun jsonElementStringValues(value: kotlinx.serialization.json.JsonElement?): List<String> {
    return when (value) {
        is JsonArray -> value.mapNotNull { (it as? JsonPrimitive)?.content?.trim()?.takeIf(String::isNotBlank) }
        is JsonPrimitive -> listOf(value.content.trim()).filter(String::isNotBlank)
        else -> emptyList()
    }
}

private fun normalizeTagEntry(value: String): String {
    return value.trim().replace(Regex("^#+"), "")
}

private fun sequenceInputEntries(kind: FrontmatterKind, value: String): List<String> {
    return value
        .split(',', '\n')
        .map { entry ->
            if (kind == FrontmatterKind.Tags) {
                normalizeTagEntry(entry)
            } else {
                entry.trim()
            }
        }
        .filter(String::isNotBlank)
}

private fun frontmatterEditableText(element: kotlinx.serialization.json.JsonElement?): String {
    return when (element) {
        null, is JsonNull -> ""
        is JsonPrimitive -> element.content
        is JsonArray -> element.mapNotNull { (it as? JsonPrimitive)?.content }.joinToString(", ")
        is JsonObject -> element.toString()
    }
}

private fun parseFrontmatterDateValue(raw: String?): LocalDate? {
    val value = raw.orEmpty().trim()
    if (value.isBlank()) {
        return null
    }
    return runCatching { LocalDate.parse(value.take(10)) }.getOrNull()
}

private fun parseFrontmatterDateTimeValue(raw: String?): LocalDateTime? {
    val value = raw.orEmpty().trim().replace('T', ' ')
    if (value.isBlank()) {
        return null
    }
    return runCatching {
        LocalDateTime.parse(value.take(16), frontmatterDateTimeFormatter)
    }.getOrNull()
}

private fun serializeFrontmatterDateTimeValue(raw: String?): String {
    val value = raw.orEmpty().trim()
    if (value.isBlank()) {
        return ""
    }
    return parseFrontmatterDateTimeValue(value)
        ?.format(frontmatterDateTimeFormatter)
        ?: value.replace('T', ' ').take(16)
}

private fun formatFrontmatterDateValue(raw: String): String {
    val date = parseFrontmatterDateValue(raw) ?: return raw
    return runCatching {
        date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    }.getOrDefault(raw)
}

private fun formatFrontmatterDateTimeValue(raw: String): String {
    val dateTime = parseFrontmatterDateTimeValue(raw) ?: return raw
    return runCatching {
        dateTime.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT))
    }.getOrDefault(raw)
}

private fun frontmatterPreviewText(entry: FrontmatterEntry): String {
    return when (entry.kind) {
        FrontmatterKind.Tags -> jsonElementStringValues(entry.value)
            .map(::normalizeTagEntry)
            .joinToString(" ") { "#$it" }
        FrontmatterKind.List -> jsonElementStringValues(entry.value).joinToString(", ")
        FrontmatterKind.Bool -> if ((entry.value as? JsonPrimitive)?.booleanOrNull == true) "True" else "False"
        FrontmatterKind.Date -> formatFrontmatterDateValue(frontmatterEditableText(entry.value))
        FrontmatterKind.DateTime,
        FrontmatterKind.Notification,
        -> formatFrontmatterDateTimeValue(frontmatterEditableText(entry.value))
        FrontmatterKind.Text -> frontmatterEditableText(entry.value)
    }
}

private fun defaultFrontmatterKeyForKind(key: String, kind: FrontmatterKind): String {
    if (key.isNotBlank()) {
        return key
    }
    return when (kind) {
        FrontmatterKind.Tags -> "tags"
        FrontmatterKind.Notification -> "notification"
        else -> ""
    }
}

private fun frontmatterDraftStateForEntry(
    entry: FrontmatterEntry? = null,
    forcedKind: FrontmatterKind? = null,
): FrontmatterDraftState {
    val kind = forcedKind ?: entry?.kind ?: FrontmatterKind.Text
    val key = defaultFrontmatterKeyForKind(entry?.key.orEmpty(), kind)
    val value = entry?.value
    return when (kind) {
        FrontmatterKind.List -> FrontmatterDraftState(
            originalKey = entry?.key,
            key = key,
            kind = kind,
            items = jsonElementStringValues(value),
        )
        FrontmatterKind.Tags -> FrontmatterDraftState(
            originalKey = entry?.key,
            key = key,
            kind = kind,
            items = jsonElementStringValues(value).map(::normalizeTagEntry),
        )
        FrontmatterKind.Bool -> FrontmatterDraftState(
            originalKey = entry?.key,
            key = key,
            kind = kind,
            boolValue = (value as? JsonPrimitive)?.booleanOrNull == true,
        )
        FrontmatterKind.Date -> FrontmatterDraftState(
            originalKey = entry?.key,
            key = key,
            kind = kind,
            text = parseFrontmatterDateValue(frontmatterEditableText(value))?.toString().orEmpty(),
        )
        FrontmatterKind.DateTime,
        FrontmatterKind.Notification,
        -> FrontmatterDraftState(
            originalKey = entry?.key,
            key = key,
            kind = kind,
            text = serializeFrontmatterDateTimeValue(frontmatterEditableText(value)),
        )
        FrontmatterKind.Text -> FrontmatterDraftState(
            originalKey = entry?.key,
            key = key,
            kind = kind,
            text = frontmatterEditableText(value),
        )
    }
}

private fun coerceFrontmatterTextPrimitive(
    text: String,
    originalValue: kotlinx.serialization.json.JsonElement? = null,
): JsonPrimitive {
    val trimmed = text.trim()
    val primitive = originalValue as? JsonPrimitive
    if (primitive != null && !primitive.isString) {
        primitive.booleanOrNull?.let {
            return JsonPrimitive(trimmed.equals("true", ignoreCase = true))
        }
        trimmed.toLongOrNull()?.let { return JsonPrimitive(it) }
        trimmed.toDoubleOrNull()?.let { return JsonPrimitive(it) }
    }
    return JsonPrimitive(trimmed)
}

private fun frontmatterDraftJsonElement(
    draft: FrontmatterDraftState,
    originalValue: kotlinx.serialization.json.JsonElement? = null,
): kotlinx.serialization.json.JsonElement {
    return when (draft.kind) {
        FrontmatterKind.List -> JsonArray(draft.items.map { JsonPrimitive(it.trim()) })
        FrontmatterKind.Tags -> JsonArray(draft.items.map { JsonPrimitive(normalizeTagEntry(it)) })
        FrontmatterKind.Bool -> JsonPrimitive(draft.boolValue)
        FrontmatterKind.Date -> JsonPrimitive(draft.text.trim().take(10))
        FrontmatterKind.DateTime,
        FrontmatterKind.Notification,
        -> JsonPrimitive(serializeFrontmatterDateTimeValue(draft.text))
        FrontmatterKind.Text -> coerceFrontmatterTextPrimitive(draft.text, originalValue)
    }
}

private fun applyFrontmatterDraftKind(
    draft: FrontmatterDraftState,
    kind: FrontmatterKind,
    originalValue: kotlinx.serialization.json.JsonElement? = null,
): FrontmatterDraftState {
    val coercedValue = frontmatterDraftJsonElement(draft, originalValue)
    return frontmatterDraftStateForEntry(
        entry = FrontmatterEntry(
            key = defaultFrontmatterKeyForKind(draft.key, kind),
            value = coercedValue,
            kind = kind,
        ),
        forcedKind = kind,
    ).copy(originalKey = draft.originalKey)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FrontmatterPanel(
    entries: List<FrontmatterEntry>,
    scopePrefix: String,
    onOpenPage: (String) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            entries.forEach { entry ->
                val key = entry.key
                val value = entry.value
                when {
                    entry.kind == FrontmatterKind.Tags -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                "$key:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                jsonElementStringValues(value).map(::normalizeTagEntry).forEach { tagText ->
                                    AssistChip(
                                        onClick = { },
                                        label = {
                                            Text("#$tagText", style = MaterialTheme.typography.labelSmall)
                                        },
                                    )
                                }
                            }
                        }
                    }
                    entry.kind == FrontmatterKind.List && value is JsonArray -> {
                        Text(
                            "$key:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        value.forEach { item ->
                            val itemText = frontmatterEditableText(item)
                            if (itemText.isNotBlank()) {
                                Row(
                                    modifier = Modifier.padding(start = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Text("\u2022", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodySmall)
                                    if (looksLikePageLink(itemText)) {
                                        Text(
                                            displayPagePath(itemText, scopePrefix).ifBlank { itemText },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.clickable { onOpenPage(itemText) },
                                        )
                                    } else {
                                        Text(itemText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        val displayValue = frontmatterPreviewText(entry)
                        if (displayValue.isNotBlank()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    "$key:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                if (looksLikePageLink(displayValue)) {
                                    Text(
                                        displayPagePath(displayValue, scopePrefix).ifBlank { displayValue },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.clickable { onOpenPage(displayValue) },
                                    )
                                } else {
                                    Text(
                                        displayValue,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun FrontmatterEditorSheet(
    entries: List<FrontmatterEntry>,
    draft: FrontmatterDraftState?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onAddProperty: () -> Unit,
    onEditProperty: (FrontmatterEntry) -> Unit,
    onDeleteProperty: (String) -> Unit,
    onDraftChange: (FrontmatterDraftState) -> Unit,
    onDismissDraft: () -> Unit,
    onSaveDraft: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Frontmatter",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = if (draft == null) {
                            if (entries.isEmpty()) {
                                "Add note properties without opening raw YAML."
                            } else {
                                "Tap a property to edit it."
                            }
                        } else {
                            "Edit structured properties instead of raw YAML."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (draft == null) {
                    Button(
                        onClick = onAddProperty,
                        enabled = !isSaving,
                    ) {
                        Text("Add")
                    }
                }
            }
            if (draft == null) {
                if (entries.isEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "No frontmatter on this page.",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        ),
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            entries.forEachIndexed { index, entry ->
                                FrontmatterPropertyRow(
                                    entry = entry,
                                    isSaving = isSaving,
                                    onEdit = onEditProperty,
                                    onDelete = onDeleteProperty,
                                )
                                if (index != entries.lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(start = 14.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                val originalEntry = draft.originalKey?.let { key ->
                    entries.firstOrNull { it.key == key }
                }
                FrontmatterDraftEditor(
                    draft = draft,
                    originalValue = originalEntry?.value,
                    isSaving = isSaving,
                    onDraftChange = onDraftChange,
                    onDelete = draft.originalKey?.let { key -> { onDeleteProperty(key) } },
                    onDismiss = onDismissDraft,
                    onSave = onSaveDraft,
                )
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun FrontmatterPropertyRow(
    entry: FrontmatterEntry,
    isSaving: Boolean,
    onEdit: (FrontmatterEntry) -> Unit,
    onDelete: (String) -> Unit,
) {
    val preview = frontmatterPreviewText(entry)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isSaving) { onEdit(entry) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = entry.key,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                FrontmatterKindBadge(entry.kind.label)
            }
            Text(
                text = preview.ifBlank { "Empty" },
                style = MaterialTheme.typography.bodyMedium,
                color = if (preview.isBlank()) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(
            onClick = { onDelete(entry.key) },
            enabled = !isSaving,
        ) {
            Icon(Icons.Default.Close, contentDescription = "Delete ${entry.key}")
        }
    }
}

@Composable
private fun FrontmatterKindBadge(label: String) {
    Box(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                shape = RoundedCornerShape(999.dp),
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FrontmatterDraftEditor(
    draft: FrontmatterDraftState,
    originalValue: kotlinx.serialization.json.JsonElement?,
    isSaving: Boolean,
    onDraftChange: (FrontmatterDraftState) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    val context = LocalContext.current

    fun commitPendingItems() {
        val nextEntries = sequenceInputEntries(draft.kind, draft.pendingItem)
        if (nextEntries.isEmpty()) {
            return
        }
        onDraftChange(
            draft.copy(
                items = (draft.items + nextEntries).distinct(),
                pendingItem = "",
            ),
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = if (draft.originalKey == null) "Add property" else "Edit property",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )

        OutlinedTextField(
            value = draft.key,
            onValueChange = { onDraftChange(draft.copy(key = it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Key") },
            singleLine = true,
            enabled = !isSaving,
        )

        Text(
            text = "Type",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FrontmatterKind.entries.forEach { kind ->
                FilterChip(
                    selected = draft.kind == kind,
                    onClick = {
                        onDraftChange(applyFrontmatterDraftKind(draft, kind, originalValue))
                    },
                    enabled = !isSaving,
                    label = { Text(kind.label) },
                )
            }
        }

        when (draft.kind) {
            FrontmatterKind.Text -> {
                OutlinedTextField(
                    value = draft.text,
                    onValueChange = { onDraftChange(draft.copy(text = it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Value") },
                    enabled = !isSaving,
                )
            }
            FrontmatterKind.Bool -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = draft.boolValue,
                        onClick = { onDraftChange(draft.copy(boolValue = true)) },
                        enabled = !isSaving,
                        label = { Text("True") },
                    )
                    FilterChip(
                        selected = !draft.boolValue,
                        onClick = { onDraftChange(draft.copy(boolValue = false)) },
                        enabled = !isSaving,
                        label = { Text("False") },
                    )
                }
            }
            FrontmatterKind.List,
            FrontmatterKind.Tags,
            -> {
                if (draft.items.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        draft.items.forEachIndexed { index, item ->
                            AssistChip(
                                onClick = {
                                    onDraftChange(
                                        draft.copy(
                                            items = draft.items.filterIndexed { itemIndex, _ -> itemIndex != index },
                                        ),
                                    )
                                },
                                label = {
                                    Text(if (draft.kind == FrontmatterKind.Tags) "#$item" else item)
                                },
                                enabled = !isSaving,
                            )
                        }
                    }
                    Text(
                        text = "Tap an item to remove it.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = draft.pendingItem,
                        onValueChange = { onDraftChange(draft.copy(pendingItem = it)) },
                        modifier = Modifier.weight(1f),
                        label = { Text(if (draft.kind == FrontmatterKind.Tags) "Tag" else "Item") },
                        placeholder = { Text(if (draft.kind == FrontmatterKind.Tags) "Add tag" else "Add item") },
                        singleLine = true,
                        enabled = !isSaving,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { commitPendingItems() }),
                    )
                    Button(
                        onClick = { commitPendingItems() },
                        enabled = !isSaving && draft.pendingItem.isNotBlank(),
                    ) {
                        Text("Add")
                    }
                }
            }
            FrontmatterKind.Date -> {
                OutlinedTextField(
                    value = draft.text.takeIf(String::isNotBlank)?.let(::formatFrontmatterDateValue).orEmpty(),
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    label = { Text("Value") },
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(
                        onClick = {
                            val initialDate = parseFrontmatterDateValue(draft.text) ?: LocalDate.now()
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    onDraftChange(
                                        draft.copy(
                                            text = LocalDate.of(year, month + 1, dayOfMonth).toString(),
                                        ),
                                    )
                                },
                                initialDate.year,
                                initialDate.monthValue - 1,
                                initialDate.dayOfMonth,
                            ).show()
                        },
                        enabled = !isSaving,
                    ) {
                        Text("Pick date")
                    }
                    if (draft.text.isNotBlank()) {
                        TextButton(
                            onClick = { onDraftChange(draft.copy(text = "")) },
                            enabled = !isSaving,
                        ) {
                            Text("Clear")
                        }
                    }
                }
            }
            FrontmatterKind.DateTime,
            FrontmatterKind.Notification,
            -> {
                OutlinedTextField(
                    value = draft.text.takeIf(String::isNotBlank)?.let(::formatFrontmatterDateTimeValue).orEmpty(),
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    label = { Text("Value") },
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(
                        onClick = {
                            val initialDateTime = parseFrontmatterDateTimeValue(draft.text)
                                ?: LocalDateTime.of(LocalDate.now(), LocalTime.of(9, 0))
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val nextDate = LocalDate.of(year, month + 1, dayOfMonth)
                                    val nextDateTime = LocalDateTime.of(nextDate, initialDateTime.toLocalTime())
                                    onDraftChange(
                                        draft.copy(
                                            text = nextDateTime.format(frontmatterDateTimeFormatter),
                                        ),
                                    )
                                },
                                initialDateTime.year,
                                initialDateTime.monthValue - 1,
                                initialDateTime.dayOfMonth,
                            ).show()
                        },
                        enabled = !isSaving,
                    ) {
                        Text("Pick date")
                    }
                    Button(
                        onClick = {
                            val initialDateTime = parseFrontmatterDateTimeValue(draft.text)
                                ?: LocalDateTime.of(LocalDate.now(), LocalTime.of(9, 0))
                            TimePickerDialog(
                                context,
                                { _, hourOfDay, minute ->
                                    val nextTime = LocalTime.of(hourOfDay, minute)
                                    val nextDateTime = LocalDateTime.of(initialDateTime.toLocalDate(), nextTime)
                                    onDraftChange(
                                        draft.copy(
                                            text = nextDateTime.format(frontmatterDateTimeFormatter),
                                        ),
                                    )
                                },
                                initialDateTime.hour,
                                initialDateTime.minute,
                                DateFormat.is24HourFormat(context),
                            ).show()
                        },
                        enabled = !isSaving,
                    ) {
                        Text("Pick time")
                    }
                    if (draft.text.isNotBlank()) {
                        TextButton(
                            onClick = { onDraftChange(draft.copy(text = "")) },
                            enabled = !isSaving,
                        ) {
                            Text("Clear")
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text("Back")
            }
            if (onDelete != null) {
                TextButton(onClick = onDelete, enabled = !isSaving) {
                    Text("Delete")
                }
            }
            Spacer(Modifier.weight(1f))
            Button(onClick = onSave, enabled = !isSaving) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Save")
                }
            }
        }
    }
}

private fun kotlinx.serialization.json.JsonElement.jsonPrimitiveOrNull(): JsonPrimitive? = this as? JsonPrimitive

private fun jsonElementDisplayValue(element: kotlinx.serialization.json.JsonElement): String {
    return when (element) {
        is JsonNull -> ""
        is JsonPrimitive -> {
            if (element.isString) element.content
            else element.content
        }
        is JsonArray -> element.mapNotNull { (it as? JsonPrimitive)?.content }.joinToString(", ")
        is JsonObject -> element.toString()
    }
}

private fun looksLikePageLink(value: String): Boolean {
    return value.contains('/') && !value.startsWith("http") && !value.contains(' ')
}

private fun queryWorkbenchPreviewBlock(workbench: QueryWorkbenchResult): QueryBlock? {
    val preview = workbench.preview
    val count = workbench.count
    if (preview == null) {
        val fallbackError = count?.error.orEmpty().trim()
        return fallbackError.takeIf(String::isNotBlank)?.let { error ->
            QueryBlock(
                source = "workbench",
                line = 0,
                key = "workbench-preview",
                error = error,
                rowCount = 0,
                stale = false,
            )
        }
    }

    val columns = preview.columns
    val rows = preview.rows
    val error = preview.error.orEmpty().ifBlank {
        count?.error.orEmpty()
    }
    return QueryBlock(
        source = "workbench",
        line = 0,
        key = "workbench-preview",
        result = if (preview.valid) {
            QueryResult(
                columns = columns,
                rows = rows,
            )
        } else {
            null
        },
        error = if (preview.valid) "" else error,
        rowCount = if (count?.valid == true) count.count else preview.count,
        renderHint = queryWorkbenchRenderHint(columns, rows),
        stale = false,
    )
}

private fun queryWorkbenchRenderHint(
    columns: List<String>,
    rows: List<Map<String, kotlinx.serialization.json.JsonElement>>,
): String {
    return if (queryResultDisplayColumns(columns, rows).size <= 1) {
        "list"
    } else {
        "table"
    }
}

private fun queryWorkbenchFallbackMessage(workbench: QueryWorkbenchResult): String {
    return workbench.preview?.error
        ?.trim()
        ?.takeIf(String::isNotBlank)
        ?: workbench.count?.error
            ?.trim()
            ?.takeIf(String::isNotBlank)
        ?: "No preview available for this query."
}

// ─── Query Block Rendering ──────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QueryBlockCard(
    block: QueryBlock,
    scopePrefix: String,
    onLinkClick: (String) -> Unit,
    onEditQuery: (() -> Unit)?,
    onLongPress: (() -> Unit)?,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onLongPress != null) {
                    Modifier.combinedClickable(
                        onClick = {},
                        onLongClick = onLongPress,
                    )
                } else {
                    Modifier
                },
            ),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "Query",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "${block.rowCount} Ergebnisse",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (onEditQuery != null) {
                    TextButton(onClick = onEditQuery) {
                        Text("Edit")
                    }
                }
            }

            if (block.error.isNotBlank()) {
                Text(
                    block.error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                return@Column
            }

            val result = block.result ?: return@Column
            if (result.rows.isEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "No results",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Column
            }

            when (block.renderHint) {
                "list" -> QueryListView(
                    columns = result.columns,
                    rows = result.rows,
                    scopePrefix = scopePrefix,
                    onLinkClick = onLinkClick,
                )
                else -> QueryTableView(
                    columns = result.columns,
                    rows = result.rows,
                    scopePrefix = scopePrefix,
                    onLinkClick = onLinkClick,
                )
            }

            if (block.stale) {
                Text(
                    "Veraltet",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

@Composable
private fun QueryListView(
    columns: List<String>,
    rows: List<Map<String, kotlinx.serialization.json.JsonElement>>,
    scopePrefix: String,
    onLinkClick: (String) -> Unit,
) {
    val displayColumns = remember(columns, rows) { queryResultDisplayColumns(columns, rows) }
    val col = displayColumns.firstOrNull() ?: return
    val linkColor = MaterialTheme.colorScheme.primary
    rows.forEach { row ->
        val renderedText = queryResultDisplayCellText(col, row, columns, scopePrefix, linkColor)
        if (renderedText.text.isNotBlank()) {
            Row(
                modifier = Modifier.padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("\u2022", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodySmall)
                MarkdownText(
                    text = renderedText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    onLinkClick = onLinkClick,
                )
            }
        }
    }
}

@Composable
private fun QueryTableView(
    columns: List<String>,
    rows: List<Map<String, kotlinx.serialization.json.JsonElement>>,
    scopePrefix: String,
    onLinkClick: (String) -> Unit,
) {
    val linkColor = MaterialTheme.colorScheme.primary
    val displayColumns = remember(columns, rows) { queryResultDisplayColumns(columns, rows) }
    val lastColumnIndex = displayColumns.lastIndex
    Column(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        Row(
            modifier = Modifier
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                )
                .padding(horizontal = 8.dp, vertical = 6.dp),
        ) {
            displayColumns.forEachIndexed { index, col ->
                Text(
                    text = queryResultColumnLabel(col, columns, rows.firstOrNull()),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = if (index == lastColumnIndex) TextAlign.End else TextAlign.Start,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(160.dp),
                )
            }
        }
        rows.forEachIndexed { rowIndex, row ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (rowIndex % 2 == 0) {
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    },
                ),
                shape = RoundedCornerShape(0.dp),
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    displayColumns.forEachIndexed { index, col ->
                        MarkdownText(
                            text = queryResultDisplayCellText(col, row, columns, scopePrefix, linkColor),
                            style = MaterialTheme.typography.bodySmall.copy(
                                textAlign = if (index == lastColumnIndex) TextAlign.End else TextAlign.Start,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.width(160.dp),
                            onLinkClick = onLinkClick,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
        Text(
            "${rows.size} Ergebnisse",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp, start = 8.dp),
        )
    }
}

private fun queryCellAnnotatedText(
    column: String,
    value: kotlinx.serialization.json.JsonElement,
    scopePrefix: String,
    linkColor: Color,
): androidx.compose.ui.text.AnnotatedString {
    val rawValue = jsonElementDisplayValue(value)
    val isPageColumn = column.equals("path", ignoreCase = true) || column.endsWith("page", ignoreCase = true)
    if (isPageColumn && looksLikePageLink(rawValue)) {
        val displayValue = displayPagePath(rawValue, scopePrefix).ifBlank { rawValue }
        return parseInlineMarkdown("[[$rawValue|$displayValue]]", linkColor)
    }
    return parseInlineMarkdown(rawValue, linkColor)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PageHistorySheet(
    pagePath: String,
    revisions: List<PageRevisionRecord>,
    isLoading: Boolean,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit,
    onRestore: (String) -> Unit,
    onPurge: () -> Unit,
) {
    var selectedRevisionId by rememberSaveable(pagePath) { mutableStateOf<String?>(null) }
    val selectedRevision = remember(revisions, selectedRevisionId) {
        revisions.firstOrNull { revision -> revision.id == selectedRevisionId } ?: revisions.firstOrNull()
    }

    LaunchedEffect(pagePath, revisions) {
        if (selectedRevisionId == null || revisions.none { it.id == selectedRevisionId }) {
            selectedRevisionId = revisions.firstOrNull()?.id
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Revision history",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = pagePath,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AssistChip(
                    onClick = onRefresh,
                    enabled = !isBusy,
                    label = { Text("Refresh") },
                )
                AssistChip(
                    onClick = onPurge,
                    enabled = !isBusy && revisions.isNotEmpty(),
                    label = { Text("Purge history") },
                )
            }
            if (revisions.isEmpty() && !isLoading) {
                Text(
                    text = "No saved revisions for this note yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else if (revisions.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(revisions, key = { it.id }) { revision ->
                        val selected = selectedRevision?.id == revision.id
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !isBusy) {
                                    selectedRevisionId = revision.id
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selected) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                },
                            ),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = formatServerDateTimeValue(revision.savedAt),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                                Text(
                                    text = firstContentLine(revision.rawMarkdown),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
                selectedRevision?.let { revision ->
                    Text(
                        text = "Preview",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        ),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(12.dp),
                        ) {
                            Text(
                                text = revision.rawMarkdown.ifBlank { "Empty note" },
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                TextButton(onClick = onDismiss, enabled = !isBusy) {
                    Text("Close")
                }
                Button(
                    onClick = { selectedRevision?.id?.let(onRestore) },
                    enabled = !isBusy && selectedRevision != null,
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text("Restore")
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrashSheet(
    pages: List<TrashPageRecord>,
    scopePrefix: String,
    isLoading: Boolean,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit,
    onRestore: (String) -> Unit,
    onDelete: (String) -> Unit,
    onEmptyTrash: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Trash",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = if (pages.isEmpty()) "Deleted notes stay here until restored or removed permanently." else "${pages.size} deleted notes",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AssistChip(
                    onClick = onRefresh,
                    enabled = !isBusy,
                    label = { Text("Refresh") },
                )
                AssistChip(
                    onClick = onEmptyTrash,
                    enabled = !isBusy && pages.isNotEmpty(),
                    label = { Text("Empty trash") },
                )
            }
            if (pages.isEmpty() && !isLoading) {
                Text(
                    text = "Trash is empty.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else if (pages.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(pages, key = { it.page }) { entry ->
                        val displayPath = displayPagePath(entry.page, scopePrefix).ifBlank { entry.page }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            ),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = pageTitleFromPath(displayPath),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "$displayPath · deleted ${formatServerDateTimeValue(entry.deletedAt)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = firstContentLine(entry.rawMarkdown),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                                ) {
                                    TextButton(
                                        onClick = { onDelete(entry.page) },
                                        enabled = !isBusy,
                                    ) {
                                        Text(
                                            text = "Delete permanently",
                                            color = MaterialTheme.colorScheme.error,
                                        )
                                    }
                                    Button(
                                        onClick = { onRestore(entry.page) },
                                        enabled = !isBusy,
                                    ) {
                                        Text("Restore")
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss, enabled = !isBusy) {
                    Text("Close")
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DocumentLibrarySheet(
    documents: List<DocumentRecord>,
    scopePrefix: String,
    isLoading: Boolean,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit,
    onOpenDocument: (String) -> Unit,
    onRenameDocument: (String, String, (Boolean) -> Unit) -> Unit,
    onDeleteDocument: (String, (Boolean) -> Unit) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var actionDocumentPath by rememberSaveable { mutableStateOf<String?>(null) }
    var renameDocumentPath by rememberSaveable { mutableStateOf<String?>(null) }
    val scopedDocuments = remember(documents, scopePrefix) {
        documents.filter { document -> pathWithinScope(document.path, scopePrefix) }
    }
    val filteredDocuments = remember(scopedDocuments, query) {
        val normalizedQuery = query.trim().lowercase()
        val visibleDocuments = if (normalizedQuery.isBlank()) {
            scopedDocuments
        } else {
            scopedDocuments.filter { document ->
                document.name.lowercase().contains(normalizedQuery) ||
                    document.path.lowercase().contains(normalizedQuery) ||
                    document.contentType.lowercase().contains(normalizedQuery)
            }
        }
        if (normalizedQuery.isBlank()) {
            visibleDocuments.sortedWith(
                compareBy<DocumentRecord>(
                    { !(it.usageKnown && it.referenceCount == 0) },
                    { -documentTimestampSortValue(it) },
                    { it.path.lowercase() },
                ),
            )
        } else {
            visibleDocuments.sortedWith(
                compareByDescending<DocumentRecord> { documentSearchScore(it, normalizedQuery) }
                    .thenByDescending { documentTimestampSortValue(it) }
                    .thenBy { it.path.lowercase() },
            )
        }
    }
    val selectedDocument = remember(documents, actionDocumentPath) {
        val normalizedTargetPath = normalizePagePath(actionDocumentPath.orEmpty())
        documents.firstOrNull { document ->
            normalizePagePath(document.path).equals(normalizedTargetPath, ignoreCase = true)
        }
    }
    val renameTargetDocument = remember(documents, renameDocumentPath) {
        val normalizedTargetPath = normalizePagePath(renameDocumentPath.orEmpty())
        documents.firstOrNull { document ->
            normalizePagePath(document.path).equals(normalizedTargetPath, ignoreCase = true)
        }
    }

    selectedDocument?.let { document ->
        DocumentActionsSheet(
            document = document,
            scopePrefix = scopePrefix,
            isBusy = isBusy,
            onDismiss = {
                if (!isBusy) {
                    actionDocumentPath = null
                }
            },
            onRename = {
                renameDocumentPath = document.path
                actionDocumentPath = null
            },
            onDelete = {
                onDeleteDocument(document.path) { success ->
                    if (success) {
                        actionDocumentPath = null
                    }
                }
            },
        )
    }

    renameTargetDocument?.let { document ->
        DocumentPathSheet(
            documentPath = displayPagePath(document.path, scopePrefix).ifBlank { document.path },
            isSaving = isBusy,
            onDismiss = {
                if (!isBusy) {
                    renameDocumentPath = null
                }
            },
            onSave = { nextDocumentPath ->
                onRenameDocument(document.path, nextDocumentPath) { success ->
                    if (success) {
                        renameDocumentPath = null
                        actionDocumentPath = null
                    }
                }
            },
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Documents",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = when {
                    scopedDocuments.isEmpty() -> "No files in the current scope yet."
                    scopePrefix.isBlank() -> "${scopedDocuments.size} files in the vault"
                    else -> "${scopedDocuments.size} files in this scope"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AssistChip(
                    onClick = onRefresh,
                    enabled = !isBusy,
                    label = { Text("Refresh") },
                )
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isBusy,
                label = { Text("Filter files") },
                singleLine = true,
            )
            if (filteredDocuments.isEmpty() && !isLoading) {
                Text(
                    text = if (query.isBlank()) {
                        "No files found in this scope."
                    } else {
                        "No matching files."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else if (filteredDocuments.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(filteredDocuments, key = { it.path }) { document ->
                        val displayPath = displayPagePath(document.path, scopePrefix).ifBlank { document.path }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !isBusy) {
                                    onOpenDocument(document.path)
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            ),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Icon(
                                    Icons.Default.Description,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp),
                                )
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        text = document.name.ifBlank { pageTitleFromPath(document.path) },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = displayPath,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = documentMetaSummary(document),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                IconButton(
                                    onClick = { actionDocumentPath = document.path },
                                    enabled = !isBusy,
                                ) {
                                    Icon(
                                        Icons.Default.MoreVert,
                                        contentDescription = "File actions",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss, enabled = !isBusy) {
                    Text("Close")
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DocumentActionsSheet(
    document: DocumentRecord,
    scopePrefix: String,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "File actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = displayPagePath(document.path, scopePrefix).ifBlank { document.path },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            documentUsageSummary(document).takeIf(String::isNotBlank)?.let { usage ->
                Text(
                    text = usage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(
                onClick = onRename,
                enabled = !isBusy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Rename or move file")
            }
            TextButton(
                onClick = onDelete,
                enabled = !isBusy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isBusy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        text = "Delete file",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DocumentPathSheet(
    documentPath: String,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var documentPathValue by remember(documentPath) {
        mutableStateOf(
            TextFieldValue(
                text = documentPath,
                selection = androidx.compose.ui.text.TextRange(documentPath.length),
            ),
        )
    }

    LaunchedEffect(documentPath) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Rename or move file",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Enter a nested path to move this file within the current scope.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = documentPathValue,
                onValueChange = { documentPathValue = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                enabled = !isSaving,
                label = { Text("File path") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (!isSaving) {
                        onSave(documentPathValue.text.trim())
                    }
                }),
                singleLine = true,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                TextButton(onClick = onDismiss, enabled = !isSaving) {
                    Text("Cancel")
                }
                Button(
                    onClick = { onSave(documentPathValue.text.trim()) },
                    enabled = !isSaving,
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text("Done")
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

private fun documentTimestampSortValue(document: DocumentRecord): Long {
    return runCatching { java.time.Instant.parse(document.createdAt).toEpochMilli() }.getOrDefault(0L)
}

private fun documentSearchScore(document: DocumentRecord, normalizedQuery: String): Int {
    val name = document.name.lowercase()
    val path = document.path.lowercase()
    return when {
        name == normalizedQuery -> 4_000
        name.startsWith(normalizedQuery) -> 2_800
        name.contains(normalizedQuery) -> 1_600
        path.startsWith(normalizedQuery) -> 1_200
        path.contains(normalizedQuery) -> 800
        document.contentType.lowercase().contains(normalizedQuery) -> 400
        else -> 0
    }
}

private fun documentUsageSummary(document: DocumentRecord): String {
    if (!document.usageKnown) {
        return ""
    }
    return if (document.referenceCount > 0) {
        "Used in ${document.referenceCount} note" + if (document.referenceCount == 1) "" else "s"
    } else {
        "Unused upload"
    }
}

private fun documentSizeLabel(size: Long): String {
    if (size <= 0L) return ""
    val kib = size / 1024.0
    return if (kib >= 1024.0) {
        "${(kib / 1024.0 * 10).toInt() / 10.0} MB"
    } else {
        "${(kib * 10).toInt() / 10.0} KB"
    }
}

private fun documentMetaSummary(document: DocumentRecord): String {
    return listOf(
        document.contentType.takeIf(String::isNotBlank),
        documentSizeLabel(document.size).takeIf(String::isNotBlank),
        documentUsageSummary(document).takeIf(String::isNotBlank),
        document.createdAt.takeIf(String::isNotBlank)?.let(::formatServerDateTimeValue),
    ).filterNotNull().joinToString(" · ")
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
    )
}

@Composable
private fun BacklinksSection(
    backlinks: List<BacklinkRecord>,
    scopePrefix: String,
    onOpenPage: (String) -> Unit,
) {
    SectionHeader("Backlinks (${backlinks.size})")
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            backlinks.forEachIndexed { index, backlink ->
                val title = backlink.sourceTitle.trim().ifBlank {
                    pageTitleFromPath(backlink.sourcePage)
                }
                val supportingText = buildString {
                    val pathText = displayPagePath(backlink.sourcePage, scopePrefix).ifBlank { backlink.sourcePage }
                    if (pathText.isNotBlank()) {
                        append(pathText)
                    }
                    if (backlink.line > 0) {
                        if (isNotBlank()) {
                            append(" · ")
                        }
                        append("line ")
                        append(backlink.line)
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenPage(backlink.sourcePage) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = title.ifBlank { backlink.sourcePage },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (supportingText.isNotBlank()) {
                        Text(
                            text = supportingText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    backlink.linkText.trim().takeIf(String::isNotBlank)?.let { linkText ->
                        Text(
                            text = linkText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                if (index < backlinks.lastIndex) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TaskResultCard(
    task: TaskResultCardModel,
    scopePrefix: String,
    onOpenPage: (String) -> Unit,
    onPatchTask: (taskRef: String, text: String?, state: String?, due: String?, remind: String?, click: String?, onResult: (Boolean) -> Unit) -> Unit,
    onDeleteTask: (taskRef: String, onResult: (Boolean) -> Unit) -> Unit,
) {
    var isBusy by remember(task.ref) { mutableStateOf(false) }
    var showScheduleSheet by remember(task.ref) { mutableStateOf(false) }
    var showActionSheet by remember(task.ref) { mutableStateOf(false) }
    val autoReminderClickTarget = remember(task.page) { noteriousPageDeepLink(task.page) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                enabled = !isBusy,
                onClick = { onOpenPage(task.page) },
                onLongClick = { showActionSheet = true },
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            TaskPreviewRow(
                text = stripTaskInlineFields(task.text),
                checked = task.done,
                due = task.due,
                remind = task.remind,
                interactive = true,
                isBusy = isBusy,
                onToggle = {
                    isBusy = true
                    onPatchTask(
                        task.ref,
                        null,
                        if (task.done) "todo" else "done",
                        null,
                        null,
                        null,
                    ) {
                        isBusy = false
                    }
                },
                onEditText = null,
                onEditSchedule = { showScheduleSheet = true },
                onLongPress = null,
                onLinkClick = null,
            )
            task.supportingText?.takeIf(String::isNotBlank)?.let { supportingText ->
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 40.dp),
                )
            }
            task.snippet?.takeIf(String::isNotBlank)?.let { snippet ->
                Text(
                    text = snippet,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 40.dp),
                )
            }
        }
    }

    if (showScheduleSheet) {
        TaskScheduleSheet(
            task = task.toApiTaskItem(),
            lineNumber = task.line?.takeIf { it > 0 },
            isSaving = isBusy,
            onDismiss = {
                if (!isBusy) {
                    showScheduleSheet = false
                }
            },
            onSave = { dueValue, remindValue ->
                val clickValue = when {
                    remindValue.isNotBlank() && task.click.isNullOrBlank() -> autoReminderClickTarget
                    remindValue.isBlank() && task.click == autoReminderClickTarget -> ""
                    else -> null
                }
                isBusy = true
                onPatchTask(
                    task.ref,
                    null,
                    null,
                    dueValue,
                    remindValue,
                    clickValue,
                ) { success ->
                    isBusy = false
                    if (success) {
                        showScheduleSheet = false
                    }
                }
            },
        )
    }

    if (showActionSheet) {
        TaskResultActionsSheet(
            pagePath = displayPagePath(task.page, scopePrefix).ifBlank { task.page },
            isBusy = isBusy,
            onDismiss = {
                if (!isBusy) {
                    showActionSheet = false
                }
            },
            onOpenPage = {
                showActionSheet = false
                onOpenPage(task.page)
            },
            onDeleteTask = {
                isBusy = true
                onDeleteTask(task.ref) { success ->
                    isBusy = false
                    if (success) {
                        showActionSheet = false
                    }
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskResultActionsSheet(
    pagePath: String,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onOpenPage: () -> Unit,
    onDeleteTask: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Task actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = pagePath,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onOpenPage,
                enabled = !isBusy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Open source page")
            }
            TextButton(
                onClick = onDeleteTask,
                enabled = !isBusy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isBusy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        text = "Delete task",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

private fun TaskItem.toTaskResultCardModel(scopePrefix: String, includePageInSupportingText: Boolean = true): TaskResultCardModel {
    val pageLabel = displayPagePath(page, scopePrefix).ifBlank { page }
    val supportingText = buildString {
        if (!who.isNullOrBlank()) {
            append(who)
        }
        if (includePageInSupportingText && pageLabel.isNotBlank()) {
            if (isNotBlank()) {
                append(" \u00B7 ")
            }
            append(pageLabel)
        }
    }.ifBlank { null }
    return TaskResultCardModel(
        ref = ref,
        page = page,
        line = line,
        text = name,
        done = done,
        due = due,
        remind = remind,
        click = click,
        supportingText = supportingText,
    )
}

private fun SearchTaskResult.toTaskResultCardModel(scopePrefix: String, openTask: TaskItem?): TaskResultCardModel {
    val normalizedText = text
    val visibleText = stripTaskInlineFields(normalizedText)
    val pageLabel = displayPagePath(page, scopePrefix).ifBlank { page }
    val snippetText = snippet
        .trim()
        .takeIf { it.isNotBlank() && it != normalizedText && it != visibleText }
    return TaskResultCardModel(
        ref = ref,
        page = page,
        line = line.takeIf { it > 0 },
        text = normalizedText,
        done = done,
        due = openTask?.due ?: taskFieldValue(normalizedText, "due"),
        remind = openTask?.remind ?: taskFieldValue(normalizedText, "remind"),
        click = openTask?.click ?: taskFieldValue(normalizedText, "click"),
        supportingText = pageLabel,
        snippet = snippetText,
    )
}

private fun TaskResultCardModel.toApiTaskItem(): ApiTaskItem {
    return ApiTaskItem(
        ref = ref,
        page = page,
        line = line,
        text = text,
        done = done,
        due = due,
        remind = remind,
        click = click,
    )
}

// ─── Browse Screen (fixed file tree) ────────────────────────────────────

@Composable
private fun BrowseScreen(
    pages: List<ApiPageSummary>,
    folders: List<String>,
    scopePrefix: String,
    currentFolder: String,
    selectedTag: String,
    onCurrentFolderChange: (String) -> Unit,
    onSelectedTagChange: (String) -> Unit,
    onOpenPage: (String) -> Unit,
    onCreatePage: (String, (Boolean) -> Unit) -> Unit,
    onCreateFolder: (String, (Boolean) -> Unit) -> Unit,
    onRenamePage: (String, String, (Boolean) -> Unit) -> Unit,
    onDeletePage: (String, (Boolean) -> Unit) -> Unit,
    onRenameFolder: (String, String, (Boolean) -> Unit) -> Unit,
    onDeleteFolder: (String, (Boolean) -> Unit) -> Unit,
) {
    var pageActionTarget by rememberSaveable { mutableStateOf<String?>(null) }
    var renamePageTarget by rememberSaveable { mutableStateOf<String?>(null) }
    var createPagePathDraft by rememberSaveable { mutableStateOf<String?>(null) }
    var folderActionTarget by rememberSaveable { mutableStateOf<String?>(null) }
    var createFolderPathDraft by rememberSaveable { mutableStateOf<String?>(null) }
    var renameFolderTarget by rememberSaveable { mutableStateOf<String?>(null) }
    var isPerformingPageAction by rememberSaveable { mutableStateOf(false) }
    var isPerformingFolderAction by rememberSaveable { mutableStateOf(false) }
    val isAnyActionBusy = isPerformingPageAction || isPerformingFolderAction
    val availableTags = remember(pages) {
        pages
            .flatMap { it.tags.orEmpty() }
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinctBy { it.lowercase() }
            .sortedBy { it.lowercase() }
    }
    val filteredPages = remember(pages, selectedTag) {
        val normalizedTag = selectedTag.trim()
        if (normalizedTag.isBlank()) {
            pages
        } else {
            pages.filter { page ->
                page.tags.orEmpty().any { tag -> tag.equals(normalizedTag, ignoreCase = true) }
            }
        }
    }
    val visibleFolders = remember(folders, selectedTag, scopePrefix) {
        if (selectedTag.isBlank()) {
            folders.filter { folderPath -> pathWithinScope(folderPath, scopePrefix) }
        } else {
            emptyList()
        }
    }
    val tree = remember(filteredPages, visibleFolders, scopePrefix) {
        buildFileTree(filteredPages, visibleFolders, scopePrefix)
    }
    val entries = remember(tree, currentFolder) { entriesForFolder(tree, currentFolder) }
    val selectedActionDisplayPath = remember(pageActionTarget, scopePrefix) {
        pageActionTarget?.let { pagePath ->
            displayPagePath(pagePath, scopePrefix).ifBlank { normalizePagePath(pagePath) }
        }.orEmpty()
    }
    val renameActionDisplayPath = remember(renamePageTarget, scopePrefix) {
        renamePageTarget?.let { pagePath ->
            displayPagePath(pagePath, scopePrefix).ifBlank { normalizePagePath(pagePath) }
        }.orEmpty()
    }
    val folderActionDisplayPath = remember(folderActionTarget) {
        normalizePagePath(folderActionTarget.orEmpty())
    }
    val renameFolderDisplayPath = remember(renameFolderTarget) {
        normalizePagePath(renameFolderTarget.orEmpty())
    }

    LaunchedEffect(availableTags, selectedTag) {
        if (selectedTag.isNotBlank() && availableTags.none { it.equals(selectedTag, ignoreCase = true) }) {
            onSelectedTagChange("")
        }
    }

    LaunchedEffect(tree, currentFolder) {
        if (currentFolder.isNotEmpty() && currentFolder !in tree.folderPaths && !tree.folderChildren.containsKey(currentFolder)) {
            onCurrentFolderChange("")
        }
    }

    BackHandler(enabled = currentFolder.isNotEmpty()) {
        onCurrentFolderChange(currentFolder.substringBeforeLast('/', ""))
    }

    if (pageActionTarget != null) {
        NoteActionsSheet(
            pagePath = selectedActionDisplayPath,
            isBusy = isPerformingPageAction,
            onDismiss = {
                if (!isPerformingPageAction) {
                    pageActionTarget = null
                }
            },
            onRename = {
                renamePageTarget = pageActionTarget
                pageActionTarget = null
            },
            onDelete = {
                val targetPagePath = pageActionTarget ?: return@NoteActionsSheet
                isPerformingPageAction = true
                onDeletePage(targetPagePath) { success ->
                    isPerformingPageAction = false
                    if (success) {
                        pageActionTarget = null
                    }
                }
            },
        )
    }

    if (renamePageTarget != null) {
        RenameNoteSheet(
            pagePath = renameActionDisplayPath,
            isSaving = isPerformingPageAction,
            onDismiss = {
                if (!isPerformingPageAction) {
                    renamePageTarget = null
                }
            },
            onSave = { nextPagePath ->
                val targetPagePath = renamePageTarget ?: return@RenameNoteSheet
                isPerformingPageAction = true
                onRenamePage(targetPagePath, nextPagePath) { success ->
                    isPerformingPageAction = false
                    if (success) {
                        renamePageTarget = null
                        pageActionTarget = null
                    }
                }
            },
        )
    }

    if (createPagePathDraft != null) {
        CreateNoteSheet(
            pagePath = createPagePathDraft.orEmpty(),
            isSaving = isPerformingPageAction,
            onDismiss = {
                if (!isPerformingPageAction) {
                    createPagePathDraft = null
                }
            },
            onSave = { nextPagePath ->
                isPerformingPageAction = true
                onCreatePage(nextPagePath) { success ->
                    isPerformingPageAction = false
                    if (success) {
                        createPagePathDraft = null
                    }
                }
            },
        )
    }

    if (folderActionTarget != null) {
        FolderActionsSheet(
            folderPath = folderActionDisplayPath,
            isBusy = isPerformingFolderAction,
            onDismiss = {
                if (!isPerformingFolderAction) {
                    folderActionTarget = null
                }
            },
            onCreateSubfolder = {
                val parentFolderPath = folderActionTarget ?: return@FolderActionsSheet
                createFolderPathDraft = "$parentFolderPath/"
                folderActionTarget = null
            },
            onRename = {
                renameFolderTarget = folderActionTarget
                folderActionTarget = null
            },
            onDelete = {
                val targetFolderPath = folderActionTarget ?: return@FolderActionsSheet
                isPerformingFolderAction = true
                onDeleteFolder(targetFolderPath) { success ->
                    isPerformingFolderAction = false
                    if (success) {
                        folderActionTarget = null
                    }
                }
            },
        )
    }

    if (createFolderPathDraft != null) {
        FolderPathSheet(
            title = "New folder",
            supportingText = "Enter a nested path to create a folder within the current scope.",
            folderPath = createFolderPathDraft.orEmpty(),
            isSaving = isPerformingFolderAction,
            onDismiss = {
                if (!isPerformingFolderAction) {
                    createFolderPathDraft = null
                }
            },
            onSave = { nextFolderPath ->
                isPerformingFolderAction = true
                onCreateFolder(nextFolderPath) { success ->
                    isPerformingFolderAction = false
                    if (success) {
                        createFolderPathDraft = null
                        folderActionTarget = null
                    }
                }
            },
        )
    }

    if (renameFolderTarget != null) {
        FolderPathSheet(
            title = "Rename or move folder",
            supportingText = "Enter a nested path to move this folder within the current scope.",
            folderPath = renameFolderDisplayPath,
            isSaving = isPerformingFolderAction,
            onDismiss = {
                if (!isPerformingFolderAction) {
                    renameFolderTarget = null
                }
            },
            onSave = { nextFolderPath ->
                val targetFolderPath = renameFolderTarget ?: return@FolderPathSheet
                isPerformingFolderAction = true
                onRenameFolder(targetFolderPath, nextFolderPath) { success ->
                    isPerformingFolderAction = false
                    if (success) {
                        renameFolderTarget = null
                        folderActionTarget = null
                    }
                }
            },
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (availableTags.isNotEmpty()) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = selectedTag.isBlank(),
                    onClick = { onSelectedTagChange("") },
                    label = { Text("All tags") },
                )
                availableTags.forEach { tag ->
                    FilterChip(
                        selected = selectedTag.equals(tag, ignoreCase = true),
                        onClick = {
                            onSelectedTagChange(
                                if (selectedTag.equals(tag, ignoreCase = true)) {
                                    ""
                                } else {
                                    tag
                                },
                            )
                        },
                        label = { Text(tag) },
                    )
                }
            }
        }

        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = if (availableTags.isEmpty()) 8.dp else 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AssistChip(
                onClick = {
                    createPagePathDraft = if (currentFolder.isBlank()) "" else "$currentFolder/"
                },
                enabled = !isAnyActionBusy,
                label = {
                    Text(if (currentFolder.isBlank()) "New note" else "New note here")
                },
                leadingIcon = {
                    Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(18.dp))
                },
            )
            AssistChip(
                onClick = {
                    createFolderPathDraft = if (currentFolder.isBlank()) "" else "$currentFolder/"
                },
                enabled = !isAnyActionBusy,
                label = {
                    Text(if (currentFolder.isBlank()) "New folder" else "New subfolder")
                },
                leadingIcon = {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                },
            )
            if (currentFolder.isNotBlank()) {
                AssistChip(
                    onClick = { folderActionTarget = currentFolder },
                    enabled = !isAnyActionBusy,
                    label = { Text("Folder actions") },
                    leadingIcon = {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                )
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 0.dp,
                end = 16.dp,
                bottom = screenContentBottomPadding,
            ),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (currentFolder.isNotEmpty()) {
                item(key = "..") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onCurrentFolderChange(currentFolder.substringBeforeLast('/', ""))
                            }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            "..",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            items(entries, key = { "${it.isFolder}:${it.nodePath}:${it.openPath}" }) { entry ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (entry.isFolder) {
                                onCurrentFolderChange(entry.nodePath)
                            } else {
                                entry.openPath?.let(onOpenPage)
                            }
                        }
                        .padding(vertical = 10.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        if (entry.isFolder) Icons.Default.FolderOpen else Icons.Default.Description,
                        contentDescription = null,
                        tint = if (entry.isFolder) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = entry.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (entry.isFolder) {
                            Text(
                                text = "${entry.childCount} items",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (entry.isFolder) {
                        IconButton(
                            onClick = { folderActionTarget = entry.nodePath },
                            enabled = !isAnyActionBusy,
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Folder actions",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else if (entry.openPath != null) {
                        IconButton(
                            onClick = { pageActionTarget = entry.openPath },
                            enabled = !isAnyActionBusy,
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Note actions",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            if (entries.isEmpty()) {
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        val emptyMessage = when {
                            currentFolder.isNotEmpty() -> "This folder is empty."
                            selectedTag.isNotBlank() -> "No pages for this tag."
                            pages.isEmpty() && folders.isEmpty() -> "No pages or folders loaded."
                            else -> "No pages loaded."
                        }
                        Text(
                            text = emptyMessage,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

private data class FileEntry(
    val name: String,
    val nodePath: String,
    val openPath: String? = null,
    val isFolder: Boolean,
    val childCount: Int = 0,
)

private data class FileTree(
    val folderChildren: Map<String, Set<String>> = emptyMap(),
    val allPages: Set<String> = emptySet(),
    val folderPaths: Set<String> = emptySet(),
    val pageLookup: Map<String, String> = emptyMap(),
)

private fun buildFileTree(pages: List<ApiPageSummary>, folders: List<String>, scopePrefix: String): FileTree {
    val folderChildren = mutableMapOf<String, MutableSet<String>>()
    val allPages = mutableSetOf<String>()
    val folderPaths = mutableSetOf<String>()
    val pageLookup = mutableMapOf<String, String>()
    folderChildren.getOrPut("") { mutableSetOf() }

    fun registerFolder(displayFolderPath: String) {
        val normalizedFolderPath = normalizePagePath(displayFolderPath)
        if (normalizedFolderPath.isBlank()) return
        val segments = normalizedFolderPath.split('/')
        for (i in segments.indices) {
            val folderPath = segments.take(i + 1).joinToString("/")
            folderPaths.add(folderPath)
            val parentPath = if (i == 0) "" else segments.take(i).joinToString("/")
            folderChildren.getOrPut(parentPath) { mutableSetOf() }.add(folderPath)
            folderChildren.getOrPut(folderPath) { mutableSetOf() }
        }
    }

    folders.forEach { folderPath ->
        registerFolder(displayPagePath(folderPath, scopePrefix))
    }

    for (page in pages) {
        val displayPath = displayPagePath(page.path, scopePrefix)
        if (displayPath.isBlank()) continue

        allPages.add(displayPath)
        pageLookup[displayPath] = page.path
        val parentFolder = displayPath.substringBeforeLast('/', "")
        registerFolder(parentFolder)
        folderChildren.getOrPut(parentFolder) { mutableSetOf() }.add(displayPath)
    }

    return FileTree(
        folderChildren = folderChildren,
        allPages = allPages,
        folderPaths = folderPaths,
        pageLookup = pageLookup,
    )
}

private fun entriesForFolder(tree: FileTree, folder: String): List<FileEntry> {
    val children = tree.folderChildren[folder] ?: return emptyList()
    val entries = mutableListOf<FileEntry>()

    for (childPath in children) {
        val isPage = childPath in tree.allPages
        val isFolder = childPath in tree.folderPaths

        if (isFolder) {
            val childCount = tree.folderChildren[childPath]?.size ?: 0
            entries.add(FileEntry(
                name = childPath.substringAfterLast('/'),
                nodePath = childPath,
                isFolder = true,
                childCount = childCount,
            ))
        }

        if (isPage) {
            entries.add(FileEntry(
                name = childPath.substringAfterLast('/'),
                nodePath = childPath,
                openPath = tree.pageLookup[childPath],
                isFolder = false,
            ))
        }
    }

    return entries.sortedWith(compareBy({ !it.isFolder }, { it.name.lowercase() }))
}

// ─── Tasks Screen ───────────────────────────────────────────────────────

@Composable
private fun TasksScreen(
    tasks: List<TaskItem>,
    scopePrefix: String,
    filterText: String,
    taskFilter: TaskListFilter,
    onFilterTextChange: (String) -> Unit,
    onTaskFilterChange: (TaskListFilter) -> Unit,
    onOpenPage: (String) -> Unit,
    onPatchTask: (taskRef: String, text: String?, state: String?, due: String?, remind: String?, click: String?, onResult: (Boolean) -> Unit) -> Unit,
    onDeleteTask: (taskRef: String, onResult: (Boolean) -> Unit) -> Unit,
) {
    val today = LocalDate.now()
    val filteredTasks = remember(tasks, filterText, taskFilter, today) {
        val lower = filterText.trim().lowercase()
        tasks.filter { task ->
            val matchesText = lower.isBlank() ||
                task.name.lowercase().contains(lower) ||
                task.page.lowercase().contains(lower) ||
                task.who?.lowercase()?.contains(lower) == true
            val dueDate = parseTaskDateValue(task.due)
            val matchesQuickFilter = when (taskFilter) {
                TaskListFilter.Open -> !task.done
                TaskListFilter.Done -> task.done
                TaskListFilter.All -> true
                TaskListFilter.WithDue -> !task.due.isNullOrBlank()
                TaskListFilter.WithReminder -> !task.remind.isNullOrBlank()
                TaskListFilter.Today -> dueDate == today
                TaskListFilter.Overdue -> dueDate?.isBefore(today) == true
            }
            matchesText && matchesQuickFilter
        }
    }

    val groupedTasks = remember(filteredTasks) {
        filteredTasks.groupBy { it.page }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = filterText,
            onValueChange = onFilterTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Filter tasks...") },
            singleLine = true,
            trailingIcon = {
                if (filterText.isNotEmpty()) {
                    IconButton(onClick = { onFilterTextChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear")
                    }
                }
            },
        )

        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TaskListFilter.entries.forEach { candidate ->
                FilterChip(
                    selected = taskFilter == candidate,
                    onClick = { onTaskFilterChange(candidate) },
                    label = { Text(candidate.label) },
                )
            }
        }

        Text(
            "${filteredTasks.size} Tasks",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 4.dp,
                end = 16.dp,
                bottom = screenContentBottomPadding,
            ),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            groupedTasks.forEach { (page, pageTasks) ->
                item(key = "header:$page") {
                    Text(
                        text = displayPagePath(page, scopePrefix).ifBlank { page },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                    )
                }
                items(pageTasks, key = { it.ref }) { task ->
                    TaskResultCard(
                        task = task.toTaskResultCardModel(
                            scopePrefix = scopePrefix,
                            includePageInSupportingText = false,
                        ),
                        scopePrefix = scopePrefix,
                        onOpenPage = onOpenPage,
                        onPatchTask = onPatchTask,
                        onDeleteTask = onDeleteTask,
                    )
                }
            }

            if (groupedTasks.isEmpty()) {
                item("empty") {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "No tasks for this filter.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

// ─── Search Screen ──────────────────────────────────────────────────────

@Composable
private fun SearchScreen(
    uiState: MainUiState,
    scopePrefix: String,
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    onClear: () -> Unit,
    onOpenPage: (String) -> Unit,
    onOpenQuery: (String) -> Unit,
    onPatchTask: (taskRef: String, text: String?, state: String?, due: String?, remind: String?, click: String?, onResult: (Boolean) -> Unit) -> Unit,
    onDeleteTask: (taskRef: String, onResult: (Boolean) -> Unit) -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val openTaskByRef = remember(uiState.tasks) { uiState.tasks.associateBy { it.ref } }
    val searchScopeDescription = remember(scopePrefix) {
        if (scopePrefix.isBlank()) {
            "all scopes"
        } else {
            "the current scope"
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchText,
            onValueChange = onSearchTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Search...") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
            trailingIcon = {
                if (searchText.isNotBlank() || uiState.searchResults != null) {
                    IconButton(
                        onClick = {
                            keyboardController?.hide()
                            onClear()
                        },
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Clear")
                    }
                }
            },
        )

        if (uiState.isSearching) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        val results = uiState.searchResults
        if (results != null) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 8.dp,
                end = 16.dp,
                bottom = screenContentBottomPadding,
            ),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (results.pages.isNotEmpty()) {
                    item { SectionHeader("Pages (${results.pages.size})") }
                    items(results.pages, key = { "p:${it.path}:${it.line}" }) { page ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenPage(page.path) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = page.title.ifBlank { page.path.substringAfterLast('/') },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = displayPagePath(page.path, scopePrefix).ifBlank { page.path },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                if (page.snippet.isNotBlank()) {
                                    Text(
                                        text = page.snippet,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(top = 4.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                if (results.tasks.isNotEmpty()) {
                    item { SectionHeader("Tasks (${results.tasks.size})") }
                    items(results.tasks, key = { "t:${it.ref}" }) { task ->
                        TaskResultCard(
                            task = task.toTaskResultCardModel(scopePrefix, openTaskByRef[task.ref]),
                            scopePrefix = scopePrefix,
                            onOpenPage = onOpenPage,
                            onPatchTask = onPatchTask,
                            onDeleteTask = onDeleteTask,
                        )
                    }
                }

                if (results.queries.isNotEmpty()) {
                    item { SectionHeader("Queries (${results.queries.size})") }
                    items(results.queries, key = { "q:${it.name}" }) { query ->
                        val folderLabel = displayPagePath(query.folder, scopePrefix).ifBlank {
                            query.folder.ifBlank { "Unfiled" }
                        }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenQuery(query.name) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = query.title.ifBlank { query.name },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "$folderLabel · ${query.name}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                query.match.takeIf(String::isNotBlank)?.let { match ->
                                    Text(
                                        text = match,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = 4.dp),
                                    )
                                }
                                if (query.snippet.isNotBlank()) {
                                    Text(
                                        text = query.snippet,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(top = 4.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                if (results.pages.isEmpty() && results.tasks.isEmpty() && results.queries.isEmpty()) {
                    item {
                        SearchNoResultsCard(
                            query = searchText,
                            scopeDescription = searchScopeDescription,
                            onClear = onClear,
                        )
                    }
                }
            }
        } else if (searchText.isBlank()) {
            SearchEmptyState(
                scopeDescription = searchScopeDescription,
                onSuggestionSelected = onSearchTextChange,
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (uiState.isSearching) "Searching..." else "Keep typing to search.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SearchEmptyState(
    scopeDescription: String,
    onSuggestionSelected: (String) -> Unit,
) {
    val suggestions = remember {
        listOf("meeting", "invoice", "roadmap", "follow up")
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "Search pages, tasks, and queries",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(
                    text = "Results stay inside $scopeDescription and update while you type.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    suggestions.forEach { suggestion ->
                        AssistChip(
                            onClick = { onSuggestionSelected(suggestion) },
                            label = { Text(suggestion) },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                        )
                    }
                }
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Good search targets",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Page titles and text, task content, saved query names, and matches inside query descriptions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SearchNoResultsCard(
    query: String,
    scopeDescription: String,
    onClear: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "No matches for \"$query\"",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Nothing matched in $scopeDescription. Try a broader term or switch scope.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(
                onClick = onClear,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text("Clear search")
            }
        }
    }
}

private fun suggestImageFileName(
    currentPagePath: String,
    image: MarkdownImageTarget,
    mimeType: String?,
): String {
    val target = image.target.trim()
    val rawName = when {
        Regex("^https?://", RegexOption.IGNORE_CASE).containsMatchIn(target) -> {
            Uri.parse(target).lastPathSegment
        }
        target.startsWith("/") -> {
            target.substringAfterLast('/')
        }
        else -> {
            resolveRelativePath(currentPagePath, target).substringAfterLast('/')
        }
    }
        ?.substringBefore('?')
        ?.substringBefore('#')
        .orEmpty()

    val sanitizedName = sanitizeFileName(
        rawName.ifBlank {
            image.alt.trim().takeIf(String::isNotBlank) ?: "image"
        },
    )
    if (sanitizedName.substringAfterLast('.', "").isNotBlank()) {
        return sanitizedName
    }

    val extension = normalizedFileExtension(mimeType)
    return if (extension != null) {
        "$sanitizedName.$extension"
    } else {
        sanitizedName
    }
}

private fun normalizeMimeType(contentType: String?, fileName: String): String {
    val explicitMimeType = contentType
        ?.substringBefore(';')
        ?.trim()
        ?.takeIf(String::isNotBlank)
    if (explicitMimeType != null) {
        return explicitMimeType
    }

    val extension = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
    return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        ?.takeIf(String::isNotBlank)
        ?: "application/octet-stream"
}

private fun normalizedFileExtension(contentType: String?): String? {
    val mimeType = contentType
        ?.substringBefore(';')
        ?.trim()
        ?.lowercase(Locale.ROOT)
        ?.takeIf(String::isNotBlank)
        ?: return null
    return MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
        ?.takeIf(String::isNotBlank)
}

private fun sanitizeFileName(value: String): String {
    val decoded = Uri.decode(value)
    return decoded
        .trim()
        .replace(Regex("[\\\\/:*?\"<>|]+"), "_")
        .ifBlank { "image" }
}

private fun writeSharedImageFile(
    context: android.content.Context,
    payload: ImageDownloadPayload,
): Uri {
    val shareDirectory = File(context.cacheDir, "shared-images").apply {
        mkdirs()
    }
    val targetFile = File(
        shareDirectory,
        "${System.currentTimeMillis()}-${sanitizeFileName(payload.fileName)}",
    )
    targetFile.writeBytes(payload.bytes)
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        targetFile,
    )
}

// ─── Settings Screen ────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    settings: dev.carnager.noterious.data.AppSettings,
    vaults: List<VaultRecord>,
    userSettings: UserSettingsPayload,
    serverSettings: ServerSettingsResponse?,
    serverMeta: ServerMetaResponse?,
    themes: List<ThemeRecord>,
    isDetailsLoading: Boolean,
    isThemesLoading: Boolean,
    isThemeBusy: Boolean,
    isVaultBusy: Boolean,
    isUserSettingsSaving: Boolean,
    onSave: (String, String, String, String, String, String) -> Unit,
    onRefreshDetails: () -> Unit,
    onRefreshThemes: () -> Unit,
    onSaveThemeSelection: (String) -> Unit,
    onUploadTheme: () -> Unit,
    onDeleteTheme: (String, (Boolean) -> Unit) -> Unit,
    onRefreshVaults: () -> Unit,
    onCreateVault: (String, (VaultRecord?) -> Unit) -> Unit,
    onRenameVault: (VaultRecord, String, (VaultRecord?) -> Unit) -> Unit,
    onSelectVault: (VaultRecord) -> Unit,
    onExportBackupManifest: () -> Unit,
    onExportBackupScript: () -> Unit,
    onSaveUserSettings: (String, String, (Boolean) -> Unit) -> Unit,
    onChangePassword: (String, String, (Boolean) -> Unit) -> Unit,
    onLogout: ((Boolean) -> Unit) -> Unit,
) {
    var serverUrl by rememberSaveable(settings.serverUrl) { mutableStateOf(settings.serverUrl) }
    var scopePrefix by rememberSaveable(settings.scopePrefix) { mutableStateOf(settings.scopePrefix) }
    var username by rememberSaveable(settings.username) { mutableStateOf(settings.username) }
    var password by rememberSaveable(settings.password) { mutableStateOf(settings.password) }
    var bearerToken by rememberSaveable(settings.bearerToken) { mutableStateOf(settings.bearerToken) }
    var startupTab by rememberSaveable(settings.startupTab) {
        mutableStateOf(startupTabForValue(settings.startupTab).wireValue)
    }
    var ntfyTopicUrl by rememberSaveable(userSettings.notifications.ntfyTopicUrl) {
        mutableStateOf(userSettings.notifications.ntfyTopicUrl)
    }
    var ntfyToken by rememberSaveable(userSettings.notifications.ntfyToken) {
        mutableStateOf(userSettings.notifications.ntfyToken)
    }
    var showChangePasswordSheet by rememberSaveable { mutableStateOf(false) }
    var currentPassword by rememberSaveable { mutableStateOf("") }
    var newPassword by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var authActionError by rememberSaveable { mutableStateOf<String?>(null) }
    var isAuthActionBusy by rememberSaveable { mutableStateOf(false) }
    var showConnectionEditorSheet by rememberSaveable { mutableStateOf(false) }
    var showThemeLibrarySheet by rememberSaveable { mutableStateOf(false) }
    var showVaultEditorSheet by rememberSaveable { mutableStateOf(false) }
    var editingVaultId by rememberSaveable { mutableStateOf<Long?>(null) }
    var vaultNameDraft by rememberSaveable { mutableStateOf("") }
    var vaultActionError by rememberSaveable { mutableStateOf<String?>(null) }
    var isVaultActionBusy by rememberSaveable { mutableStateOf(false) }

    val selectedThemeId = settings.themeId.trim().ifBlank { "system" }
    val currentSelectedTheme = themes.firstOrNull { theme ->
        theme.id.equals(selectedThemeId, ignoreCase = true)
    }
    val editingVault = vaults.firstOrNull { vault -> vault.id == editingVaultId }
    val currentScopeLabel = remember(settings.scopePrefix, vaults) {
        displayCurrentScopeLabel(settings.scopePrefix, vaults)
    }

    if (showConnectionEditorSheet) {
        ModalBottomSheet(
            onDismissRequest = { showConnectionEditorSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Edit connection",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                AppTextField(value = serverUrl, onValueChange = { serverUrl = it }, label = "Server URL")
                AppTextField(value = scopePrefix, onValueChange = { scopePrefix = it }, label = "Scope Prefix")
                AppTextField(value = username, onValueChange = { username = it }, label = "Username")
                AppTextField(value = password, onValueChange = { password = it }, label = "Password", isPassword = true)
                AppTextField(value = bearerToken, onValueChange = { bearerToken = it }, label = "Bearer Token", isPassword = true)
                Text(
                    text = "Open on startup",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StartupTab.entries.forEach { option ->
                        FilterChip(
                            selected = startupTab == option.wireValue,
                            onClick = { startupTab = option.wireValue },
                            label = { Text(option.label) },
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TextButton(
                        onClick = { showConnectionEditorSheet = false },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            onSave(
                                serverUrl,
                                scopePrefix,
                                username,
                                password,
                                bearerToken,
                                startupTabForValue(startupTab).wireValue,
                            )
                            showConnectionEditorSheet = false
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Save")
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    if (showChangePasswordSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                if (!isAuthActionBusy) {
                    showChangePasswordSheet = false
                    authActionError = null
                }
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Change password",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                AppTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    label = "Current password",
                    isPassword = true,
                )
                AppTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = "New password",
                    isPassword = true,
                )
                AppTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = "Confirm new password",
                    isPassword = true,
                )
                authActionError?.takeIf(String::isNotBlank)?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TextButton(
                        onClick = {
                            showChangePasswordSheet = false
                            authActionError = null
                        },
                        enabled = !isAuthActionBusy,
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            authActionError = when {
                                currentPassword.isBlank() -> "Current password is required."
                                newPassword.isBlank() -> "New password is required."
                                newPassword != confirmPassword -> "New passwords do not match."
                                else -> null
                            }
                            if (authActionError != null) {
                                return@Button
                            }
                            isAuthActionBusy = true
                            onChangePassword(currentPassword, newPassword) { success ->
                                isAuthActionBusy = false
                                if (success) {
                                    currentPassword = ""
                                    newPassword = ""
                                    confirmPassword = ""
                                    authActionError = null
                                    showChangePasswordSheet = false
                                }
                            }
                        },
                        enabled = !isAuthActionBusy,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Update")
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    if (showThemeLibrarySheet) {
        ThemeLibrarySheet(
            selectedThemeId = selectedThemeId,
            themes = themes,
            isLoading = isThemesLoading,
            isBusy = isThemeBusy,
            onDismiss = { showThemeLibrarySheet = false },
            onRefresh = onRefreshThemes,
            onSelectTheme = onSaveThemeSelection,
            onUploadTheme = onUploadTheme,
            onDeleteTheme = onDeleteTheme,
        )
    }

    if (showVaultEditorSheet) {
        VaultEditorSheet(
            title = if (editingVault != null) "Rename vault" else "New vault",
            value = vaultNameDraft,
            error = vaultActionError,
            isBusy = isVaultBusy || isVaultActionBusy,
            onValueChange = {
                vaultNameDraft = it
                vaultActionError = null
            },
            onDismiss = {
                if (!isVaultBusy && !isVaultActionBusy) {
                    showVaultEditorSheet = false
                    editingVaultId = null
                    vaultActionError = null
                }
            },
            onConfirm = {
                val normalizedVaultName = vaultNameDraft.trim()
                vaultActionError = if (normalizedVaultName.isBlank()) {
                    "Vault name is required."
                } else {
                    null
                }
                if (vaultActionError != null) {
                    return@VaultEditorSheet
                }

                isVaultActionBusy = true
                val currentEditingVault = editingVault
                if (currentEditingVault != null) {
                    onRenameVault(currentEditingVault, normalizedVaultName) { updatedVault ->
                        isVaultActionBusy = false
                        if (updatedVault != null) {
                            vaultNameDraft = ""
                            editingVaultId = null
                            vaultActionError = null
                            showVaultEditorSheet = false
                        }
                    }
                } else {
                    onCreateVault(normalizedVaultName) { createdVault ->
                        isVaultActionBusy = false
                        if (createdVault != null) {
                            vaultNameDraft = ""
                            editingVaultId = null
                            vaultActionError = null
                            showVaultEditorSheet = false
                        }
                    }
                }
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SettingsSectionCard(title = "Connection") {
            SettingsInfoRow("Server", settings.serverUrl.ifBlank { "Not configured" })
            SettingsInfoRow("Scope", currentScopeLabel)
            SettingsInfoRow("Username", settings.username.ifBlank { "Not configured" })
            SettingsInfoRow(
                "Auth",
                when {
                    settings.bearerToken.isNotBlank() -> "Bearer token saved"
                    settings.password.isNotBlank() -> "Password saved"
                    else -> "No credentials stored"
                },
            )
            SettingsInfoRow("Open on startup", startupTabForValue(settings.startupTab).label)
            Button(
                onClick = { showConnectionEditorSheet = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (settings.serverUrl.isBlank()) "Set up connection" else "Edit connection")
            }
        }

        SettingsSectionCard(
            title = "Appearance",
            action = {
                TextButton(
                    onClick = {
                        onRefreshThemes()
                        showThemeLibrarySheet = true
                    },
                    enabled = !isThemesLoading && !isThemeBusy,
                ) {
                    Text("Library")
                }
            },
        ) {
            SettingsInfoRow(
                "Theme",
                currentSelectedTheme?.name ?: if (selectedThemeId.equals("system", ignoreCase = true)) {
                    "System default"
                } else {
                    "Unavailable ($selectedThemeId)"
                },
            )
            Text(
                text = "Theme selection is local to this device. Custom themes are shared through the server library.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (isThemesLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            Button(
                onClick = {
                    onRefreshThemes()
                    showThemeLibrarySheet = true
                },
                enabled = !isThemeBusy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Choose theme")
            }
        }

        SettingsSectionCard(title = "User notifications") {
            AppTextField(
                value = ntfyTopicUrl,
                onValueChange = { ntfyTopicUrl = it },
                label = "ntfy topic URL",
            )
            AppTextField(
                value = ntfyToken,
                onValueChange = { ntfyToken = it },
                label = "ntfy token",
                isPassword = true,
            )
            Button(
                onClick = {
                    onSaveUserSettings(ntfyTopicUrl, ntfyToken) { success ->
                        if (success) {
                            ntfyTopicUrl = ntfyTopicUrl.trim()
                            ntfyToken = ntfyToken.trim()
                        }
                    }
                },
                enabled = !isUserSettingsSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (isUserSettingsSaving) "Saving..." else "Save notifications")
            }
        }

        SettingsSectionCard(
            title = "Vaults",
            action = {
                TextButton(onClick = onRefreshVaults, enabled = !isVaultBusy && !isVaultActionBusy) {
                    Text("Refresh")
                }
            },
        ) {
            SettingsInfoRow("Current scope", displayCurrentScopeLabel(settings.scopePrefix, vaults))
            Button(
                onClick = {
                    editingVaultId = null
                    vaultNameDraft = ""
                    vaultActionError = null
                    showVaultEditorSheet = true
                },
                enabled = !isVaultBusy && !isVaultActionBusy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("New vault")
            }
            if (vaults.isEmpty()) {
                Text(
                    text = "No top-level vaults found yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                vaults
                    .sortedBy { vault -> displayScopeName(vault).lowercase(Locale.ROOT) }
                    .forEach { vault ->
                        val isCurrentVault = normalizeScopePrefix(settings.scopePrefix) ==
                            normalizeScopePrefix(scopePrefixForVault(vault))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                            ),
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = displayScopeName(vault),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = vault.vaultPath.ifBlank { "Vault path unavailable" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    if (isCurrentVault) {
                                        AssistChip(
                                            onClick = {},
                                            enabled = false,
                                            label = { Text("Current") },
                                        )
                                    } else {
                                        TextButton(
                                            onClick = { onSelectVault(vault) },
                                            enabled = !isVaultBusy && !isVaultActionBusy,
                                        ) {
                                            Text("Use")
                                        }
                                    }
                                    TextButton(
                                        onClick = {
                                            editingVaultId = vault.id
                                            vaultNameDraft = displayScopeName(vault)
                                            vaultActionError = null
                                            showVaultEditorSheet = true
                                        },
                                        enabled = !isVaultBusy && !isVaultActionBusy,
                                    ) {
                                        Text("Rename")
                                    }
                                }
                            }
                        }
                    }
            }
        }

        SettingsSectionCard(
            title = "Backup",
            action = {
                TextButton(onClick = onRefreshDetails, enabled = !isDetailsLoading) {
                    Text("Refresh")
                }
            },
        ) {
            if (isDetailsLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            serverMeta?.let { meta ->
                SettingsInfoRow("Vault root", meta.runtimeVault.vaultPath)
                SettingsInfoRow("Data dir", meta.dataDir)
                SettingsInfoRow("Index DB", meta.database)
                meta.currentVault?.vaultPath?.takeIf(String::isNotBlank)?.let { currentVaultPath ->
                    SettingsInfoRow("Current scope vault", currentVaultPath)
                }
            }
            Text(
                text = "The manifest and shell script describe the current deployment. They are helper files, not the backup itself.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onExportBackupManifest,
                enabled = serverMeta != null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save backup manifest")
            }
            Button(
                onClick = onExportBackupScript,
                enabled = serverMeta != null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save backup script")
            }
        }

        SettingsSectionCard(
            title = "Server runtime",
            action = {
                TextButton(onClick = onRefreshDetails, enabled = !isDetailsLoading) {
                    Text("Refresh")
                }
            },
        ) {
            if (isDetailsLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            serverSettings?.let { snapshot ->
                SettingsInfoRow("Vault root", snapshot.settings.vault.vaultPath)
                SettingsInfoRow("Applied vault", snapshot.appliedVault.vaultPath)
                SettingsInfoRow("Notification interval", snapshot.settings.notifications.ntfyInterval)
                SettingsInfoRow("Upload placement", displayUploadPlacement(snapshot.settings.documents.uploadPlacement))
                snapshot.settings.documents.uploadSubfolder.takeIf(String::isNotBlank)?.let { subfolder ->
                    SettingsInfoRow("Upload subfolder", subfolder)
                }
                SettingsInfoRow("Restart required", if (snapshot.restartRequired) "Yes" else "No")
                if (snapshot.restartRequiredReasons.isNotEmpty()) {
                    SettingsInfoRow("Restart reasons", snapshot.restartRequiredReasons.joinToString("\n"))
                }
            }
            serverMeta?.let { meta ->
                if (serverSettings != null) {
                    HorizontalDivider()
                }
                SettingsInfoRow("Listen address", meta.listenAddr)
                SettingsInfoRow("Server time", meta.serverTime)
                SettingsInfoRow("Database", meta.database)
                SettingsInfoRow("Index", meta.indexStatus.summary)
                SettingsInfoRow(
                    "Vault health",
                    if (meta.vaultHealth.healthy) {
                        "Healthy"
                    } else {
                        listOf("Unhealthy", meta.vaultHealth.reason, meta.vaultHealth.message)
                            .filter(String::isNotBlank)
                            .joinToString(" · ")
                    },
                )
                meta.currentVault?.vaultPath?.takeIf(String::isNotBlank)?.let { currentVaultPath ->
                    SettingsInfoRow("Current scope vault", currentVaultPath)
                }
                SettingsInfoRow("Runtime vault", meta.runtimeVault.vaultPath)
                SettingsInfoRow("Watcher", if (meta.watcherEnabled) meta.watchInterval else "Disabled")
                SettingsInfoRow(
                    "Notifications runtime",
                    if (meta.notificationEnabled) meta.notificationInterval else "Disabled",
                )
            }
            if (!isDetailsLoading && serverSettings == null && serverMeta == null) {
                Text(
                    text = "No server details loaded yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        SettingsSectionCard(title = "Session") {
            Button(
                onClick = {
                    authActionError = null
                    showChangePasswordSheet = true
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isAuthActionBusy,
            ) {
                Text("Change password")
            }

            TextButton(
                onClick = {
                    isAuthActionBusy = true
                    onLogout { _ ->
                        isAuthActionBusy = false
                        showChangePasswordSheet = false
                        authActionError = null
                        currentPassword = ""
                        newPassword = ""
                        confirmPassword = ""
                    }
                },
                enabled = !isAuthActionBusy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Log out")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeLibrarySheet(
    selectedThemeId: String,
    themes: List<ThemeRecord>,
    isLoading: Boolean,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit,
    onSelectTheme: (String) -> Unit,
    onUploadTheme: () -> Unit,
    onDeleteTheme: (String, (Boolean) -> Unit) -> Unit,
) {
    val selectedCustomTheme = themes.firstOrNull { theme ->
        theme.id.equals(selectedThemeId, ignoreCase = true) && theme.source.equals("custom", ignoreCase = true)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Theme library",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Pick a local app theme or manage the shared server theme library.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextButton(onClick = onRefresh, enabled = !isLoading && !isBusy) {
                    Text("Refresh")
                }
                Button(
                    onClick = onUploadTheme,
                    enabled = !isBusy,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Upload theme JSON")
                }
            }

            ThemeLibraryItem(
                title = "System default",
                subtitle = "Use the mobile default theme and follow the system light/dark preference.",
                selected = selectedThemeId.equals("system", ignoreCase = true),
                onClick = { onSelectTheme("system") },
            )
            themes
                .sortedBy { theme -> theme.name.lowercase(Locale.ROOT) }
                .forEach { theme ->
                    ThemeLibraryItem(
                        title = theme.name.ifBlank { theme.id },
                        subtitle = listOf(
                            themeBadge(theme),
                            theme.description.takeIf(String::isNotBlank),
                        ).joinToString(" · "),
                        selected = theme.id.equals(selectedThemeId, ignoreCase = true),
                        onClick = { onSelectTheme(theme.id) },
                    )
                }

            selectedCustomTheme?.let { theme ->
                TextButton(
                    onClick = {
                        onDeleteTheme(theme.id) { _ -> }
                    },
                    enabled = !isBusy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (isBusy) "Deleting..." else "Delete selected custom theme")
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ThemeLibraryItem(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VaultEditorSheet(
    title: String,
    value: String,
    error: String?,
    isBusy: Boolean,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            AppTextField(
                value = value,
                onValueChange = onValueChange,
                label = "Vault name",
            )
            error?.takeIf(String::isNotBlank)?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextButton(onClick = onDismiss, enabled = !isBusy) {
                    Text("Cancel")
                }
                Button(
                    onClick = onConfirm,
                    enabled = !isBusy,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (isBusy) "Saving..." else "Save")
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

private fun themeBadge(theme: ThemeRecord): String {
    val source = theme.source.ifBlank { "theme" }.replaceFirstChar { char ->
        if (char.isLowerCase()) char.titlecase(Locale.ROOT) else char.toString()
    }
    val kind = theme.kind.ifBlank { "" }.replaceFirstChar { char ->
        if (char.isLowerCase()) char.titlecase(Locale.ROOT) else char.toString()
    }
    return listOf(source, kind).filter(String::isNotBlank).joinToString(" · ")
}

@Composable
private fun SettingsSectionCard(
    title: String,
    action: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                action?.invoke()
            }
            content()
        }
    }
}

@Composable
private fun SettingsInfoRow(label: String, value: String) {
    val normalizedValue = value.trim().ifBlank { "Not set" }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = normalizedValue,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun displayUploadPlacement(value: String): String {
    return when (value.trim()) {
        "vault-root" -> "Vault root"
        "note-subfolder" -> "Note subfolder"
        "same-folder" -> "Same folder"
        else -> value.ifBlank { "Unknown" }
    }
}

@Composable
private fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isPassword: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
    )
}
