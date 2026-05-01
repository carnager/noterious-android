package dev.carnager.noterious.data

import dev.carnager.noterious.model.ApiAuthSession
import dev.carnager.noterious.model.ApiPageDetail
import dev.carnager.noterious.model.ApiPageSummary
import dev.carnager.noterious.model.ApiPagesResponse
import dev.carnager.noterious.model.ApiTaskItem
import dev.carnager.noterious.model.ApiTasksResponse
import dev.carnager.noterious.model.DerivedPageResponse
import dev.carnager.noterious.model.DocumentRecord
import dev.carnager.noterious.model.SearchResponse
import dev.carnager.noterious.model.TaskItem
import dev.carnager.noterious.model.TodaySnapshot
import dev.carnager.noterious.model.VaultRecord
import dev.carnager.noterious.model.VaultsResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
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
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Serializable
    private data class SavePageRequest(
        val rawMarkdown: String,
        val baseRawMarkdown: String? = null,
    )

    @Serializable
    private data class TaskPatchRequest(
        val text: String? = null,
        val state: String? = null,
        val due: String? = null,
        val remind: String? = null,
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
        apiTasks.filter { !it.done }.map(::toTaskItem)
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
    ): ApiTaskItem = withContext(Dispatchers.IO) {
        val baseUrl = normalizeBaseUrl(url)
        ensureAuthenticated(baseUrl, bearerToken, username, password)
        val requestBody = json.encodeToString(
            TaskPatchRequest(
                text = text,
                state = state,
                due = due,
                remind = remind,
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

        val overdue = tasks.filter { task ->
            task.due?.let(::parseIsoDate)?.isBefore(today) == true
        }
        val dueToday = tasks.filter { task ->
            task.due?.let(::parseIsoDate) == today
        }
        val remindersToday = tasks.filter { task ->
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
    ): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 10_000
            readTimeout = 10_000
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
        val encodedPath = pagePath.trim().split("/").joinToString("/") { segment ->
            URLEncoder.encode(segment, Charsets.UTF_8.name()).replace("+", "%20")
        }
        return "${apiEndpointUrl(baseUrl, "pages")}/$encodedPath"
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
