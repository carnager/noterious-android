package dev.carnager.noterious.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import dev.carnager.noterious.data.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

private const val MarkdownLinkAnnotation = "markdown-link"

data class MarkdownImageTarget(
    val alt: String,
    val target: String,
)

internal data class DownloadedContent(
    val bytes: ByteArray,
    val contentType: String?,
)

@Composable
fun MarkdownContent(
    markdown: String,
    currentPagePath: String,
    settings: AppSettings,
    modifier: Modifier = Modifier,
    hideQueryFences: Boolean = false,
    onLinkClick: ((String) -> Unit)? = null,
    onImageClick: ((MarkdownImageTarget) -> Unit)? = null,
) {
    val blocks = remember(markdown, hideQueryFences) {
        parseMarkdownBlocks(stripFrontmatter(markdown), hideQueryFences = hideQueryFences)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Heading -> MarkdownHeading(block, onLinkClick)
                is MarkdownBlock.Paragraph -> MarkdownParagraph(block, onLinkClick)
                is MarkdownBlock.BulletItem -> MarkdownBulletItem(block, onLinkClick)
                is MarkdownBlock.NumberedItem -> MarkdownNumberedItem(block, onLinkClick)
                is MarkdownBlock.TaskItem -> MarkdownTaskItem(block, onLinkClick)
                is MarkdownBlock.BlockQuote -> MarkdownBlockQuote(block, onLinkClick)
                is MarkdownBlock.CodeFence -> MarkdownCodeFence(block)
                is MarkdownBlock.Table -> MarkdownTableBlock(block, onLinkClick)
                is MarkdownBlock.Image -> MarkdownImageBlock(
                    block = block,
                    currentPagePath = currentPagePath,
                    settings = settings,
                    onLinkClick = onLinkClick,
                    onImageClick = onImageClick,
                )
            }
        }
    }
}

@Composable
private fun MarkdownHeading(block: MarkdownBlock.Heading, onLinkClick: ((String) -> Unit)?) {
    val linkColor = MaterialTheme.colorScheme.primary
    val style = when (block.level) {
        1 -> MaterialTheme.typography.headlineSmall
        2 -> MaterialTheme.typography.titleLarge
        3 -> MaterialTheme.typography.titleMedium
        else -> MaterialTheme.typography.titleSmall
    }
    MarkdownText(
        text = parseInlineMarkdown(block.text, linkColor),
        style = style,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.SemiBold,
        onLinkClick = onLinkClick,
    )
}

@Composable
private fun MarkdownParagraph(block: MarkdownBlock.Paragraph, onLinkClick: ((String) -> Unit)?) {
    val linkColor = MaterialTheme.colorScheme.primary
    MarkdownText(
        text = parseInlineMarkdown(block.text, linkColor),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        onLinkClick = onLinkClick,
    )
}

@Composable
private fun MarkdownBulletItem(block: MarkdownBlock.BulletItem, onLinkClick: ((String) -> Unit)?) {
    val linkColor = MaterialTheme.colorScheme.primary
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "\u2022",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.Bold,
        )
        MarkdownText(
            text = parseInlineMarkdown(block.text, linkColor),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            onLinkClick = onLinkClick,
        )
    }
}

@Composable
private fun MarkdownNumberedItem(block: MarkdownBlock.NumberedItem, onLinkClick: ((String) -> Unit)?) {
    val linkColor = MaterialTheme.colorScheme.primary
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "${block.number}.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.SemiBold,
        )
        MarkdownText(
            text = parseInlineMarkdown(block.text, linkColor),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            onLinkClick = onLinkClick,
        )
    }
}

@Composable
private fun MarkdownTaskItem(block: MarkdownBlock.TaskItem, onLinkClick: ((String) -> Unit)?) {
    val linkColor = MaterialTheme.colorScheme.primary
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        androidx.compose.material3.Icon(
            if (block.checked) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (block.checked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline,
        )
        MarkdownText(
            text = parseInlineMarkdown(block.text, linkColor),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            onLinkClick = onLinkClick,
        )
    }
}

@Composable
private fun MarkdownBlockQuote(block: MarkdownBlock.BlockQuote, onLinkClick: ((String) -> Unit)?) {
    val linkColor = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.16f),
                shape = RoundedCornerShape(24.dp),
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            text = "“",
            style = MaterialTheme.typography.displaySmall.copy(fontFamily = FontFamily.Serif),
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
            modifier = Modifier.align(Alignment.TopStart),
        )
        MarkdownText(
            text = parseInlineMarkdown(block.text, linkColor),
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = FontFamily.Serif,
                fontStyle = FontStyle.Italic,
                lineHeight = 30.sp,
            ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 26.dp, top = 8.dp, end = 22.dp, bottom = 14.dp),
            onLinkClick = onLinkClick,
        )
        Text(
            text = "”",
            style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Serif),
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
            modifier = Modifier.align(Alignment.BottomEnd),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 26.dp)
                .width(48.dp)
                .height(2.dp)
                .background(
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(999.dp),
                ),
        )
    }
}

@Composable
private fun MarkdownCodeFence(block: MarkdownBlock.CodeFence) {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                shape = RoundedCornerShape(14.dp),
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        text = block.text,
        style = MaterialTheme.typography.bodySmall,
        fontFamily = FontFamily.Monospace,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun MarkdownTableBlock(block: MarkdownBlock.Table, onLinkClick: ((String) -> Unit)?) {
    val linkColor = MaterialTheme.colorScheme.primary
    val columnCount = remember(block) {
        maxOf(block.headers.size, block.rows.maxOfOrNull { it.size } ?: 0)
    }
    val headers = remember(block, columnCount) {
        List(columnCount) { index ->
            block.headers.getOrElse(index) { "Column ${index + 1}" }.ifBlank { "Column ${index + 1}" }
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
                    headers.forEach { header ->
                        Text(
                            text = header,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .width(160.dp)
                                .padding(horizontal = 8.dp),
                        )
                    }
                }

                if (block.rows.isEmpty()) {
                    Row(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.65f))
                            .padding(vertical = 10.dp),
                    ) {
                        headers.forEachIndexed { index, _ ->
                            Text(
                                text = if (index == 0) "No rows" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .width(160.dp)
                                    .padding(horizontal = 8.dp),
                            )
                        }
                    }
                    return@Column
                }

                block.rows.forEachIndexed { rowIndex, row ->
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
                        headers.indices.forEach { index ->
                            MarkdownText(
                                text = parseInlineMarkdown(row.getOrElse(index) { "" }.ifBlank { "\u2014" }, linkColor),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .width(160.dp)
                                    .padding(horizontal = 8.dp),
                                onLinkClick = onLinkClick,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MarkdownImageBlock(
    block: MarkdownBlock.Image,
    currentPagePath: String,
    settings: AppSettings,
    onLinkClick: ((String) -> Unit)?,
    onImageClick: ((MarkdownImageTarget) -> Unit)?,
) {
    val request = remember(block.target, currentPagePath, settings.serverUrl, settings.bearerToken) {
        resolveImageRequest(
            target = block.target,
            currentPagePath = currentPagePath,
            settings = settings,
        )
    }
    val bitmap by produceState<ImageBitmap?>(initialValue = null, request) {
        value = request?.let { loadImageBitmap(it, settings.bearerToken) }
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap!!,
                    contentDescription = block.alt.ifBlank { pageTitleFromPath(block.target) },
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = onImageClick != null || onLinkClick != null) {
                            if (onImageClick != null) {
                                onImageClick(MarkdownImageTarget(alt = block.alt, target = block.target))
                            } else {
                                onLinkClick?.invoke(block.target)
                            }
                        },
                )
            } else {
                Text(
                    text = block.alt.ifBlank { pageTitleFromPath(block.target) },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Bild nicht verfuegbar",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (block.alt.isNotBlank()) {
                Text(
                    text = block.alt,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun MarkdownText(
    text: AnnotatedString,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight? = null,
    onLinkClick: ((String) -> Unit)? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    val effectiveStyle = if (fontWeight != null) {
        style.copy(color = color, fontWeight = fontWeight)
    } else {
        style.copy(color = color)
    }
    val hasLinks = text.getStringAnnotations(
        tag = MarkdownLinkAnnotation,
        start = 0,
        end = text.length,
    ).isNotEmpty()

    if (hasLinks && onLinkClick != null) {
        ClickableText(
            text = text,
            modifier = modifier,
            style = effectiveStyle,
            maxLines = maxLines,
            overflow = overflow,
        ) { offset ->
            text.getStringAnnotations(
                tag = MarkdownLinkAnnotation,
                start = offset,
                end = offset,
            ).firstOrNull()?.let { annotation ->
                onLinkClick(annotation.item)
            }
        }
    } else {
        Text(
            text = text,
            style = effectiveStyle,
            modifier = modifier,
            maxLines = maxLines,
            overflow = overflow,
        )
    }
}

private sealed interface MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock
    data class Paragraph(val text: String) : MarkdownBlock
    data class BulletItem(val text: String) : MarkdownBlock
    data class NumberedItem(val number: Int, val text: String) : MarkdownBlock
    data class TaskItem(val checked: Boolean, val text: String) : MarkdownBlock
    data class BlockQuote(val text: String) : MarkdownBlock
    data class CodeFence(val text: String, val language: String = "") : MarkdownBlock
    data class Table(val headers: List<String>, val rows: List<List<String>>) : MarkdownBlock
    data class Image(val alt: String, val target: String) : MarkdownBlock
}

private data class ParsedTableBlock(
    val headers: List<String>,
    val rows: List<List<String>>,
    val endLineIndex: Int,
)

internal data class ImageRequestSpec(
    val url: String,
    val includeAuthorization: Boolean,
)

private fun stripFrontmatter(markdown: String): String {
    if (!markdown.startsWith("---\n")) return markdown
    val end = markdown.indexOf("\n---\n", startIndex = 4)
    if (end == -1) return markdown
    return markdown.substring(end + 5).trimStart()
}

private fun parseMarkdownBlocks(markdown: String, hideQueryFences: Boolean): List<MarkdownBlock> {
    val lines = markdown.replace("\r\n", "\n").split('\n')
    val blocks = mutableListOf<MarkdownBlock>()
    val paragraph = mutableListOf<String>()
    val blockQuote = mutableListOf<String>()
    val codeFence = mutableListOf<String>()
    var inCodeFence = false
    var codeFenceMarker = "```"
    var codeFenceInfo = ""

    fun flushParagraph() {
        if (paragraph.isEmpty()) return
        val text = paragraph.joinToString("\n").trimEnd()
        paragraph.clear()
        standaloneImageMatch(text)?.let { image ->
            blocks += MarkdownBlock.Image(alt = image.alt, target = image.target)
            return
        }
        blocks += MarkdownBlock.Paragraph(text)
    }

    fun flushBlockQuote() {
        if (blockQuote.isEmpty()) return
        blocks += MarkdownBlock.BlockQuote(blockQuote.joinToString("\n").trimEnd())
        blockQuote.clear()
    }

    var index = 0
    while (index < lines.size) {
        val rawLine = lines[index].trimEnd()
        val trimmedLine = rawLine.trim()

        if (inCodeFence) {
            val fencePattern = Regex("^${Regex.escape(codeFenceMarker)}\\s*$")
            if (fencePattern.matches(trimmedLine)) {
                val language = codeFenceInfo.substringBefore(' ').trim()
                if (!(hideQueryFences && language.equals("query", ignoreCase = true))) {
                    blocks += MarkdownBlock.CodeFence(
                        text = codeFence.joinToString("\n").trimEnd(),
                        language = language,
                    )
                }
                codeFence.clear()
                codeFenceInfo = ""
                inCodeFence = false
                index += 1
                continue
            }

            codeFence += rawLine
            index += 1
            continue
        }

        val codeFenceMatch = Regex("^(```+)(.*)$").matchEntire(trimmedLine)
        if (codeFenceMatch != null) {
            flushParagraph()
            flushBlockQuote()
            inCodeFence = true
            codeFenceMarker = codeFenceMatch.groupValues[1]
            codeFenceInfo = codeFenceMatch.groupValues[2].trim()
            index += 1
            continue
        }

        if (trimmedLine.isBlank()) {
            flushParagraph()
            flushBlockQuote()
            index += 1
            continue
        }

        val table = markdownTableBlockAt(lines, index)
        if (table != null) {
            flushParagraph()
            flushBlockQuote()
            blocks += MarkdownBlock.Table(headers = table.headers, rows = table.rows)
            index = table.endLineIndex + 1
            continue
        }

        if (trimmedLine.startsWith(">")) {
            flushParagraph()
            blockQuote += trimmedLine.removePrefix(">").trimStart()
            index += 1
            continue
        }
        flushBlockQuote()

        headingMatch(trimmedLine)?.let { (level, text) ->
            flushParagraph()
            blocks += MarkdownBlock.Heading(level, text)
            index += 1
            continue
        }

        taskMatch(trimmedLine)?.let { (checked, text) ->
            flushParagraph()
            blocks += MarkdownBlock.TaskItem(checked = checked, text = text)
            index += 1
            continue
        }

        bulletMatch(trimmedLine)?.let { text ->
            flushParagraph()
            blocks += MarkdownBlock.BulletItem(text)
            index += 1
            continue
        }

        numberedMatch(trimmedLine)?.let { (number, text) ->
            flushParagraph()
            blocks += MarkdownBlock.NumberedItem(number = number, text = text)
            index += 1
            continue
        }

        paragraph += trimmedLine
        index += 1
    }

    flushParagraph()
    flushBlockQuote()

    if (codeFence.isNotEmpty() && !(hideQueryFences && codeFenceInfo.startsWith("query", ignoreCase = true))) {
        blocks += MarkdownBlock.CodeFence(
            text = codeFence.joinToString("\n").trimEnd(),
            language = codeFenceInfo.substringBefore(' ').trim(),
        )
    }

    return blocks
}

private fun headingMatch(line: String): Pair<Int, String>? {
    val hashes = line.takeWhile { it == '#' }
    if (hashes.isEmpty() || hashes.length > 6) return null
    val text = line.drop(hashes.length).trim()
    if (text.isBlank()) return null
    return hashes.length to text
}

private fun taskMatch(line: String): Pair<Boolean, String>? {
    val match = Regex("""^[-*+]\s+\[([ xX])]\s+(.*)$""").matchEntire(line) ?: return null
    return (match.groupValues[1].equals("x", ignoreCase = true)) to match.groupValues[2].trim()
}

private fun bulletMatch(line: String): String? {
    return when {
        line.startsWith("- ") -> line.removePrefix("- ").trim()
        line.startsWith("* ") -> line.removePrefix("* ").trim()
        line.startsWith("+ ") -> line.removePrefix("+ ").trim()
        else -> null
    }
}

private fun numberedMatch(line: String): Pair<Int, String>? {
    val dotIndex = line.indexOf(". ")
    if (dotIndex <= 0) return null
    val number = line.substring(0, dotIndex).toIntOrNull() ?: return null
    return number to line.substring(dotIndex + 2).trim()
}

private fun splitMarkdownTableRow(line: String): List<String> {
    return line
        .trim()
        .removePrefix("|")
        .removeSuffix("|")
        .split("|")
        .map(String::trim)
}

private fun looksLikeMarkdownTableRow(line: String): Boolean {
    return line.contains('|') && splitMarkdownTableRow(line).size >= 2
}

private fun isMarkdownTableSeparator(line: String): Boolean {
    val cells = splitMarkdownTableRow(line)
    return cells.isNotEmpty() && cells.all { cell ->
        Regex("^:?-{3,}:?$").matches(cell)
    }
}

private fun markdownTableBlockAt(lines: List<String>, startLineIndex: Int): ParsedTableBlock? {
    if (startLineIndex + 1 >= lines.size) return null

    val headerLine = lines[startLineIndex].trim()
    val separatorLine = lines[startLineIndex + 1].trim()
    if (!looksLikeMarkdownTableRow(headerLine) || !isMarkdownTableSeparator(separatorLine)) {
        return null
    }

    val headers = splitMarkdownTableRow(headerLine)
    if (headers.size < 2) return null

    val rows = mutableListOf<List<String>>()
    var endLineIndex = startLineIndex + 1

    var index = startLineIndex + 2
    while (index < lines.size) {
        val candidate = lines[index].trim()
        if (!looksLikeMarkdownTableRow(candidate)) {
            break
        }
        rows += splitMarkdownTableRow(candidate)
        endLineIndex = index
        index += 1
    }

    return ParsedTableBlock(
        headers = headers,
        rows = rows,
        endLineIndex = endLineIndex,
    )
}

private data class StandaloneImageMatch(
    val alt: String,
    val target: String,
)

private fun standaloneImageMatch(text: String): StandaloneImageMatch? {
    val trimmed = text.trim()
    val wikiMatch = Regex("""^!\[\[([^\]|]+)(?:\|([^\]]+))?]]$""").matchEntire(trimmed)
    if (wikiMatch != null) {
        val target = wikiMatch.groupValues[1].trim()
        val label = wikiMatch.groupValues.getOrNull(2).orEmpty().trim()
        return StandaloneImageMatch(
            alt = label.ifBlank { pageTitleFromPath(target) },
            target = target,
        )
    }

    val markdownMatch = Regex("""^!\[([^\]]*)]\(([^)\s]+)\)$""").matchEntire(trimmed)
    if (markdownMatch != null) {
        val alt = markdownMatch.groupValues[1].trim()
        val target = markdownMatch.groupValues[2].trim()
        return StandaloneImageMatch(
            alt = alt.ifBlank { pageTitleFromPath(target) },
            target = target,
        )
    }

    return null
}

internal fun parseInlineMarkdown(text: String, linkColor: Color): AnnotatedString {
    return buildAnnotatedString {
        appendInlineMarkdown(text, linkColor)
    }
}

private fun AnnotatedString.Builder.appendInlineMarkdown(text: String, linkColor: Color) {
    var index = 0

    while (index < text.length) {
        when {
            text.startsWith("![[", index) -> {
                val end = text.indexOf("]]", startIndex = index + 3)
                if (end > index + 3) {
                    val body = text.substring(index + 3, end)
                    val target = body.substringBefore('|').trim()
                    val label = body.substringAfter('|', pageTitleFromPath(target)).trim().ifBlank {
                        pageTitleFromPath(target)
                    }
                    appendLink("[Image: $label]", target, linkColor)
                    index = end + 2
                } else {
                    append(text[index])
                    index += 1
                }
            }
            text.startsWith("![", index) -> {
                val close = text.indexOf("]", startIndex = index + 2)
                val openParen = if (close != -1) text.indexOf("(", startIndex = close + 1) else -1
                val closeParen = if (openParen != -1) text.indexOf(")", startIndex = openParen + 1) else -1
                if (close > index + 1 && openParen == close + 1 && closeParen > openParen + 1) {
                    val alt = text.substring(index + 2, close).trim()
                    val target = text.substring(openParen + 1, closeParen).trim()
                    appendLink("[Image: ${alt.ifBlank { pageTitleFromPath(target) }}]", target, linkColor)
                    index = closeParen + 1
                } else {
                    append(text[index])
                    index += 1
                }
            }
            text.startsWith("**", index) || text.startsWith("__", index) -> {
                val marker = text.substring(index, index + 2)
                val end = text.indexOf(marker, startIndex = index + 2)
                if (end > index + 2) {
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    appendInlineMarkdown(text.substring(index + 2, end), linkColor)
                    pop()
                    index = end + 2
                } else {
                    append(text[index])
                    index += 1
                }
            }
            text.startsWith("~~", index) -> {
                val end = text.indexOf("~~", startIndex = index + 2)
                if (end > index + 2) {
                    pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                    appendInlineMarkdown(text.substring(index + 2, end), linkColor)
                    pop()
                    index = end + 2
                } else {
                    append(text[index])
                    index += 1
                }
            }
            text.startsWith("*", index) || text.startsWith("_", index) -> {
                val marker = text[index]
                val end = text.indexOf(marker, startIndex = index + 1)
                if (end > index + 1) {
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    appendInlineMarkdown(text.substring(index + 1, end), linkColor)
                    pop()
                    index = end + 1
                } else {
                    append(text[index])
                    index += 1
                }
            }
            text.startsWith("`", index) -> {
                val end = text.indexOf("`", startIndex = index + 1)
                if (end > index + 1) {
                    pushStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = Color(0x1A7A6F66),
                        ),
                    )
                    append(text.substring(index + 1, end))
                    pop()
                    index = end + 1
                } else {
                    append(text[index])
                    index += 1
                }
            }
            text.startsWith("[[", index) -> {
                val end = text.indexOf("]]", startIndex = index + 2)
                if (end > index + 2) {
                    val body = text.substring(index + 2, end)
                    val target = body.substringBefore('|').trim()
                    val label = body.substringAfter('|', pageTitleFromPath(target)).trim().ifBlank {
                        pageTitleFromPath(target)
                    }
                    appendLink(label, target, linkColor)
                    index = end + 2
                } else {
                    append(text[index])
                    index += 1
                }
            }
            text.startsWith("[", index) -> {
                val close = text.indexOf("]", startIndex = index + 1)
                val openParen = if (close != -1) text.indexOf("(", startIndex = close + 1) else -1
                val closeParen = if (openParen != -1) text.indexOf(")", startIndex = openParen + 1) else -1
                if (close > index + 1 && openParen == close + 1 && closeParen > openParen + 1) {
                    val label = text.substring(index + 1, close)
                    val target = text.substring(openParen + 1, closeParen).trim()
                    pushStringAnnotation(tag = MarkdownLinkAnnotation, annotation = target)
                    pushStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline))
                    appendInlineMarkdown(label, linkColor)
                    pop()
                    pop()
                    index = closeParen + 1
                } else {
                    append(text[index])
                    index += 1
                }
            }
            else -> {
                append(text[index])
                index += 1
            }
        }
    }
}

private fun AnnotatedString.Builder.appendLink(label: String, target: String, linkColor: Color) {
    pushStringAnnotation(tag = MarkdownLinkAnnotation, annotation = target)
    pushStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline))
    append(label)
    pop()
    pop()
}

internal fun resolveImageRequest(
    target: String,
    currentPagePath: String,
    settings: AppSettings,
): ImageRequestSpec? {
    val trimmedTarget = target.trim()
    if (trimmedTarget.isBlank()) return null

    if (Regex("^https?://", RegexOption.IGNORE_CASE).containsMatchIn(trimmedTarget)) {
        return ImageRequestSpec(url = trimmedTarget, includeAuthorization = false)
    }

    val normalizedBaseUrl = normalizeServerBaseUrl(settings.serverUrl)
    if (normalizedBaseUrl.isBlank()) return null

    if (trimmedTarget.startsWith("/")) {
        return ImageRequestSpec(
            url = "$normalizedBaseUrl$trimmedTarget",
            includeAuthorization = true,
        )
    }

    val resolvedPath = resolveRelativePath(currentPagePath, trimmedTarget)
    if (!isImagePath(resolvedPath)) return null

    return ImageRequestSpec(
        url = documentDownloadUrl(normalizedBaseUrl, resolvedPath, inline = true),
        includeAuthorization = true,
    )
}

internal suspend fun downloadContent(
    request: ImageRequestSpec,
    bearerToken: String,
): DownloadedContent? {
    return withContext(Dispatchers.IO) {
        val connection = (URL(request.url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
            if (request.includeAuthorization && bearerToken.isNotBlank()) {
                setRequestProperty("Authorization", "Bearer $bearerToken")
            }
        }

        try {
            if (connection.responseCode !in 200..299) {
                return@withContext null
            }
            val contentType = connection.contentType
            val bytes = connection.inputStream.use { stream -> stream.readBytes() }
            DownloadedContent(
                bytes = bytes,
                contentType = contentType,
            )
        } finally {
            connection.disconnect()
        }
    }
}

private suspend fun loadImageBitmap(request: ImageRequestSpec, bearerToken: String): ImageBitmap? {
    val content = downloadContent(request, bearerToken) ?: return null
    return withContext(Dispatchers.IO) {
        BitmapFactory.decodeByteArray(content.bytes, 0, content.bytes.size)?.asImageBitmap()
    }
}
