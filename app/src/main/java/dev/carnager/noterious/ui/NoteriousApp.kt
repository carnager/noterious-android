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
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Task
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import dev.carnager.noterious.model.DerivedPageResponse
import dev.carnager.noterious.model.DocumentRecord
import dev.carnager.noterious.model.QueryBlock
import dev.carnager.noterious.model.TaskItem
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
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.io.File
import java.util.Locale

private enum class Tab { Home, Browse, Tasks, Search, Settings }

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

private data class ImageDownloadPayload(
    val bytes: ByteArray,
    val fileName: String,
    val mimeType: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteriousApp(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableStateOf(Tab.Home) }
    val snackbarHostState = remember { SnackbarHostState() }
    var showSlashMenu by remember { mutableStateOf(false) }
    var showCommandPalette by remember { mutableStateOf(false) }
    var showScopePicker by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.error) {
        if (uiState.openPagePath == null) {
            uiState.error?.let { msg ->
                snackbarHostState.showSnackbar(msg)
                viewModel.clearError()
            }
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
                showCommandPalette = true
            },
            onOpenDocument = {
                navigateToTab(Tab.Browse)
            },
            onSearch = {
                navigateToTab(Tab.Search)
            },
            onSettings = {
                navigateToTab(Tab.Settings)
            },
        )
    }

    // Command palette overlay
    if (showCommandPalette) {
        CommandPaletteSheet(
            pages = uiState.pages,
            scopePrefix = uiState.settings.scopePrefix,
            onOpenPage = {
                showCommandPalette = false
                viewModel.openPage(it)
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
            settings = uiState.settings,
            error = uiState.error,
            isLoading = uiState.isPageLoading,
            isSaving = uiState.isPageSaving,
            onBack = { viewModel.closePage() },
            onOpenPage = { viewModel.openPage(it) },
            onClearError = { viewModel.clearError() },
            onSavePage = { markdown, baseMarkdown, onResult ->
                viewModel.saveOpenPage(markdown, baseMarkdown, onResult)
            },
            onShowGlobalMenu = { showSlashMenu = true },
            onPatchTask = { taskRef, text, state, due, remind, onResult ->
                viewModel.patchOpenPageTask(
                    taskRef = taskRef,
                    text = text,
                    state = state,
                    due = due,
                    remind = remind,
                    onResult = onResult,
                )
            },
            onUploadDocument = { uri, onResult ->
                viewModel.uploadDocumentForOpenPage(uri, onResult)
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
                                text = uiState.settings.scopePrefix.ifBlank { "All scopes" },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = "Scope waehlen",
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
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Sync, contentDescription = "Aktualisieren")
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == Tab.Home,
                    onClick = { selectedTab = Tab.Home },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Home") },
                )
                NavigationBarItem(
                    selected = selectedTab == Tab.Browse,
                    onClick = { selectedTab = Tab.Browse },
                    icon = { Icon(Icons.Default.Folder, contentDescription = null) },
                    label = { Text("Browse") },
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
                    label = { Text("Suche") },
                )
                NavigationBarItem(
                    selected = selectedTab == Tab.Settings,
                    onClick = { selectedTab = Tab.Settings },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Settings") },
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showSlashMenu = true },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(Icons.Default.Menu, contentDescription = "Menue", tint = MaterialTheme.colorScheme.onPrimary)
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
                    Tab.Home -> HomeScreen(uiState = uiState, onOpenPage = { viewModel.openPage(it) })
                    Tab.Browse -> BrowseScreen(
                        pages = uiState.pages,
                        scopePrefix = uiState.settings.scopePrefix,
                        onOpenPage = { viewModel.openPage(it) },
                    )
                    Tab.Tasks -> TasksScreen(
                        tasks = uiState.tasks,
                        scopePrefix = uiState.settings.scopePrefix,
                        onOpenPage = { viewModel.openPage(it) },
                    )
                    Tab.Search -> SearchScreen(
                        uiState = uiState,
                        scopePrefix = uiState.settings.scopePrefix,
                        onSearch = { viewModel.search(it) },
                        onClear = { viewModel.clearSearch() },
                        onOpenPage = { viewModel.openPage(it) },
                    )
                    Tab.Settings -> SettingsScreen(
                        settings = uiState.settings,
                        onSave = { url, scope, user, pass, token ->
                            viewModel.saveSettings(url, scope, user, pass, token)
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
                )
            },
            onSelectVault = { vault ->
                showScopePicker = false
                viewModel.selectVault(vault)
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
) {
    val normalizedCurrentScope = remember(currentScopePrefix) {
        normalizeScopePrefix(currentScopePrefix)
    }
    val selectableVaults = remember(vaults) {
        vaults
            .sortedBy { vault -> vault.name.lowercase() }
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
                        title = vault.name.ifBlank { vault.key.ifBlank { vault.vaultPath } },
                        subtitle = vault.key.takeIf { it.isNotBlank() && it != vault.name } ?: vault.vaultPath,
                        selected = normalizedCurrentScope == scopePrefixForVault(vault),
                        onClick = { onSelectVault(vault) },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
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

// ─── Command Palette ────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommandPaletteSheet(
    pages: List<ApiPageSummary>,
    scopePrefix: String,
    onOpenPage: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(pages, query) {
        if (query.isBlank()) {
            pages.sortedByDescending { it.updatedAt }.take(30)
        } else {
            val lower = query.lowercase()
            pages.filter { it.path.lowercase().contains(lower) || it.title.lowercase().contains(lower) }
                .take(50)
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
                placeholder = { Text("Seite oeffnen...") },
                singleLine = true,
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Leeren")
                        }
                    }
                },
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier.height(400.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
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
    settings: dev.carnager.noterious.data.AppSettings,
    error: String?,
    isLoading: Boolean,
    isSaving: Boolean,
    onBack: () -> Unit,
    onOpenPage: (String) -> Unit,
    onClearError: () -> Unit,
    onSavePage: (String, String, (Boolean) -> Unit) -> Unit,
    onShowGlobalMenu: () -> Unit,
    onPatchTask: (taskRef: String, text: String?, state: String?, due: String?, remind: String?, onResult: (Boolean) -> Unit) -> Unit,
    onUploadDocument: (Uri, (DocumentRecord?) -> Unit) -> Unit,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val snackbarHostState = remember { SnackbarHostState() }
    val noteContentBottomPadding = 104.dp
    val displayPath = remember(pagePath, settings.scopePrefix) {
        displayPagePath(pagePath, settings.scopePrefix).ifBlank { pagePath }
    }
    val displayTitle = frontmatter?.get("title")
        ?.jsonPrimitiveOrNull()
        ?.content
        ?.takeIf(String::isNotBlank)
        ?: pageTitleFromPath(displayPath)

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
        pendingBlockInsertAnchor = null
        tableEditorState = null
        textBlockEditorState = null
        taskTextEditorState = null
        taskScheduleEditorState = null
        editorMode = NoteEditorMode.Preview
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
        pendingBlockInsertAnchor = null
        blockActionMenuState = BlockActionMenuState(
            blockIndex = blockIndex,
            lineNumber = lineNumber,
        )
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
        onSuccess: (() -> Unit)? = null,
    ) {
        if (pendingTaskRef != null) return
        pendingTaskRef = taskRef
        onPatchTask(taskRef, text, state, due, remind) { success ->
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
                Icon(Icons.Default.Menu, contentDescription = "Menue", tint = MaterialTheme.colorScheme.onPrimary)
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurueck")
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

                    if (!isDirty && frontmatter != null && frontmatter.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        FrontmatterPanel(
                            frontmatter = frontmatter,
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
                patchTask(
                    taskRef = openTaskSchedule.ref,
                    due = dueValue,
                    remind = remindValue,
                )
            },
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FrontmatterPanel(
    frontmatter: JsonObject,
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
            frontmatter.entries.forEach { (key, value) ->
                when {
                    key == "tags" && value is JsonArray -> {
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
                                value.forEach { tag ->
                                    val tagText = tag.jsonPrimitiveOrNull()?.content ?: return@forEach
                                    AssistChip(
                                        onClick = { },
                                        label = {
                                            Text(tagText, style = MaterialTheme.typography.labelSmall)
                                        },
                                    )
                                }
                            }
                        }
                    }
                    value is JsonArray -> {
                        Text(
                            "$key:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        value.forEach { item ->
                            val itemText = jsonElementDisplayValue(item)
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
                        val displayValue = jsonElementDisplayValue(value)
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
                        Text("Bearbeiten")
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
                    "Keine Ergebnisse",
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

// ─── Home Screen ────────────────────────────────────────────────────────

@Composable
private fun HomeScreen(uiState: MainUiState, onOpenPage: (String) -> Unit) {
    val today = uiState.today

    if (uiState.settings.serverUrl.isBlank()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "Konfiguriere die Server-URL in den Settings.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (today.overdue.isNotEmpty()) {
            item { SectionHeader("Ueberfaellig (${today.overdue.size})") }
            items(today.overdue, key = { it.ref }) { task ->
                TaskRow(task = task, onOpenPage = onOpenPage)
            }
        }

        if (today.dueToday.isNotEmpty()) {
            item { SectionHeader("Heute faellig (${today.dueToday.size})") }
            items(today.dueToday, key = { it.ref }) { task ->
                TaskRow(task = task, onOpenPage = onOpenPage)
            }
        }

        if (today.remindersToday.isNotEmpty()) {
            item { SectionHeader("Erinnerungen") }
            items(today.remindersToday, key = { it.ref }) { task ->
                TaskRow(task = task, onOpenPage = onOpenPage)
            }
        }

        if (today.overdue.isEmpty() && today.dueToday.isEmpty() && today.remindersToday.isEmpty()) {
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Keine Aufgaben fuer heute.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
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
private fun TaskRow(task: TaskItem, onOpenPage: (String) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenPage(task.page) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                if (task.done) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (task.done) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                val meta = buildList {
                    task.due?.let { add("Faellig: $it") }
                    task.who?.let { add(it) }
                    add(task.page.substringAfterLast('/'))
                }.joinToString(" \u00B7 ")
                Text(
                    text = meta,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ─── Browse Screen (fixed file tree) ────────────────────────────────────

@Composable
private fun BrowseScreen(
    pages: List<ApiPageSummary>,
    scopePrefix: String,
    onOpenPage: (String) -> Unit,
) {
    var currentFolder by rememberSaveable { mutableStateOf("") }

    val tree = remember(pages, scopePrefix) { buildFileTree(pages, scopePrefix) }
    val entries = remember(tree, currentFolder) { entriesForFolder(tree, currentFolder) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (currentFolder.isNotEmpty()) {
            item(key = "..") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            currentFolder = currentFolder.substringBeforeLast('/', "")
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
                            currentFolder = entry.nodePath
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
                            text = "${entry.childCount} Eintraege",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        if (entries.isEmpty() && currentFolder.isEmpty() && pages.isEmpty()) {
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Keine Seiten geladen.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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

private fun buildFileTree(pages: List<ApiPageSummary>, scopePrefix: String): FileTree {
    val folderChildren = mutableMapOf<String, MutableSet<String>>()
    val allPages = mutableSetOf<String>()
    val folderPaths = mutableSetOf<String>()
    val pageLookup = mutableMapOf<String, String>()

    for (page in pages) {
        val displayPath = displayPagePath(page.path, scopePrefix)
        if (displayPath.isBlank()) continue

        allPages.add(displayPath)
        pageLookup[displayPath] = page.path
        val segments = displayPath.split('/')

        // Register all intermediate folder paths
        for (i in 0 until segments.size - 1) {
            val folderPath = segments.take(i + 1).joinToString("/")
            folderPaths.add(folderPath)
            val parentPath = if (i == 0) "" else segments.take(i).joinToString("/")
            folderChildren.getOrPut(parentPath) { mutableSetOf() }.add(folderPath)
        }

        // Register the page itself under its parent folder
        val parentFolder = if (segments.size > 1) segments.dropLast(1).joinToString("/") else ""
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
    onOpenPage: (String) -> Unit,
) {
    var filterText by rememberSaveable { mutableStateOf("") }

    val filteredTasks = remember(tasks, filterText) {
        if (filterText.isBlank()) {
            tasks
        } else {
            val lower = filterText.lowercase()
            tasks.filter { task ->
                task.name.lowercase().contains(lower) ||
                    task.page.lowercase().contains(lower) ||
                    task.who?.lowercase()?.contains(lower) == true
            }
        }
    }

    val groupedTasks = remember(filteredTasks) {
        filteredTasks.groupBy { it.page }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = filterText,
            onValueChange = { filterText = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Tasks filtern...") },
            singleLine = true,
            trailingIcon = {
                if (filterText.isNotEmpty()) {
                    IconButton(onClick = { filterText = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Leeren")
                    }
                }
            },
        )

        Text(
            "${filteredTasks.size} offene Tasks",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
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
                    TaskRow(task = task, onOpenPage = onOpenPage)
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
    onSearch: (String) -> Unit,
    onClear: () -> Unit,
    onOpenPage: (String) -> Unit,
) {
    var searchText by rememberSaveable { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchText,
            onValueChange = {
                searchText = it
                onSearch(it)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Suche...") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
            trailingIcon = {
                if (searchText.isNotEmpty()) {
                    IconButton(onClick = {
                        searchText = ""
                        onClear()
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Leeren")
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
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (results.pages.isNotEmpty()) {
                    item { SectionHeader("Seiten (${results.pages.size})") }
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
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenPage(task.page) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Icon(
                                    if (task.done) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (task.done) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(20.dp),
                                )
                                Column {
                                    Text(
                                        text = task.text,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text = displayPagePath(task.page, scopePrefix).ifBlank { task.page },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }

                if (results.pages.isEmpty() && results.tasks.isEmpty()) {
                    item {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "Keine Ergebnisse.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
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

@Composable
private fun SettingsScreen(
    settings: dev.carnager.noterious.data.AppSettings,
    onSave: (String, String, String, String, String) -> Unit,
) {
    var serverUrl by rememberSaveable(settings.serverUrl) { mutableStateOf(settings.serverUrl) }
    var scopePrefix by rememberSaveable(settings.scopePrefix) { mutableStateOf(settings.scopePrefix) }
    var username by rememberSaveable(settings.username) { mutableStateOf(settings.username) }
    var password by rememberSaveable(settings.password) { mutableStateOf(settings.password) }
    var bearerToken by rememberSaveable(settings.bearerToken) { mutableStateOf(settings.bearerToken) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AppTextField(value = serverUrl, onValueChange = { serverUrl = it }, label = "Server URL")
        AppTextField(value = scopePrefix, onValueChange = { scopePrefix = it }, label = "Scope Prefix")
        AppTextField(value = username, onValueChange = { username = it }, label = "Benutzername")
        AppTextField(value = password, onValueChange = { password = it }, label = "Passwort", isPassword = true)
        AppTextField(value = bearerToken, onValueChange = { bearerToken = it }, label = "Bearer Token", isPassword = true)

        Spacer(Modifier.height(4.dp))

        Button(
            onClick = { onSave(serverUrl, scopePrefix, username, password, bearerToken) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Speichern")
        }
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
