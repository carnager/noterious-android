package dev.carnager.noterious.data

import dev.carnager.noterious.model.ApiAuthSession
import dev.carnager.noterious.model.ApiPageDetail
import dev.carnager.noterious.model.ApiPageSummary
import dev.carnager.noterious.model.ApiPagesResponse
import dev.carnager.noterious.model.ApiTaskItem
import dev.carnager.noterious.model.ApiTasksResponse
import dev.carnager.noterious.model.DerivedPageResponse
import dev.carnager.noterious.model.DocumentListResponse
import dev.carnager.noterious.model.DocumentRecord
import dev.carnager.noterious.model.FolderListResponse
import dev.carnager.noterious.model.PageHistoryResponse
import dev.carnager.noterious.model.QueryCopilotResponse
import dev.carnager.noterious.model.QueryWorkbenchResult
import dev.carnager.noterious.model.SavedQueryRecord
import dev.carnager.noterious.model.SearchResponse
import dev.carnager.noterious.model.ServerMetaResponse
import dev.carnager.noterious.model.ServerSettingsResponse
import dev.carnager.noterious.model.TaskItem
import dev.carnager.noterious.model.ThemeListResponse
import dev.carnager.noterious.model.ThemeRecord
import dev.carnager.noterious.model.TodaySnapshot
import dev.carnager.noterious.model.TrashListResponse
import dev.carnager.noterious.model.UserSettingsPayload
import dev.carnager.noterious.model.UserSettingsResponse
import dev.carnager.noterious.model.VaultRecord
import dev.carnager.noterious.model.VaultsResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDate
import java.time.ZoneId

class NoteriousRepository {
    private val authLock = Any()
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    data class DocumentMoveResult(
        val document: DocumentRecord,
        val targetPath: String,
        val rewrittenPages: List<String>,
    )

    @Serializable
    private data class SavePageRequest(
        val rawMarkdown: String,
        val baseRawMarkdown: String? = null,
    )

    @Serializable
    private data class MovePageRequest(
        val targetPage: String,
    )

    @Serializable
    private data class RestorePageHistoryRequest(
        val revisionId: String,
    )

    @Serializable
    private data class MoveDocumentRequest(
        val targetPath: String,
    )

    @Serializable
    private data class CreateFolderRequest(
        val folder: String,
    )

    @Serializable
    private data class MoveFolderRequest(
        val targetFolder: String = "",
        val name: String = "",
    )

    @Serializable
    private data class QueryWorkbenchRequest(
        val query: String,
        val previewLimit: Int,
    )

    @Serializable
    private data class QueryCopilotRequest(
        val intent: String,
        val currentQuery: String = "",
        val previewLimit: Int,
    )

    @Serializable
    private data class ChangePasswordRequest(
        val currentPassword: String,
        val newPassword: String,
    )

    @Serializable
    private data class NamedVaultRequest(
        val name: String,
    )

    @Serializable
    private data class TaskPatchRequest(
        val text: String? = null,
        val state: String? = null,
        val due: String? = null,
        val remind: String? = null,
        val click: String? = null,
    )

    @Serializable
    private data class PagePatchRequest(
        val frontmatter: FrontmatterPatch? = null,
    )

    @Serializable
    private data class FrontmatterPatch(
        val set: JsonObject? = null,
        val remove: List<String> = emptyList(),
    )

    @Serializable
    private data class PageDeletedResponse(
        val ok: Boolean = false,
        val page: String = "",
    )

    @Serializable
    private data class OkResponse(
        val ok: Boolean = false,
    )

    @Serializable
    private data class OkIdResponse(
        val ok: Boolean = false,
        val id: String = "",
    )

    @Serializable
    private data class CreatedFolderResponse(
        val folder: String = "",
    )

    @Serializable
    private data class MovedFolderResponse(
        val folder: String = "",
        val sourceFolder: String = "",
        val targetFolder: String = "",
        val name: String = "",
    )

    @Serializable
    private data class DeletedFolderResponse(
        val ok: Boolean = false,
        val folder: String = "",
    )

    @Serializable
    private data class MovedDocumentResponse(
        val document: DocumentRecord = DocumentRecord(),
        val sourcePath: String = "",
        val targetPath: String = "",
        val rewrittenPages: List<String> = emptyList(),
    )

    @Serializable
    private data class DeletedDocumentResponse(
        val ok: Boolean = false,
        val path: String = "",
    )

    @Volatile
    private var activeEventConnection: HttpURLConnection? = null

    data class ServerEvent(
        val type: String,
        val data: String?,
    )

    suspend fun fetchVaults(
        url: String,
        bearerToken: String,
        username: String,
        password: String,
    ): List<VaultRecord> = withContext(Dispatchers.IO) {
        val baseUrl = normalizeBaseUrl(url)
        ensureAuthenticated(baseUrl, bearerToken, username, password)
        runCatching {
            json.decodeFromString<VaultsResponse>(
                performGet(apiEndpointUrl(baseUrl, "user/vaults"), bearerToken, ""),
            ).vaults
        }.getOrDefault(emptyList())
    }

    suspend fun createVault(
        url: String,
        name: String,
        bearerToken: String,
        username: String,
        password: String,
    ): VaultRecord = withContext(Dispatchers.IO) {
        val baseUrl = normalizeBaseUrl(url)
        ensureAuthenticated(baseUrl, bearerToken, username, password)
        val requestBody = json.encodeToString(
            NamedVaultRequest(
                name = name.trim(),
            ),
        )
        json.decodeFromString(
            performRequest(
                method = "POST",
                url = apiEndpointUrl(baseUrl, "user/vaults"),
                bearerToken = bearerToken,
                scopePrefix = "",
                requestBody = requestBody,
            ),
        )
    }

    suspend fun renameVault(
        url: String,
        vaultId: Long,
        name: String,
        bearerToken: String,
        username: String,
        password: String,
    ): VaultRecord = withContext(Dispatchers.IO) {
        val baseUrl = normalizeBaseUrl(url)
        ensureAuthenticated(baseUrl, bearerToken, username, password)
        val requestBody = json.encodeToString(
            NamedVaultRequest(
                name = name.trim(),
            ),
        )
        json.decodeFromString(
            performRequest(
                method = "PUT",
                url = "${apiEndpointUrl(baseUrl, "user/vaults")}/${vaultId}",
                bearerToken = bearerToken,
                scopePrefix = "",
                requestBody = requestBody,
            ),
        )
    }

    suspend fun fetchServerSettings(
        url: String,
        bearerToken: String,
        username: String,
        password: String,
    ): ServerSettingsResponse = withContext(Dispatchers.IO) {
        val baseUrl = normalizeBaseUrl(url)
        ensureAuthenticated(baseUrl, bearerToken, username, password)
        json.decodeFromString(
            performGet(apiEndpointUrl(baseUrl, "settings"), bearerToken, ""),
        )
    }

    suspend fun fetchUserSettings(
        url: String,
        bearerToken: String,
        username: String,
        password: String,
    ): UserSettingsPayload = withContext(Dispatchers.IO) {
        val baseUrl = normalizeBaseUrl(url)
        ensureAuthenticated(baseUrl, bearerToken, username, password)
        json.decodeFromString<UserSettingsResponse>(
            performGet(apiEndpointUrl(baseUrl, "user/settings"), bearerToken, ""),
        ).settings
    }

    suspend fun saveUserSettings(
        url: String,
        settings: UserSettingsPayload,
        bearerToken: String,
        username: String,
        password: String,
    ): UserSettingsPayload = withContext(Dispatchers.IO) {
        val baseUrl = normalizeBaseUrl(url)
        ensureAuthenticated(baseUrl, bearerToken, username, password)
        val requestBody = json.encodeToString(UserSettingsResponse(settings = settings))
        json.decodeFromString<UserSettingsResponse>(
            performRequest(
                method = "PUT",
                url = apiEndpointUrl(baseUrl, "user/settings"),
                bearerToken = bearerToken,
                scopePrefix = "",
                requestBody = requestBody,
            ),
        ).settings
    }

    suspend fun fetchServerMeta(
        url: String,
        bearerToken: String,
        username: String,
        password: String,
    ): ServerMetaResponse = withContext(Dispatchers.IO) {
        val baseUrl = normalizeBaseUrl(url)
        ensureAuthenticated(baseUrl, bearerToken, username, password)
        json.decodeFromString(
            performGet(apiEndpointUrl(baseUrl, "meta"), bearerToken, ""),
        )
    }

    suspend fun fetchThemes(
        url: String,
        bearerToken: String,
        username: String,
        password: String,
    ): List<ThemeRecord> = withContext(Dispatchers.IO) {
        val baseUrl = normalizeBaseUrl(url)
        ensureAuthenticated(baseUrl, bearerToken, username, password)
        json.decodeFromString<ThemeListResponse>(
            performGet(apiEndpointUrl(baseUrl, "themes"), bearerToken, ""),
        ).themes
    }

    suspend fun uploadTheme(
        url: String,
        fileName: String,
        contentType: String,
        content: ByteArray,
        bearerToken: String,
        username: String,
        password: String,
    ): ThemeRecord = withContext(Dispatchers.IO) {
        val baseUrl = normalizeBaseUrl(url)
        ensureAuthenticated(baseUrl, bearerToken, username, password)
        json.decodeFromString(
            performMultipartRequest(
                url = apiEndpointUrl(baseUrl, "themes"),
                bearerToken = bearerToken,
                scopePrefix = "",
                formFields = emptyMap(),
                fileFieldName = "file",
                fileName = fileName,
                contentType = contentType.ifBlank { "application/json" },
                fileContent = content,
            ),
        )
    }

    suspend fun deleteTheme(
        url: String,
        themeId: String,
        bearerToken: String,
        username: String,
        password: String,
    ): String = withContext(Dispatchers.IO) {
        val baseUrl = normalizeBaseUrl(url)
        ensureAuthenticated(baseUrl, bearerToken, username, password)
        json.decodeFromString<OkIdResponse>(
            performRequest(
                method = "DELETE",
                url = "${apiEndpointUrl(baseUrl, "themes")}/${encodedPath(themeId)}",
                bearerToken = bearerToken,
                scopePrefix = "",
                requestBody = null,
            ),
        ).id
    }

    suspend fun changePassword(
        url: String,
        currentPassword: String,
        newPassword: String,
        bearerToken: String,
        username: String,
        password: String,
    ): ApiAuthSession = withContext(Dispatchers.IO) {
        val baseUrl = normalizeBaseUrl(url)
        ensureAuthenticated(baseUrl, bearerToken, username, password)
        val requestBody = json.encodeToString(
            ChangePasswordRequest(
                currentPassword = currentPassword,
                newPassword = newPassword,
            ),
        )
        json.decodeFromString(
            performRequest(
                method = "POST",
                url = apiEndpointUrl(baseUrl, "auth/change-password"),
                bearerToken = bearerToken,
                scopePrefix = "",
                requestBody = requestBody,
            ),
        )
    }

    suspend fun logout(
        url: String,
        bearerToken: String,
    ) = withContext(Dispatchers.IO) {
        val baseUrl = normalizeBaseUrl(url)
        try {
            performRequest(
                method = "POST",
                url = apiEndpointUrl(baseUrl, "auth/logout"),
                bearerToken = bearerToken,
                scopePrefix = "",
                requestBody = null,
            )
        } finally {
            clearCookies()
        }
    }

    suspend fun fetchPages(
        url: String,
        scopePrefix: String,
        bearerToken: String,
        username: String,
        password: String,
    ): List<ApiPageSummary> = withContext(Dispatchers.IO) {
        val baseUrl = normalizeBaseUrl(url)
        ensureAuthenticated(baseUrl, bearerToken, username, password)
        json.decodeFromString<ApiPagesResponse>(
            performGet(apiEndpointUrl(baseUrl, "pages"), bearerToken, scopePrefix),
        ).pages
    }

    suspend fun fetchTasks(
        url: String,
        scopePrefix: String,
        bearerToken: String,
        username: String,
        password: String,
    ): List<TaskItem> = withContext(Dispatchers.IO) {
        val baseUrl = normalizeBaseUrl(url)
        ensureAuthenticated(baseUrl, bearerToken, username, password)
        val apiTasks = json.decodeFromString<ApiTasksResponse>(
            performGet(apiEndpointUrl(baseUrl, "tasks"), bearerToken, scopePrefix),
        ).tasks
        apiTasks.map(::toTaskItem)
    }

    suspend fun fetchFolders(
        url: String,
        scopePrefix: String,
        bearerToken: String,
        username: String,
        password: String,
    ): List<String> = withContext(Dispatchers.IO) {
        val baseUrl = normalizeBaseUrl(url)
        ensureAuthenticated(baseUrl, bearerToken, username, password)
        json.decodeFromString<FolderListResponse>(
            performGet(apiEndpointUrl(baseUrl, "folders"), bearerToken, scopePrefix),
        ).folders
    }

    suspend fun fetchDocuments(
        url: String,
        scopePrefix: String,
        bearerToken: String,
        username: String,
        password: String,
    ): List<DocumentRecord> = withContext(Dispatchers.IO) {
        val baseUrl = normalizeBaseUrl(url)
        ensureAuthenticated(baseUrl, bearerToken, username, password)
        json.decodeFromString<DocumentListResponse>(
            performGet("${apiEndpointUrl(baseUrl, "documents")}?withUsage=1", bearerToken, scopePrefix),
        ).documents
    }

    suspend fun fetchPageDetail(
        url: String,
        pagePath: String,
        scopePrefix: String,
        bearerToken: String,
        username: String,
        password: String,
    ): ApiPageDetail = withContext(Dispatchers.IO) {
        val baseUrl = normalizeBaseUrl(url)
        ensureAuthenticated(baseUrl, bearerToken, username, password)
        json.decodeFromString<ApiPageDetail>(
            performGet(pageUrl(baseUrl, pagePath), bearerToken, scopePrefix),
        )
    }

    suspend fun fetchDerivedPage(
        url: String,
        pagePath: String,
        scopePrefix: String,
        bearerToken: String,
        username: String,
        password: String,
    ): DerivedPageResponse = withContext(Dispatchers.IO) {
        val baseUrl = normalizeBaseUrl(url)
        ensureAuthenticated(baseUrl, bearerToken, username, password)
        json.decodeFromString<DerivedPageResponse>(
            performGet("${pageUrl(baseUrl, pagePath)}/derived", bearerToken, scopePrefix),
        )
    }

    suspend fun savePage(
        url: String,
        pagePath: String,
        rawMarkdown: String,
        baseRawMarkdown: String,
        scopePrefix: String,
        bearerToken: String,
        username: String,
        password: String,
    ): ApiPageDetail = withContext(Dispatchers.IO) {
        val baseUrl = normalizeBaseUrl(url)
        ensureAuthenticated(baseUrl, bearerToken, username, password)
        val requestBody = json.encodeToString(
            SavePageRequest(
                rawMarkdown = rawMarkdown,
                baseRawMarkdown = baseRawMarkdown,
            ),
        )
        json.decodeFromString<ApiPageDetail>(
            performRequest(
                method = "PUT",
                url = pageUrl(baseUrl, pagePath),
                bearerToken = bearerToken,
                scopePrefix = scopePrefix,
                requestBody = requestBody,
            ),
        )
    }

    suspend fun createPage(
        url: String,
        pagePath: String,
        rawMarkdown: String,
        scopePrefix: String,
        bearerToken: String,
        username: String,
        password: String,
    ): ApiPageDetail = withContext(Dispatchers.IO) {
        val baseUrl = normalizeBaseUrl(url)
        ensureAuthenticated(baseUrl, bearerToken, username, password)
        val requestBody = json.encodeToString(
            SavePageRequest(
                rawMarkdown = rawMarkdown,
            ),
        )
        json.decodeFromString<ApiPageDetail>(
            performRequest(
                method = "PUT",
                url = pageUrl(baseUrl, pagePath),
                bearerToken = bearerToken,
                scopePrefix = scopePrefix,
                requestBody = requestBody,
            ),
        )
    }

    suspend fun movePage(
        url: String,
        pagePath: String,
        targetPage: String,
        scopePrefix: String,
        bearerToken: String,
        username: String,
        password: String,
    ): ApiPageDetail = withContext(Dispatchers.IO) {
        val baseUrl = normalizeBaseUrl(url)
        ensureAuthenticated(baseUrl, bearerToken, username, password)
        val requestBody = json.encodeToString(
            MovePageRequest(
                targetPage = targetPage,
            ),
        )
        json.decodeFromString<ApiPageDetail>(
            performRequest(
                method = "POST",
                url = "${pageUrl(baseUrl, pagePath)}/move",
                bearerToken = bearerToken,
                scopePrefix = scopePrefix,
                requestBody = requestBody,
            ),
        )
    }

    suspend fun deletePage(
        url: String,
        pagePath: String,
        scopePrefix: String,
        bearerToken: String,
        username: String,
        password: String,
    ): String = withContext(Dispatchers.IO) {
        val baseUrl = normalizeBaseUrl(url)
        ensureAuthenticated(baseUrl, bearerToken, username, password)
        json.decodeFromString<PageDeletedResponse>(
            performRequest(
                method = "DELETE",
                url = pageUrl(baseUrl, pagePath),
                bearerToken = bearerToken,
                scopePrefix = scopePrefix,
                requestBody = null,
            ),
        ).page
    }

    suspend fun createFolder(
        url: String,
        folderPath: String,
        scopePrefix: String,
        bearerToken: String,
        username: String,
        password: String,
    ): String = withContext(Dispatchers.IO) {
        val baseUrl = normalizeBaseUrl(url)
        ensureAuthenticated(baseUrl, bearerToken, username, password)
        val requestBody = json.encodeToString(
            CreateFolderRequest(
                folder = folderPath,
            ),
        )
        json.decodeFromString<CreatedFolderResponse>(
            performRequest(
                method = "POST",
                url = apiEndpointUrl(baseUrl, "folders"),
                bearerToken = bearerToken,
                scopePrefix = scopePrefix,
                requestBody = requestBody,
            ),
        ).folder
    }

    suspend fun moveFolder(
        url: String,
        folderPath: String,
        targetFolderPath: String,
        scopePrefix: String,
        bearerToken: String,
        username: String,
        password: String,
    ): String = withContext(Dispatchers.IO) {
        val normalizedTargetFolderPath = targetFolderPath.trim().trim('/').split('/')
            .map(String::trim)
            .filter(String::isNotBlank)
            .joinToString("/")
        val targetParentFolder = normalizedTargetFolderPath.substringBeforeLast('/', "")
        val targetName = normalizedTargetFolderPath.substringAfterLast('/', normalizedTargetFolderPath)
        val baseUrl = normalizeBaseUrl(url)
        ensureAuthenticated(baseUrl, bearerToken, username, password)
        val requestBody = json.encodeToString(
            MoveFolderRequest(
                targetFolder = targetParentFolder,
                name = targetName,
            ),
        )
        json.decodeFromString<MovedFolderResponse>(
            performRequest(
                method = "POST",
                url = "${folderUrl(baseUrl, folderPath)}/move",
                bearerToken = bearerToken,
                scopePrefix = scopePrefix,
                requestBody = requestBody,
            ),
        ).folder
    }

    suspend fun deleteFolder(
        url: String,
        folderPath: String,
        scopePrefix: String,
        bearerToken: String,
        username: String,
        password: String,
    ): String = withContext(Dispatchers.IO) {
        val baseUrl = normalizeBaseUrl(url)
        ensureAuthenticated(baseUrl, bearerToken, username, password)
        json.decodeFromString<DeletedFolderResponse>(
            performRequest(
                method = "DELETE",
                url = folderUrl(baseUrl, folderPath),
                bearerToken = bearerToken,
                scopePrefix = scopePrefix,
                requestBody = null,
            ),
        ).folder
    }

    suspend fun moveDocument(
        url: String,
        documentPath: String,
        targetPath: String,
        scopePrefix: String,
        bearerToken: String,
        username: String,
        password: String,
    ): DocumentMoveResult = withContext(Dispatchers.IO) {
        val baseUrl = normalizeBaseUrl(url)
        ensureAuthenticated(baseUrl, bearerToken, username, password)
        val requestBody = json.encodeToString(
            MoveDocumentRequest(
                targetPath = targetPath,
            ),
        )
        val response = json.decodeFromString<MovedDocumentResponse>(
            performRequest(
                method = "POST",
                url = documentMoveUrl(baseUrl, documentPath),
                bearerToken = bearerToken,
                scopePrefix = scopePrefix,
                requestBody = requestBody,
            ),
        )
        DocumentMoveResult(
            document = response.document,
            targetPath = response.targetPath.ifBlank { targetPath },
            rewrittenPages = response.rewrittenPages,
        )
    }

    suspend fun deleteDocument(
        url: String,
        documentPath: String,
        scopePrefix: String,
        bearerToken: String,
        username: String,
        password: String,
    ): String = withContext(Dispatchers.IO) {
        val baseUrl = normalizeBaseUrl(url)
        ensureAuthenticated(baseUrl, bearerToken, username, password)
        json.decodeFromString<DeletedDocumentResponse>(
            performRequest(
                method = "DELETE",
                url = documentUrl(baseUrl, documentPath),
                bearerToken = bearerToken,
                scopePrefix = scopePrefix,
                requestBody = null,
            ),
        ).path
    }

    suspend fun fetchPageHistory(
        url: String,
        pagePath: String,
        scopePrefix: String,
        bearerToken: String,
        username: String,
        password: String,
    ): PageHistoryResponse = withContext(Dispatchers.IO) {
        val baseUrl = normalizeBaseUrl(url)
        ensureAuthenticated(baseUrl, bearerToken, username, password)
        json.decodeFromString<PageHistoryResponse>(
            performGet(pageHistoryUrl(baseUrl, pagePath), bearerToken, scopePrefix),
        )
    }

    suspend fun restorePageHistory(
        url: String,
        pagePath: String,
        revisionId: String,
        scopePrefix: String,
        bearerToken: String,
        username: String,
        password: String,
    ): ApiPageDetail = withContext(Dispatchers.IO) {
        val baseUrl = normalizeBaseUrl(url)
        ensureAuthenticated(baseUrl, bearerToken, username, password)
        val requestBody = json.encodeToString(
            RestorePageHistoryRequest(
                revisionId = revisionId,
            ),
        )
        json.decodeFromString<ApiPageDetail>(
            performRequest(
                method = "POST",
                url = "${pageHistoryUrl(baseUrl, pagePath)}/restore",
                bearerToken = bearerToken,
                scopePrefix = scopePrefix,
                requestBody = requestBody,
            ),
        )
    }

    suspend fun deletePageHistory(
        url: String,
        pagePath: String,
        scopePrefix: String,
        bearerToken: String,
        username: String,
        password: String,
    ): String = withContext(Dispatchers.IO) {
        val baseUrl = normalizeBaseUrl(url)
        ensureAuthenticated(baseUrl, bearerToken, username, password)
        json.decodeFromString<PageDeletedResponse>(
            performRequest(
                method = "DELETE",
                url = pageHistoryUrl(baseUrl, pagePath),
                bearerToken = bearerToken,
                scopePrefix = scopePrefix,
                requestBody = null,
            ),
        ).page
    }

    suspend fun fetchTrashPages(
        url: String,
        scopePrefix: String,
        bearerToken: String,
        username: String,
        password: String,
    ): TrashListResponse = withContext(Dispatchers.IO) {
        val baseUrl = normalizeBaseUrl(url)
        ensureAuthenticated(baseUrl, bearerToken, username, password)
        json.decodeFromString<TrashListResponse>(
            performGet(apiEndpointUrl(baseUrl, "trash/pages"), bearerToken, scopePrefix),
        )
    }

    suspend fun restoreTrashPage(
        url: String,
        pagePath: String,
        scopePrefix: String,
        bearerToken: String,
        username: String,
        password: String,
    ): ApiPageDetail = withContext(Dispatchers.IO) {
        val baseUrl = normalizeBaseUrl(url)
        ensureAuthenticated(baseUrl, bearerToken, username, password)
        json.decodeFromString<ApiPageDetail>(
            performRequest(
                method = "POST",
                url = "${trashPageUrl(baseUrl, pagePath)}/restore",
                bearerToken = bearerToken,
                scopePrefix = scopePrefix,
                requestBody = null,
            ),
        )
    }

    suspend fun permanentlyDeleteTrashPage(
        url: String,
        pagePath: String,
        scopePrefix: String,
        bearerToken: String,
        username: String,
        password: String,
    ): String = withContext(Dispatchers.IO) {
        val baseUrl = normalizeBaseUrl(url)
        ensureAuthenticated(baseUrl, bearerToken, username, password)
        json.decodeFromString<PageDeletedResponse>(
            performRequest(
                method = "DELETE",
                url = trashPageUrl(baseUrl, pagePath),
                bearerToken = bearerToken,
                scopePrefix = scopePrefix,
                requestBody = null,
            ),
        ).page
    }

    suspend fun emptyTrash(
        url: String,
        scopePrefix: String,
        bearerToken: String,
        username: String,
        password: String,
    ): Boolean = withContext(Dispatchers.IO) {
        val baseUrl = normalizeBaseUrl(url)
        ensureAuthenticated(baseUrl, bearerToken, username, password)
        json.decodeFromString<OkResponse>(
            performRequest(
                method = "DELETE",
                url = apiEndpointUrl(baseUrl, "trash/pages"),
                bearerToken = bearerToken,
                scopePrefix = scopePrefix,
                requestBody = null,
            ),
        ).ok
    }

    suspend fun uploadDocument(
        url: String,
        pagePath: String,
        fileName: String,
        contentType: String,
        content: ByteArray,
        scopePrefix: String,
        bearerToken: String,
        username: String,
        password: String,
    ): DocumentRecord = withContext(Dispatchers.IO) {
        val baseUrl = normalizeBaseUrl(url)
        ensureAuthenticated(baseUrl, bearerToken, username, password)
        json.decodeFromString<DocumentRecord>(
            performMultipartRequest(
                url = apiEndpointUrl(baseUrl, "documents"),
                bearerToken = bearerToken,
                scopePrefix = scopePrefix,
                formFields = mapOf("page" to pagePath),
                fileFieldName = "file",
                fileName = fileName,
                contentType = contentType.ifBlank { "application/octet-stream" },
                fileContent = content,
            ),
        )
    }

    suspend fun patchPageFrontmatter(
        url: String,
        pagePath: String,
        scopePrefix: String,
        bearerToken: String,
        username: String,
        password: String,
        set: Map<String, JsonElement> = emptyMap(),
        remove: List<String> = emptyList(),
    ): ApiPageDetail = withContext(Dispatchers.IO) {
        val normalizedRemove = remove.map(String::trim).filter(String::isNotBlank)
        if (set.isEmpty() && normalizedRemove.isEmpty()) {
            return@withContext fetchPageDetail(
                url = url,
                pagePath = pagePath,
                scopePrefix = scopePrefix,
                bearerToken = bearerToken,
                username = username,
                password = password,
            )
        }

        val baseUrl = normalizeBaseUrl(url)
        ensureAuthenticated(baseUrl, bearerToken, username, password)
        val requestBody = json.encodeToString(
            PagePatchRequest(
                frontmatter = FrontmatterPatch(
                    set = set.takeIf(Map<String, JsonElement>::isNotEmpty)?.let(::JsonObject),
                    remove = normalizedRemove,
                ),
            ),
        )
        json.decodeFromString<ApiPageDetail>(
            performRequest(
                method = "PATCH",
                url = pageUrl(baseUrl, pagePath),
                bearerToken = bearerToken,
                scopePrefix = scopePrefix,
                requestBody = requestBody,
            ),
        )
    }

    suspend fun patchTask(
        url: String,
        taskRef: String,
        scopePrefix: String,
        bearerToken: String,
        username: String,
        password: String,
        text: String? = null,
        state: String? = null,
        due: String? = null,
        remind: String? = null,
        click: String? = null,
    ): ApiTaskItem = withContext(Dispatchers.IO) {
        val baseUrl = normalizeBaseUrl(url)
        ensureAuthenticated(baseUrl, bearerToken, username, password)
        val requestBody = json.encodeToString(
            TaskPatchRequest(
                text = text,
                state = state,
                due = due,
                remind = remind,
                click = click,
            ),
        )
        json.decodeFromString<ApiTaskItem>(
            performRequest(
                method = "PATCH",
                url = taskUrl(baseUrl, taskRef),
                bearerToken = bearerToken,
                scopePrefix = scopePrefix,
                requestBody = requestBody,
            ),
        )
    }

    suspend fun deleteTask(
        url: String,
        taskRef: String,
        scopePrefix: String,
        bearerToken: String,
        username: String,
        password: String,
    ) = withContext(Dispatchers.IO) {
        val baseUrl = normalizeBaseUrl(url)
        ensureAuthenticated(baseUrl, bearerToken, username, password)
        performRequest(
            method = "DELETE",
            url = taskUrl(baseUrl, taskRef),
            bearerToken = bearerToken,
            scopePrefix = scopePrefix,
            requestBody = null,
        )
    }

    suspend fun search(
        url: String,
        query: String,
        scopePrefix: String,
        bearerToken: String,
        username: String,
        password: String,
    ): SearchResponse = withContext(Dispatchers.IO) {
        val baseUrl = normalizeBaseUrl(url)
        ensureAuthenticated(baseUrl, bearerToken, username, password)
        val encodedQuery = URLEncoder.encode(query, Charsets.UTF_8.name())
        json.decodeFromString<SearchResponse>(
            performGet(
                "${apiEndpointUrl(baseUrl, "search")}?q=$encodedQuery",
                bearerToken,
                scopePrefix,
            ),
        )
    }

    suspend fun fetchSavedQuery(
        url: String,
        name: String,
        scopePrefix: String,
        bearerToken: String,
        username: String,
        password: String,
    ): SavedQueryRecord = withContext(Dispatchers.IO) {
        val baseUrl = normalizeBaseUrl(url)
        ensureAuthenticated(baseUrl, bearerToken, username, password)
        json.decodeFromString<SavedQueryRecord>(
            performGet(
                savedQueryUrl(baseUrl, name),
                bearerToken,
                scopePrefix,
            ),
        )
    }

    suspend fun runQueryWorkbench(
        url: String,
        query: String,
        previewLimit: Int,
        scopePrefix: String,
        bearerToken: String,
        username: String,
        password: String,
    ): QueryWorkbenchResult = withContext(Dispatchers.IO) {
        val baseUrl = normalizeBaseUrl(url)
        ensureAuthenticated(baseUrl, bearerToken, username, password)
        val requestBody = json.encodeToString(
            QueryWorkbenchRequest(
                query = query,
                previewLimit = previewLimit,
            ),
        )
        json.decodeFromString<QueryWorkbenchResult>(
            performRequest(
                method = "POST",
                url = queryWorkbenchUrl(baseUrl),
                bearerToken = bearerToken,
                scopePrefix = scopePrefix,
                requestBody = requestBody,
            ),
        )
    }

    suspend fun generateQueryCopilot(
        url: String,
        intent: String,
        currentQuery: String,
        previewLimit: Int,
        scopePrefix: String,
        bearerToken: String,
        username: String,
        password: String,
    ): QueryCopilotResponse = withContext(Dispatchers.IO) {
        val baseUrl = normalizeBaseUrl(url)
        ensureAuthenticated(baseUrl, bearerToken, username, password)
        val requestBody = json.encodeToString(
            QueryCopilotRequest(
                intent = intent,
                currentQuery = currentQuery,
                previewLimit = previewLimit,
            ),
        )
        try {
            json.decodeFromString<QueryCopilotResponse>(
                performRequest(
                    method = "POST",
                    url = queryCopilotUrl(baseUrl),
                    bearerToken = bearerToken,
                    scopePrefix = scopePrefix,
                    requestBody = requestBody,
                    readTimeoutMs = 90_000,
                ),
            )
        } catch (error: IllegalStateException) {
            val bodyMessage = error.message
                ?.substringAfterLast(": ")
                ?.trim()
                ?.takeIf(String::isNotBlank)
            throw IllegalStateException(bodyMessage ?: error.message ?: "AI query generation failed.")
        }
    }

    suspend fun consumeEvents(
        url: String,
        scopePrefix: String,
        bearerToken: String,
        username: String,
        password: String,
        onConnected: (() -> Unit)? = null,
        onEvent: (ServerEvent) -> Unit,
    ) = withContext(Dispatchers.IO) {
        val baseUrl = normalizeBaseUrl(url)
        ensureAuthenticated(baseUrl, bearerToken, username, password)

        val connection = (URL(eventsUrl(baseUrl, scopePrefix)).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 0
            setRequestProperty("Accept", "text/event-stream")
            if (bearerToken.isNotBlank()) {
                setRequestProperty("Authorization", "Bearer $bearerToken")
            }
        }

        try {
            activeEventConnection = connection
            val status = connection.responseCode
            if (status !in 200..299) {
                val body = connection.errorStream?.let(::readStream).orEmpty()
                throw IllegalStateException("Event stream failed: HTTP $status: $body")
            }
            onConnected?.invoke()

            BufferedReader(InputStreamReader(BufferedInputStream(connection.inputStream))).use { reader ->
                var eventType: String? = null
                val dataLines = mutableListOf<String>()
                while (true) {
                    val line = reader.readLine() ?: break
                    when {
                        line.isEmpty() -> {
                            val type = eventType?.takeIf(String::isNotBlank)
                            if (type != null) {
                                onEvent(ServerEvent(type = type, data = dataLines.joinToString("\n").ifBlank { null }))
                            }
                            eventType = null
                            dataLines.clear()
                        }
                        line.startsWith(":") -> Unit
                        line.startsWith("event:") -> eventType = line.substringAfter(':').trim()
                        line.startsWith("data:") -> dataLines += line.substringAfter(':').trimStart()
                    }
                }
            }
        } finally {
            if (activeEventConnection === connection) {
                activeEventConnection = null
            }
            connection.disconnect()
        }
    }

    fun closeEventStream() {
        activeEventConnection?.disconnect()
        activeEventConnection = null
    }

    fun buildTodaySnapshot(tasks: List<TaskItem>): TodaySnapshot {
        val today = LocalDate.now(ZoneId.systemDefault())
        val todayString = today.toString()
        val openTasks = tasks.filterNot { it.done }

        val overdue = openTasks.filter { task ->
            task.due?.let(::parseIsoDate)?.isBefore(today) == true
        }
        val dueToday = openTasks.filter { task ->
            task.due?.let(::parseIsoDate) == today
        }
        val remindersToday = openTasks.filter { task ->
            task.remind?.take(10) == todayString
        }

        return TodaySnapshot(
            date = todayString,
            overdue = overdue,
            dueToday = dueToday,
            remindersToday = remindersToday,
        )
    }

    private fun toTaskItem(task: ApiTaskItem): TaskItem {
        val whoValues = jsonElementStrings(task.who)
        val who = whoValues.joinToString(", ").ifBlank { null }
        return TaskItem(
            ref = task.ref,
            page = task.page,
            name = task.text,
            state = task.state,
            done = task.done,
            pos = task.line,
            line = task.line,
            who = who,
            due = task.due,
            remind = task.remind,
            click = task.click,
            followup = who != null,
            remindCandidate = !task.remind.isNullOrBlank() || !task.due.isNullOrBlank(),
        )
    }

    private fun parseIsoDate(raw: String): LocalDate? {
        return runCatching { LocalDate.parse(raw) }.getOrNull()
    }

    private fun ensureAuthenticated(baseUrl: String, bearerToken: String, username: String, password: String) {
        if (bearerToken.isNotBlank() || username.isBlank() || password.isBlank()) {
            return
        }

        synchronized(authLock) {
            val currentSession = runCatching {
                loadSession(baseUrl)
            }.getOrNull()
            if (currentSession?.authenticated == true) {
                return
            }
            if (currentSession?.setupRequired == true) {
                throw IllegalStateException("Server requires initial account setup.")
            }

            runCatching {
                login(baseUrl, username, password)
            }.onFailure { error ->
                if (!isSkippableLoginError(error)) {
                    throw error
                }
            }
        }
    }

    private fun isMissingApiLoginEndpoint(error: Throwable): Boolean {
        val message = error.message.orEmpty()
        return message.startsWith("API login failed: HTTP 404:") ||
            message.startsWith("API login failed: HTTP 405:") ||
            message.startsWith("API login failed: HTTP 501:")
    }

    private fun isSkippableLoginError(error: Throwable): Boolean {
        val message = error.message.orEmpty()
        return isMissingApiLoginEndpoint(error) ||
            message.startsWith("Login failed: HTTP 404:") ||
            message.startsWith("Login failed: HTTP 405:") ||
            message.startsWith("Login failed: HTTP 501:") ||
            message.startsWith("API login failed: HTTP 503:")
    }

    private fun login(baseUrl: String, username: String, password: String) {
        clearCookies()
        runCatching {
            loginWithApi(baseUrl, username, password)
        }.onFailure { error ->
            if (!isMissingApiLoginEndpoint(error)) {
                throw error
            }
            loginWithLegacyEndpoint(baseUrl, username, password)
        }
    }

    private fun loadSession(baseUrl: String): ApiAuthSession {
        return json.decodeFromString(performGet(apiEndpointUrl(baseUrl, "auth/me"), "", ""))
    }

    private fun loginWithApi(baseUrl: String, username: String, password: String) {
        val authUrl = apiEndpointUrl(baseUrl, "auth/login")
        val requestBody = json.encodeToString(
            kotlinx.serialization.serializer<Map<String, String>>(),
            mapOf("username" to username, "password" to password),
        )

        val connection = (URL(authUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10_000
            readTimeout = 10_000
            doOutput = true
            instanceFollowRedirects = false
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
        }

        try {
            writeBody(connection.outputStream, requestBody)
            val status = connection.responseCode
            val responseBody = connection.inputStreamOrError()
            if (status !in 200..299) {
                throw IllegalStateException("API login failed: HTTP $status: $responseBody")
            }
            val session = runCatching {
                json.decodeFromString<ApiAuthSession>(responseBody)
            }.getOrElse {
                throw IllegalStateException("API login failed: invalid response body")
            }
            if (!session.authenticated) {
                throw IllegalStateException("API login failed: session not authenticated")
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun loginWithLegacyEndpoint(baseUrl: String, username: String, password: String) {
        val authUrl = "$baseUrl/.auth"
        val body = buildString {
            append("username=")
            append(URLEncoder.encode(username, Charsets.UTF_8.name()))
            append("&password=")
            append(URLEncoder.encode(password, Charsets.UTF_8.name()))
            append("&rememberMe=true")
        }

        val connection = (URL(authUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10_000
            readTimeout = 10_000
            doOutput = true
            instanceFollowRedirects = false
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            setRequestProperty("Accept", "application/json")
        }

        try {
            writeBody(connection.outputStream, body)
            val status = connection.responseCode
            val responseBody = connection.inputStreamOrError()
            if (status !in 200..299) {
                throw IllegalStateException("Login failed: HTTP $status: $responseBody")
            }
            if (!responseBody.contains("\"status\":\"ok\"")) {
                throw IllegalStateException("Login failed: $responseBody")
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun clearCookies() {
        cookieManager.cookieStore.removeAll()
    }

    private fun performGet(url: String, bearerToken: String, scopePrefix: String): String {
        return performRequest(
            method = "GET",
            url = url,
            bearerToken = bearerToken,
            scopePrefix = scopePrefix,
            requestBody = null,
        )
    }

    private fun performRequest(
        method: String,
        url: String,
        bearerToken: String,
        scopePrefix: String,
        requestBody: String?,
        readTimeoutMs: Int = 10_000,
    ): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 10_000
            readTimeout = readTimeoutMs
            setRequestProperty("Accept", "application/json")
            doOutput = requestBody != null
            if (bearerToken.isNotBlank()) {
                setRequestProperty("Authorization", "Bearer $bearerToken")
            }
            normalizedScopePrefix(scopePrefix).takeIf(String::isNotBlank)?.let { scope ->
                setRequestProperty("X-Noterious-Scope", scope)
            }
            if (requestBody != null) {
                setRequestProperty("Content-Type", "application/json")
            }
        }

        try {
            if (requestBody != null) {
                writeBody(connection.outputStream, requestBody)
            }
            val status = connection.responseCode
            val body = connection.inputStreamOrError()
            if (status !in 200..299) {
                if (status == 404) {
                    throw IllegalStateException("API endpoint not found: $url")
                }
                throw IllegalStateException("HTTP $status from $url: $body")
            }
            return body
        } finally {
            connection.disconnect()
        }
    }

    private fun performMultipartRequest(
        url: String,
        bearerToken: String,
        scopePrefix: String,
        formFields: Map<String, String>,
        fileFieldName: String,
        fileName: String,
        contentType: String,
        fileContent: ByteArray,
    ): String {
        val boundary = "----NoteriousBoundary${System.currentTimeMillis()}"
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10_000
            readTimeout = 30_000
            doOutput = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            if (bearerToken.isNotBlank()) {
                setRequestProperty("Authorization", "Bearer $bearerToken")
            }
            normalizedScopePrefix(scopePrefix).takeIf(String::isNotBlank)?.let { scope ->
                setRequestProperty("X-Noterious-Scope", scope)
            }
        }

        try {
            connection.outputStream.use { output ->
                formFields.forEach { (name, value) ->
                    writeMultipartField(output, boundary, name, value)
                }
                writeMultipartFile(
                    outputStream = output,
                    boundary = boundary,
                    fieldName = fileFieldName,
                    fileName = fileName,
                    contentType = contentType,
                    content = fileContent,
                )
                output.write("--$boundary--\r\n".toByteArray(Charsets.UTF_8))
            }

            val status = connection.responseCode
            val body = connection.inputStreamOrError()
            if (status !in 200..299) {
                throw IllegalStateException("HTTP $status from $url: $body")
            }
            return body
        } finally {
            connection.disconnect()
        }
    }

    private fun normalizeBaseUrl(rawUrl: String): String {
        var trimmed = rawUrl.trim().removeSuffix("/")
        if (trimmed.isBlank()) return trimmed
        val knownSuffixes = listOf("/api", "/api/tasks", "/api/pages", "/api/auth/login", "/api/auth/me")
        for (suffix in knownSuffixes) {
            if (trimmed.contains(suffix)) {
                trimmed = trimmed.substringBefore(suffix)
                break
            }
        }
        return trimmed.removeSuffix("/")
    }

    private fun apiEndpointUrl(baseUrl: String, endpoint: String): String {
        return "$baseUrl/api/$endpoint"
    }

    private fun pageUrl(baseUrl: String, pagePath: String): String {
        return "${apiEndpointUrl(baseUrl, "pages")}/${encodedPath(pagePath)}"
    }

    private fun folderUrl(baseUrl: String, folderPath: String): String {
        return "${apiEndpointUrl(baseUrl, "folders")}/${encodedPath(folderPath)}"
    }

    private fun documentUrl(baseUrl: String, documentPath: String): String {
        return "${apiEndpointUrl(baseUrl, "documents")}/${encodedPath(documentPath)}"
    }

    private fun documentMoveUrl(baseUrl: String, documentPath: String): String {
        return "${apiEndpointUrl(baseUrl, "documents/move")}/${encodedPath(documentPath)}"
    }

    private fun pageHistoryUrl(baseUrl: String, pagePath: String): String {
        return "${apiEndpointUrl(baseUrl, "page-history")}/${encodedPath(pagePath)}"
    }

    private fun trashPageUrl(baseUrl: String, pagePath: String): String {
        return "${apiEndpointUrl(baseUrl, "trash/pages")}/${encodedPath(pagePath)}"
    }

    private fun queryWorkbenchUrl(baseUrl: String): String {
        return apiEndpointUrl(baseUrl, "query/workbench")
    }

    private fun savedQueryUrl(baseUrl: String, name: String): String {
        return "${apiEndpointUrl(baseUrl, "queries")}/${encodedPath(name)}"
    }

    private fun queryCopilotUrl(baseUrl: String): String {
        return apiEndpointUrl(baseUrl, "query/copilot")
    }

    private fun encodedPath(value: String): String {
        return value.trim().split("/").joinToString("/") { segment ->
            URLEncoder.encode(segment, Charsets.UTF_8.name()).replace("+", "%20")
        }
    }

    private fun taskUrl(baseUrl: String, taskRef: String): String {
        val encodedRef = URLEncoder.encode(taskRef.trim(), Charsets.UTF_8.name()).replace("+", "%20")
        return "${apiEndpointUrl(baseUrl, "tasks")}/$encodedRef"
    }

    private fun eventsUrl(baseUrl: String, scopePrefix: String): String {
        val normalizedScope = normalizedScopePrefix(scopePrefix)
        if (normalizedScope.isBlank()) {
            return apiEndpointUrl(baseUrl, "events")
        }
        return "${apiEndpointUrl(baseUrl, "events")}?scope=${URLEncoder.encode(normalizedScope, Charsets.UTF_8.name())}"
    }

    private fun normalizedScopePrefix(scopePrefix: String): String {
        return scopePrefix.trim().trim('/')
    }

    private fun jsonElementStrings(element: JsonElement?): List<String> {
        return when (element) {
            null, JsonNull -> emptyList()
            is JsonArray -> element.mapNotNull { it.jsonPrimitiveContentOrNull() }
            is JsonPrimitive -> listOfNotNull(element.jsonPrimitiveContentOrNull())
            else -> emptyList()
        }
    }

    private fun JsonElement.jsonPrimitiveContentOrNull(): String? {
        return (this as? JsonPrimitive)?.content
    }

    private fun HttpURLConnection.inputStreamOrError(): String {
        val stream = if (responseCode in 200..299) inputStream else errorStream
        return stream?.let(::readStream).orEmpty()
    }

    private fun readStream(stream: java.io.InputStream): String {
        return BufferedReader(InputStreamReader(stream)).use { reader -> reader.readText() }
    }

    private fun writeBody(outputStream: OutputStream, body: String) {
        outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.write(body)
        }
    }

    private fun writeMultipartField(
        outputStream: OutputStream,
        boundary: String,
        name: String,
        value: String,
    ) {
        outputStream.write("--$boundary\r\n".toByteArray(Charsets.UTF_8))
        outputStream.write("Content-Disposition: form-data; name=\"$name\"\r\n\r\n".toByteArray(Charsets.UTF_8))
        outputStream.write(value.toByteArray(Charsets.UTF_8))
        outputStream.write("\r\n".toByteArray(Charsets.UTF_8))
    }

    private fun writeMultipartFile(
        outputStream: OutputStream,
        boundary: String,
        fieldName: String,
        fileName: String,
        contentType: String,
        content: ByteArray,
    ) {
        val safeFileName = fileName.replace("\"", "")
        outputStream.write("--$boundary\r\n".toByteArray(Charsets.UTF_8))
        outputStream.write(
            "Content-Disposition: form-data; name=\"$fieldName\"; filename=\"$safeFileName\"\r\n"
                .toByteArray(Charsets.UTF_8),
        )
        outputStream.write("Content-Type: $contentType\r\n\r\n".toByteArray(Charsets.UTF_8))
        outputStream.write(content)
        outputStream.write("\r\n".toByteArray(Charsets.UTF_8))
    }

    companion object {
        private val cookieManager = CookieManager(null, CookiePolicy.ACCEPT_ALL).also {
            java.net.CookieHandler.setDefault(it)
        }
    }
}
