package dev.carnager.noterious.ui

import dev.carnager.noterious.model.ApiPageDetail
import dev.carnager.noterious.model.ApiPageSummary
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

internal const val templateFolderName = "_templates"
internal const val templateMarkerKey = "_template"
internal const val templateLabelKey = "_template_label"
internal const val templateFolderKey = "_template_folder"
internal const val templateListKey = "_template_list"
internal const val templateTagsKey = "_template_tags"
internal const val templateBoolKey = "_template_bool"
internal const val templateDateKey = "_template_date"
internal const val templateDateTimeKey = "_template_datetime"
internal const val templateNotificationKey = "_template_notification"

internal const val propertyListKey = "_type_list"
internal const val propertyTagsKey = "_type_tags"
internal const val propertyBoolKey = "_type_bool"
internal const val propertyDateKey = "_type_date"
internal const val propertyDateTimeKey = "_type_datetime"
internal const val propertyNotificationKey = "_type_notification"

private val frontmatterKindMetadataMappings = listOf(
    propertyTagsKey to listOf(propertyTagsKey, templateTagsKey),
    propertyListKey to listOf(propertyListKey, templateListKey),
    propertyBoolKey to listOf(propertyBoolKey, templateBoolKey),
    propertyDateKey to listOf(propertyDateKey, templateDateKey),
    propertyDateTimeKey to listOf(propertyDateTimeKey, templateDateTimeKey),
    propertyNotificationKey to listOf(propertyNotificationKey, templateNotificationKey),
)

private val templateMetadataKeys = setOf(
    templateMarkerKey,
    templateLabelKey,
    templateFolderKey,
    templateListKey,
    templateTagsKey,
    templateBoolKey,
    templateDateKey,
    templateDateTimeKey,
    templateNotificationKey,
)

private val propertyMetadataKeys = setOf(
    propertyListKey,
    propertyTagsKey,
    propertyBoolKey,
    propertyDateKey,
    propertyDateTimeKey,
    propertyNotificationKey,
)

private val internalTemplateMetadataKeys = templateMetadataKeys + propertyMetadataKeys

internal data class NoteTemplate(
    val id: String,
    val name: String,
    val folder: String,
    val fieldKeys: List<String> = emptyList(),
)

private data class TemplatePathInfo(
    val scopePrefix: String,
    val relativePath: String,
)

internal fun isInternalTemplateMetadataKey(key: String): Boolean {
    return internalTemplateMetadataKeys.contains(key.trim())
}

internal fun noteTemplatesFromPages(
    pages: List<ApiPageSummary>,
    scopePrefix: String,
): List<NoteTemplate> {
    return pages
        .mapNotNull(::noteTemplateFromPage)
        .filter { template -> templateScopeMatches(template.id, scopePrefix) }
        .sortedWith(compareBy({ it.name.lowercase() }, { it.id.lowercase() }))
}

internal fun buildPagePathFromTemplate(template: NoteTemplate, draftPath: String): String {
    val normalizedDraftPath = normalizePagePath(draftPath)
    if (normalizedDraftPath.isBlank()) {
        return ""
    }
    val normalizedFolder = normalizePagePath(template.folder)
    if (
        normalizedFolder.isBlank() ||
        normalizedDraftPath == normalizedFolder ||
        normalizedDraftPath.startsWith("$normalizedFolder/")
    ) {
        return normalizedDraftPath
    }
    return "$normalizedFolder/$normalizedDraftPath"
}

internal fun buildMarkdownFromTemplate(
    pagePath: String,
    templatePage: ApiPageDetail,
): String {
    val normalizedPagePath = normalizePagePath(pagePath)
    val frontmatterLines = renderTemplateFrontmatterLines(
        pagePath = normalizedPagePath,
        frontmatter = templatePage.frontmatter,
    )
    val bodySource = splitMarkdownFrontmatter(templatePage.rawMarkdown).body.replace("\r\n", "\n")
    val body = replaceTemplatePlaceholders(bodySource, normalizedPagePath)
    val fallbackTitle = pageTitleFromPath(normalizedPagePath)
    val fallbackBody = fallbackTitle.takeIf(String::isNotBlank)?.let { "# $it\n" }.orEmpty()
    val content = if (body.isEmpty()) fallbackBody else body
    return (listOf("---") + frontmatterLines + "---").joinToString("\n") + "\n" + content
}

internal fun templateFieldSummary(template: NoteTemplate, limit: Int = 4): String {
    val keys = template.fieldKeys
        .map(String::trim)
        .filter(String::isNotBlank)
    if (keys.isEmpty()) {
        return "No predefined properties."
    }
    val maxItems = limit.coerceAtLeast(1)
    if (keys.size <= maxItems) {
        return keys.joinToString(" · ")
    }
    return keys.take(maxItems).joinToString(" · ") + " +${keys.size - maxItems}"
}

private fun noteTemplateFromPage(page: ApiPageSummary): NoteTemplate? {
    val info = templatePathInfo(page.path) ?: return null
    if (info.relativePath.isBlank()) {
        return null
    }

    val frontmatter = page.frontmatter
    val label = jsonElementStringValue(frontmatter[templateLabelKey])
        .ifBlank { page.title.trim() }
        .ifBlank { pageTitleFromPath(info.relativePath) }
    val folder = normalizePagePath(
        jsonElementStringValue(frontmatter[templateFolderKey])
            .ifBlank { defaultTemplateFolderFromPath(page.path) },
    )
    val fieldKeys = frontmatter.entries
        .map { it.key.trim() }
        .filter { key -> key.isNotBlank() && !isInternalTemplateMetadataKey(key) }

    return NoteTemplate(
        id = normalizePagePath(page.path),
        name = label,
        folder = folder,
        fieldKeys = fieldKeys,
    )
}

private fun replaceTemplatePlaceholders(value: String, pagePath: String): String {
    val title = pageTitleFromPath(pagePath)
    return value
        .replace(Regex("\\{\\{\\s*title\\s*}}", RegexOption.IGNORE_CASE), title)
        .replace(Regex("\\{\\{\\s*path\\s*}}", RegexOption.IGNORE_CASE), pagePath)
}

private fun templatePathInfo(pagePath: String): TemplatePathInfo? {
    val normalizedPath = normalizePagePath(pagePath)
    if (normalizedPath.isBlank()) {
        return null
    }
    if (normalizedPath.startsWith("$templateFolderName/")) {
        return TemplatePathInfo(
            scopePrefix = "",
            relativePath = normalizedPath.removePrefix("$templateFolderName/"),
        )
    }

    val parts = normalizedPath.split('/')
    if (parts.size >= 3 && parts[1] == templateFolderName) {
        return TemplatePathInfo(
            scopePrefix = parts[0],
            relativePath = parts.drop(2).joinToString("/"),
        )
    }
    return null
}

private fun defaultTemplateFolderFromPath(pagePath: String): String {
    val info = templatePathInfo(pagePath) ?: return ""
    val parts = info.relativePath.split('/').filter(String::isNotBlank)
    if (parts.size <= 1) {
        return ""
    }
    return normalizePagePath(parts.dropLast(1).joinToString("/"))
}

private fun templateScopeMatches(pagePath: String, scopePrefix: String): Boolean {
    val info = templatePathInfo(pagePath) ?: return false
    val normalizedScopePrefix = normalizePagePath(scopePrefix)
    if (normalizedScopePrefix.isBlank()) {
        return info.scopePrefix.isBlank()
    }
    return info.scopePrefix.isBlank() || info.scopePrefix == normalizedScopePrefix
}

private fun jsonElementStringValue(value: JsonElement?): String {
    return when (value) {
        is JsonPrimitive -> value.content.trim()
        else -> ""
    }
}

private fun jsonElementStringValues(value: JsonElement?): List<String> {
    return when (value) {
        is JsonArray -> value.mapNotNull { (it as? JsonPrimitive)?.content?.trim()?.takeIf(String::isNotBlank) }
        is JsonPrimitive -> listOf(value.content.trim()).filter(String::isNotBlank)
        else -> emptyList()
    }
}

private fun renderTemplateFrontmatterLines(
    pagePath: String,
    frontmatter: JsonObject,
): List<String> {
    val lines = mutableListOf<String>()
    renderTemplateKindMetadata(lines, frontmatter)
    frontmatter.entries.forEach { (key, value) ->
        val normalizedKey = key.trim()
        if (normalizedKey.isBlank() || isInternalTemplateMetadataKey(normalizedKey)) {
            return@forEach
        }
        renderFrontmatterEntry(
            lines = lines,
            key = normalizedKey,
            value = templateFieldDefaultValue(
                key = normalizedKey,
                value = value,
                pagePath = pagePath,
            ),
        )
    }
    return lines
}

private fun renderTemplateKindMetadata(lines: MutableList<String>, frontmatter: JsonObject) {
    frontmatterKindMetadataMappings.forEach { (metadataKey, aliases) ->
        val fieldKeys = aliases
            .flatMap { alias -> jsonElementStringValues(frontmatter[alias]) }
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct()
            .sortedBy(String::lowercase)
        if (fieldKeys.isNotEmpty()) {
            renderFrontmatterEntry(
                lines = lines,
                key = metadataKey,
                value = JsonArray(fieldKeys.map(::JsonPrimitive)),
            )
        }
    }
}

private fun templateFieldDefaultValue(
    key: String,
    value: JsonElement,
    pagePath: String,
): JsonElement {
    if (value is JsonNull) {
        return if (key.equals("title", ignoreCase = true)) {
            JsonPrimitive(pageTitleFromPath(pagePath))
        } else {
            JsonPrimitive("")
        }
    }

    if (value is JsonArray) {
        return JsonArray(
            value.map { entry ->
                val primitive = entry as? JsonPrimitive
                if (primitive?.isString == true) {
                    JsonPrimitive(replaceTemplatePlaceholders(primitive.content, pagePath).trim())
                } else {
                    entry
                }
            },
        )
    }

    val primitive = value as? JsonPrimitive ?: return value
    primitive.booleanOrNull?.let { return JsonPrimitive(it) }
    if (!primitive.isString) {
        return primitive
    }

    val replacedValue = replaceTemplatePlaceholders(primitive.content, pagePath)
    if (replacedValue.isBlank() && key.equals("title", ignoreCase = true)) {
        return JsonPrimitive(pageTitleFromPath(pagePath))
    }
    return JsonPrimitive(replacedValue)
}

private fun renderFrontmatterEntry(lines: MutableList<String>, key: String, value: JsonElement) {
    when (value) {
        is JsonArray -> {
            if (value.isEmpty()) {
                lines += "$key: []"
                return
            }
            lines += "$key:"
            value.forEach { entry ->
                lines += "  - ${renderFrontmatterScalar(entry)}"
            }
        }
        else -> lines += "$key: ${renderFrontmatterScalar(value)}"
    }
}

private fun renderFrontmatterScalar(value: JsonElement): String {
    return when (value) {
        is JsonNull -> "\"\""
        is JsonPrimitive -> value.toString()
        else -> value.toString()
    }
}
