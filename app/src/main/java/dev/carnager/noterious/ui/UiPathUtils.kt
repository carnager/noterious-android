package dev.carnager.noterious.ui

import dev.carnager.noterious.model.DocumentRecord
import dev.carnager.noterious.model.VaultRecord
import java.net.URLEncoder

internal fun normalizePagePath(value: String): String {
    return value
        .trim()
        .replace('\\', '/')
        .replace(Regex("\\.md$", RegexOption.IGNORE_CASE), "")
        .replace(Regex("^/+"), "")
        .replace(Regex("/+"), "/")
        .split('/')
        .map(String::trim)
        .filter(String::isNotBlank)
        .joinToString("/")
}

internal fun normalizeScopePrefix(value: String): String = normalizePagePath(value)

internal fun scopePrefixForVault(vault: VaultRecord): String {
    val fallbackPath = vault.vaultPath.replace('\\', '/').substringAfterLast('/', vault.vaultPath)
    val rawValue = vault.name
        .takeIf(String::isNotBlank)
        ?: vault.key.takeIf(String::isNotBlank)
        ?: fallbackPath
    return normalizeScopePrefix(rawValue)
}

internal fun displayScopeName(value: String): String {
    val normalized = normalizeScopePrefix(value)
    if (normalized.isBlank()) {
        return ""
    }
    return normalized
        .substringAfterLast('/')
        .removePrefix("root__")
        .removePrefix("root_")
        .removePrefix("root-")
        .trim()
}

internal fun displayScopeName(vault: VaultRecord): String {
    val pathLeaf = vault.vaultPath.replace('\\', '/').substringAfterLast('/', vault.vaultPath)
    return sequenceOf(vault.name, pathLeaf, vault.key)
        .map(::displayScopeName)
        .firstOrNull(String::isNotBlank)
        ?: "Scope"
}

internal fun displayCurrentScopeLabel(scopePrefix: String, vaults: List<VaultRecord>): String {
    val normalizedScopePrefix = normalizeScopePrefix(scopePrefix)
    if (normalizedScopePrefix.isBlank()) {
        return "All scopes"
    }
    return vaults
        .firstOrNull { scopePrefixForVault(it) == normalizedScopePrefix }
        ?.let(::displayScopeName)
        ?: displayScopeName(normalizedScopePrefix).ifBlank { normalizedScopePrefix }
}

internal fun displayPagePath(path: String, scopePrefix: String): String {
    val normalizedPath = normalizePagePath(path)
    val normalizedScopePrefix = normalizeScopePrefix(scopePrefix)
    if (normalizedPath.isBlank() || normalizedScopePrefix.isBlank()) {
        return normalizedPath
    }
    if (normalizedPath == normalizedScopePrefix) {
        return ""
    }
    if (normalizedPath.startsWith("$normalizedScopePrefix/")) {
        return normalizedPath.removePrefix("$normalizedScopePrefix/")
    }
    return normalizedPath
}

internal fun applyScopePrefixToPagePath(pagePath: String, scopePrefix: String): String {
    val normalizedPath = normalizePagePath(pagePath)
    val normalizedScopePrefix = normalizeScopePrefix(scopePrefix)
    if (
        normalizedPath.isBlank() ||
        normalizedScopePrefix.isBlank() ||
        normalizedPath == normalizedScopePrefix ||
        normalizedPath.startsWith("$normalizedScopePrefix/")
    ) {
        return normalizedPath
    }
    return "$normalizedScopePrefix/$normalizedPath"
}

internal fun pageTitleFromPath(pagePath: String): String {
    val normalized = normalizePagePath(pagePath)
    return normalized.substringAfterLast('/', normalized)
}

internal fun noteriousPageDeepLink(pagePath: String): String {
    val normalizedPagePath = normalizePagePath(pagePath)
    if (normalizedPagePath.isBlank()) {
        return "noterious://open"
    }
    val encodedPagePath = URLEncoder.encode(normalizedPagePath, Charsets.UTF_8.name())
    return "noterious://open?page=$encodedPagePath"
}

internal fun pageDirectory(pagePath: String): String {
    val normalized = normalizePagePath(pagePath)
    if (!normalized.contains('/')) {
        return ""
    }
    return normalized.substringBeforeLast('/', "")
}

internal fun resolveRelativePath(currentPagePath: String, target: String): String {
    val normalizedTarget = target.trim()
    if (normalizedTarget.isBlank() || normalizedTarget.startsWith('#') || Regex("^[a-z]+:", RegexOption.IGNORE_CASE).containsMatchIn(normalizedTarget)) {
        return normalizedTarget
    }

    val baseDirectory = pageDirectory(currentPagePath)
    val source = buildString {
        if (baseDirectory.isNotBlank()) {
            append(baseDirectory)
            append('/')
        }
        append(normalizedTarget)
    }

    val resolvedSegments = ArrayDeque<String>()
    source
        .replace('\\', '/')
        .split('/')
        .forEach { segment ->
            when (val trimmed = segment.trim()) {
                "", "." -> Unit
                ".." -> if (resolvedSegments.isNotEmpty()) {
                    resolvedSegments.removeLast()
                }
                else -> resolvedSegments.addLast(trimmed)
            }
    }
    return resolvedSegments.joinToString("/")
}

internal fun resolvePageLinkTarget(currentPagePath: String, target: String, scopePrefix: String): String {
    val trimmedTarget = target.trim()
    if (
        trimmedTarget.isBlank() ||
        trimmedTarget.startsWith('#') ||
        Regex("^[a-z]+:", RegexOption.IGNORE_CASE).containsMatchIn(trimmedTarget)
    ) {
        return trimmedTarget
    }

    if (trimmedTarget.startsWith("/")) {
        return normalizePagePath(trimmedTarget)
    }

    val normalizedPathTarget = normalizePagePath(trimmedTarget)
    val normalizedScopePrefix = normalizeScopePrefix(scopePrefix)
    if (
        normalizedPathTarget.isNotBlank() &&
        normalizedScopePrefix.isNotBlank() &&
        (normalizedPathTarget == normalizedScopePrefix || normalizedPathTarget.startsWith("$normalizedScopePrefix/"))
    ) {
        return normalizedPathTarget
    }

    val usesExplicitRelativeSyntax = trimmedTarget
        .replace('\\', '/')
        .let { it == "." || it == ".." || it.startsWith("./") || it.startsWith("../") }
    if (usesExplicitRelativeSyntax) {
        return normalizePagePath(resolveRelativePath(currentPagePath, trimmedTarget))
    }

    return applyScopePrefixToPagePath(normalizedPathTarget, scopePrefix)
}

internal fun normalizeServerBaseUrl(rawUrl: String): String {
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

internal fun documentDownloadUrl(baseUrl: String, documentPath: String, inline: Boolean = false): String {
    val normalizedBaseUrl = normalizeServerBaseUrl(baseUrl)
    val normalizedDocumentPath = normalizePagePath(documentPath)
    if (normalizedBaseUrl.isBlank() || normalizedDocumentPath.isBlank()) {
        return ""
    }
    val encodedPath = URLEncoder.encode(normalizedDocumentPath, Charsets.UTF_8.name())
    val suffix = if (inline) "&inline=1" else ""
    return "$normalizedBaseUrl/api/documents/download?path=$encodedPath$suffix"
}

internal fun isImagePath(path: String): Boolean {
    return Regex("\\.(avif|bmp|gif|ico|jpe?g|png|svg|webp)(?:[?#].*)?$", RegexOption.IGNORE_CASE)
        .containsMatchIn(path.trim())
}

internal fun looksLikeDocumentPath(path: String): Boolean {
    val normalized = normalizePagePath(path)
    val leaf = normalized.substringAfterLast('/', normalized)
    return leaf.contains('.') && !leaf.endsWith(".md", ignoreCase = true)
}

internal fun relativeDocumentPath(currentPagePath: String, documentPath: String): String {
    val fromDirectory = pageDirectory(currentPagePath)
    val targetPath = normalizePagePath(documentPath)
    val fromParts = fromDirectory.split('/').filter(String::isNotBlank)
    val targetParts = targetPath.split('/').filter(String::isNotBlank)

    var common = 0
    while (
        common < fromParts.size &&
        common < targetParts.size &&
        fromParts[common] == targetParts[common]
    ) {
        common += 1
    }

    val upwards = List((fromParts.size - common).coerceAtLeast(0)) { ".." }
    val downwards = targetParts.drop(common)
    return (upwards + downwards).joinToString("/").ifBlank {
        targetPath.substringAfterLast('/', targetPath)
    }
}

internal fun isImageContentType(contentType: String): Boolean {
    return Regex("^image/", RegexOption.IGNORE_CASE).containsMatchIn(contentType.trim())
}

internal fun documentEmbedsInline(document: DocumentRecord): Boolean {
    return isImageContentType(document.contentType) || isImagePath(document.path.ifBlank { document.name })
}

internal fun markdownLinkForDocument(document: DocumentRecord, currentPagePath: String): String {
    val label = document.name.ifBlank { document.path.substringAfterLast('/', document.path) }
        .replace("]", "\\]")
    val target = relativeDocumentPath(currentPagePath, document.path)
    return if (documentEmbedsInline(document)) {
        "![$label]($target)"
    } else {
        "[$label]($target)"
    }
}

internal fun rawOffsetForLineNumber(markdown: String, lineNumber: Int): Int {
    val source = markdown.replace("\r\n", "\n")
    val lines = source.split('\n')
    val target = lineNumber.coerceIn(1, lines.size.coerceAtLeast(1))
    var offset = 0
    for (index in 1 until target) {
        offset += lines[index - 1].length + 1
    }
    return offset
}

internal fun buildMarkdownTable(columns: Int, rows: Int): String {
    val safeColumns = clampTableDimension(columns, fallback = 2, max = 20)
    val safeRows = clampTableDimension(rows, fallback = 1, max = 50)
    val header = when (safeColumns) {
        1 -> listOf("Column")
        2 -> listOf("Column", "Value")
        else -> List(safeColumns) { index -> "Column ${index + 1}" }
    }
    val separator = List(safeColumns) { "---" }
    val blankRow = List(safeColumns) { "" }

    val lines = mutableListOf(
        "| ${header.joinToString(" | ")} |",
        "| ${separator.joinToString(" | ")} |",
    )
    repeat(safeRows) {
        lines += "| ${blankRow.joinToString(" | ")} |"
    }
    return lines.joinToString("\n") + "\n"
}

private fun clampTableDimension(value: Int, fallback: Int, max: Int): Int {
    return value.takeIf { it > 0 }?.coerceAtMost(max) ?: fallback
}
