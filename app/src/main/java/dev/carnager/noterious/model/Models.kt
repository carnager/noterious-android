package dev.carnager.noterious.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable
data class ApiAuthSession(
    val authenticated: Boolean = false,
    val setupRequired: Boolean = false,
)

@Serializable
data class ServerVaultSettings(
    val vaultPath: String = "",
)

@Serializable
data class UserNotificationSettings(
    val ntfyTopicUrl: String = "",
    val ntfyToken: String = "",
)

@Serializable
data class UserSettingsPayload(
    val notifications: UserNotificationSettings = UserNotificationSettings(),
)

@Serializable
data class UserSettingsResponse(
    val settings: UserSettingsPayload = UserSettingsPayload(),
)

@Serializable
data class ServerNotificationSettings(
    val ntfyInterval: String = "",
)

@Serializable
data class ServerDocumentSettings(
    val uploadPlacement: String = "",
    val uploadSubfolder: String = "",
)

@Serializable
data class ServerSettingsPayload(
    val vault: ServerVaultSettings = ServerVaultSettings(),
    val notifications: ServerNotificationSettings = ServerNotificationSettings(),
    val documents: ServerDocumentSettings = ServerDocumentSettings(),
)

@Serializable
data class ServerSettingsResponse(
    val settings: ServerSettingsPayload = ServerSettingsPayload(),
    val appliedVault: ServerVaultSettings = ServerVaultSettings(),
    val restartRequired: Boolean = false,
    val restartRequiredReasons: List<String> = emptyList(),
)

@Serializable
data class ScopedVaultRecord(
    val id: Long = 0,
    val key: String = "",
    val name: String = "",
    val vaultPath: String = "",
)

@Serializable
data class VaultHealthRecord(
    val healthy: Boolean = false,
    val reason: String = "",
    val message: String = "",
)

@Serializable
data class ServerIndexStatus(
    val dbPresent: Boolean = false,
    val indexedPageCount: Int = 0,
    val indexedTaskCount: Int = 0,
    val latestIndexedAt: String = "",
    val latestVaultModAt: String = "",
    val fresh: Boolean = false,
    val summary: String = "",
)

@Serializable
data class WatcherRuntimeStateRecord(
    val lastPollAt: String = "",
    val lastSuccessAt: String = "",
    val lastError: String = "",
    val lastChangedCount: Int = 0,
    val lastDeletedCount: Int = 0,
    val knownPageCount: Int = 0,
)

@Serializable
data class ServerMetaResponse(
    val name: String = "",
    val listenAddr: String = "",
    val runtimeVault: ServerVaultSettings = ServerVaultSettings(),
    val currentVault: ScopedVaultRecord? = null,
    val vaultHealth: VaultHealthRecord = VaultHealthRecord(),
    val dataDir: String = "",
    val database: String = "",
    val indexStatus: ServerIndexStatus = ServerIndexStatus(),
    val serverTime: String = "",
    val serverFirst: Boolean = false,
    val watchInterval: String = "",
    val watcherEnabled: Boolean = false,
    val watcherState: WatcherRuntimeStateRecord? = null,
    val notificationInterval: String = "",
    val notificationEnabled: Boolean = false,
    val restartRequired: Boolean = false,
    val restartRequiredReasons: List<String> = emptyList(),
)

@Serializable
data class ThemeTokens(
    val bg: String = "",
    val bgGradientStart: String = "",
    val bgGradientEnd: String = "",
    val bgGlowA: String = "",
    val bgGlowB: String = "",
    val sidebar: String = "",
    val sidebarSoft: String = "",
    val panel: String = "",
    val panelStrong: String = "",
    val surface: String = "",
    val surfaceSoft: String = "",
    val overlay: String = "",
    val overlaySoft: String = "",
    val table: String = "",
    val tableHeader: String = "",
    val editorOverlay: String = "",
    val ink: String = "",
    val muted: String = "",
    val accent: String = "",
    val accentSoft: String = "",
    val warn: String = "",
    val line: String = "",
    val lineStrong: String = "",
    val focusRing: String = "",
    val selection: String = "",
    val shadow: String = "",
    val themeColor: String = "",
)

@Serializable
data class ThemeRecord(
    val version: Int = 1,
    val id: String = "",
    val name: String = "",
    val source: String = "",
    val kind: String = "",
    val description: String = "",
    val tokens: ThemeTokens = ThemeTokens(),
)

@Serializable
data class ThemeListResponse(
    val themes: List<ThemeRecord> = emptyList(),
    val count: Int = 0,
)

@Serializable
data class ApiPagesResponse(
    val pages: List<ApiPageSummary> = emptyList(),
)

@Serializable
data class ApiPageSummary(
    val path: String = "",
    val title: String = "",
    val tags: List<String>? = null,
    val frontmatter: JsonObject = JsonObject(emptyMap()),
    val createdAt: String = "",
    val updatedAt: String = "",
)

@Serializable
data class ApiPageDetail(
    val page: String = "",
    val title: String = "",
    val createdAt: String = "",
    val updatedAt: String = "",
    val frontmatter: JsonObject = JsonObject(emptyMap()),
    val rawMarkdown: String = "",
    val tasks: List<ApiTaskItem> = emptyList(),
)

@Serializable
data class ApiTasksResponse(
    val tasks: List<ApiTaskItem> = emptyList(),
)

@Serializable
data class ApiTaskItem(
    val ref: String = "",
    val page: String = "",
    val line: Int? = null,
    val text: String = "",
    @Serializable(with = FlexibleStringSerializer::class)
    val state: String? = null,
    val done: Boolean = false,
    @Serializable(with = FlexibleStringSerializer::class)
    val due: String? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    val remind: String? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    val click: String? = null,
    val who: JsonElement? = null,
)

@Serializable
data class TaskItem(
    val ref: String = "",
    val page: String = "",
    val name: String = "",
    @Serializable(with = FlexibleStringSerializer::class)
    val state: String? = null,
    val done: Boolean = false,
    val pos: Int? = null,
    val line: Int? = null,
    val tags: List<String> = emptyList(),
    @SerialName("inheritedTags")
    val inheritedTags: List<String> = emptyList(),
    @Serializable(with = FlexibleStringSerializer::class)
    val who: String? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    val due: String? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    val remind: String? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    val click: String? = null,
    val followup: Boolean = false,
    @SerialName("remindCandidate")
    val remindCandidate: Boolean = false,
)

@Serializable
data class SearchResponse(
    val query: String = "",
    val pages: List<SearchPageResult> = emptyList(),
    val tasks: List<SearchTaskResult> = emptyList(),
    val queries: List<SearchSavedQueryResult> = emptyList(),
    val counts: SearchCounts = SearchCounts(),
)

@Serializable
data class SearchPageResult(
    val path: String = "",
    val title: String = "",
    val match: String = "",
    val line: Int = 1,
    val snippet: String = "",
)

@Serializable
data class SearchTaskResult(
    val ref: String = "",
    val page: String = "",
    val line: Int = 0,
    val text: String = "",
    val done: Boolean = false,
    val snippet: String = "",
)

@Serializable
data class SearchSavedQueryResult(
    val name: String = "",
    val title: String = "",
    val folder: String = "",
    val match: String = "",
    val snippet: String = "",
)

@Serializable
data class SavedQueryRecord(
    val name: String = "",
    val title: String = "",
    val description: String = "",
    val folder: String = "",
    val tags: List<String> = emptyList(),
    val query: String = "",
    val createdAt: String = "",
    val updatedAt: String = "",
)

@Serializable
data class SearchCounts(
    val pages: Int = 0,
    val tasks: Int = 0,
    val queries: Int = 0,
    val total: Int = 0,
)

@Serializable
data class QueryPreviewResult(
    val valid: Boolean = false,
    val error: String? = null,
    val columns: List<String> = emptyList(),
    val rows: List<Map<String, JsonElement>> = emptyList(),
    val count: Int = 0,
    val limit: Int? = null,
    val truncated: Boolean = false,
)

@Serializable
data class QueryCountResult(
    val valid: Boolean = false,
    val error: String? = null,
    val count: Int = 0,
)

@Serializable
data class QueryWorkbenchResult(
    val preview: QueryPreviewResult? = null,
    val count: QueryCountResult? = null,
)

@Serializable
data class QueryCopilotResponse(
    val query: String = "",
    val formattedQuery: String = "",
    val explanation: String = "",
    val assumptions: List<String> = emptyList(),
    val attempts: Int = 0,
    val repaired: Boolean = false,
    val valid: Boolean = false,
    val error: String = "",
)

@Serializable
data class PageRevisionRecord(
    val id: String = "",
    val page: String = "",
    val savedAt: String = "",
    val rawMarkdown: String = "",
)

@Serializable
data class PageHistoryResponse(
    val page: String = "",
    val revisions: List<PageRevisionRecord> = emptyList(),
    val count: Int = 0,
)

@Serializable
data class TrashPageRecord(
    val page: String = "",
    val deletedAt: String = "",
    val rawMarkdown: String = "",
)

@Serializable
data class TrashListResponse(
    val pages: List<TrashPageRecord> = emptyList(),
    val count: Int = 0,
)

@Serializable
data class FolderListResponse(
    val folders: List<String> = emptyList(),
    val count: Int = 0,
)

@Serializable
data class TodaySnapshot(
    val date: String = "",
    val overdue: List<TaskItem> = emptyList(),
    val dueToday: List<TaskItem> = emptyList(),
    val remindersToday: List<TaskItem> = emptyList(),
)

@Serializable
data class VaultRecord(
    val id: Long = 0,
    val key: String = "",
    val name: String = "",
    val vaultPath: String = "",
)

@Serializable
data class VaultsResponse(
    val vaults: List<VaultRecord> = emptyList(),
    val count: Int = 0,
)

@Serializable
data class DocumentRecord(
    val id: String = "",
    val path: String = "",
    val name: String = "",
    val contentType: String = "",
    val size: Long = 0,
    val createdAt: String = "",
    val downloadURL: String = "",
    val usageKnown: Boolean = false,
    val referenceCount: Int = 0,
    val referencedBy: List<String> = emptyList(),
)

@Serializable
data class DocumentListResponse(
    val documents: List<DocumentRecord> = emptyList(),
    val count: Int = 0,
    val query: String = "",
)

@Serializable
data class BacklinkRecord(
    val sourcePage: String = "",
    val sourceTitle: String = "",
    val linkText: String = "",
    val kind: String = "",
    val line: Int = 0,
)

@Serializable
data class DerivedPageResponse(
    val page: String = "",
    val title: String = "",
    val backlinks: List<BacklinkRecord> = emptyList(),
    val queryBlocks: List<QueryBlock> = emptyList(),
)

@Serializable
data class QueryBlock(
    val source: String = "",
    val line: Int = 0,
    val id: String = "",
    val key: String = "",
    val datasets: List<String> = emptyList(),
    val result: QueryResult? = null,
    val error: String = "",
    val rowCount: Int = 0,
    val renderHint: String = "",
    val stale: Boolean = false,
)

@Serializable
data class QueryResult(
    val columns: List<String> = emptyList(),
    val rows: List<Map<String, JsonElement>> = emptyList(),
)
