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
data class ApiPagesResponse(
    val pages: List<ApiPageSummary> = emptyList(),
)

@Serializable
data class ApiPageSummary(
    val path: String = "",
    val title: String = "",
    val tags: List<String>? = null,
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
    val followup: Boolean = false,
    @SerialName("remindCandidate")
    val remindCandidate: Boolean = false,
)

@Serializable
data class SearchResponse(
    val query: String = "",
    val pages: List<SearchPageResult> = emptyList(),
    val tasks: List<SearchTaskResult> = emptyList(),
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
data class SearchCounts(
    val pages: Int = 0,
    val tasks: Int = 0,
    val total: Int = 0,
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
)

@Serializable
data class DerivedPageResponse(
    val page: String = "",
    val title: String = "",
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
