package dev.carnager.noterious.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
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
    linkDefinitions: Map<String, String>? = null,
    onLinkClick: ((String) -> Unit)? = null,
    onTextClick: (() -> Unit)? = null,
    onImageClick: ((MarkdownImageTarget) -> Unit)? = null,
    onTaskToggle: ((lineIndex: Int, checked: Boolean) -> Unit)? = null,
    onTaskDueDateClick: ((lineIndex: Int, currentDue: String?) -> Unit)? = null,
    onTaskReminderClick: ((lineIndex: Int, currentRemind: String?) -> Unit)? = null,
) {
    val effectiveLinkDefinitions = remember(markdown, linkDefinitions) {
        linkDefinitions ?: extractMarkdownReferenceDefinitions(stripFrontmatter(markdown))
    }
    val blocks = remember(markdown, hideQueryFences) {
        parseMarkdownBlocks(stripFrontmatter(markdown), hideQueryFences = hideQueryFences)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Heading -> MarkdownHeading(block, effectiveLinkDefinitions, onLinkClick, onTextClick)
                is MarkdownBlock.Paragraph -> MarkdownParagraph(block, effectiveLinkDefinitions, onLinkClick, onTextClick)
                is MarkdownBlock.BulletItem -> MarkdownBulletItem(block, effectiveLinkDefinitions, onLinkClick, onTextClick)
                is MarkdownBlock.NumberedItem -> MarkdownNumberedItem(block, effectiveLinkDefinitions, onLinkClick, onTextClick)
                is MarkdownBlock.TaskItem -> MarkdownTaskItem(
                    block = block,
                    linkDefinitions = effectiveLinkDefinitions,
                    onLinkClick = onLinkClick,
                    onTextClick = onTextClick,
                    onToggle = onTaskToggle,
                    onDueDateClick = onTaskDueDateClick,
                    onReminderClick = onTaskReminderClick,
                )
                is MarkdownBlock.BlockQuote -> MarkdownBlockQuote(
                    block = block,
                    currentPagePath = currentPagePath,
                    settings = settings,
                    linkDefinitions = effectiveLinkDefinitions,
                    onLinkClick = onLinkClick,
                    onTextClick = onTextClick,
                    onImageClick = onImageClick,
                )
                is MarkdownBlock.CodeFence -> MarkdownCodeFence(block)
                is MarkdownBlock.Table -> MarkdownTableBlock(block, effectiveLinkDefinitions, onLinkClick, onTextClick)
                is MarkdownBlock.Image -> MarkdownImageBlock(
                    block = block,
                    currentPagePath = currentPagePath,
                    settings = settings,
                    onLinkClick = onLinkClick,
                    onImageClick = onImageClick,
                )
                is MarkdownBlock.Details -> MarkdownDetailsBlock(
                    block = block,
                    currentPagePath = currentPagePath,
                    settings = settings,
                    linkDefinitions = effectiveLinkDefinitions,
                    onLinkClick = onLinkClick,
                    onTextClick = onTextClick,
                    onImageClick = onImageClick,
                )
                is MarkdownBlock.FootnoteDefinitions -> MarkdownFootnoteDefinitions(
                    block = block,
                    linkDefinitions = effectiveLinkDefinitions,
                    onLinkClick = onLinkClick,
                    onTextClick = onTextClick,
                )
                is MarkdownBlock.DefinitionList -> MarkdownDefinitionList(
                    title = "Definitions",
                    block = block,
                    linkDefinitions = effectiveLinkDefinitions,
                    onLinkClick = onLinkClick,
                    onTextClick = onTextClick,
                )
                is MarkdownBlock.AbbreviationList -> MarkdownAbbreviationList(
                    block = block,
                    linkDefinitions = effectiveLinkDefinitions,
                    onLinkClick = onLinkClick,
                    onTextClick = onTextClick,
                )
            }
        }
    }
}

@Composable
private fun MarkdownHeading(
    block: MarkdownBlock.Heading,
    linkDefinitions: Map<String, String>,
    onLinkClick: ((String) -> Unit)?,
    onTextClick: (() -> Unit)?,
) {
    val linkColor = MaterialTheme.colorScheme.primary
    val style = when (block.level) {
        1 -> MaterialTheme.typography.headlineSmall
        2 -> MaterialTheme.typography.titleLarge
        3 -> MaterialTheme.typography.titleMedium
        else -> MaterialTheme.typography.titleSmall
    }
    MarkdownText(
        text = parseInlineMarkdown(block.text, linkColor, linkDefinitions),
        style = style,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.SemiBold,
        onLinkClick = onLinkClick,
        onTextClick = onTextClick,
    )
}

@Composable
private fun MarkdownParagraph(
    block: MarkdownBlock.Paragraph,
    linkDefinitions: Map<String, String>,
    onLinkClick: ((String) -> Unit)?,
    onTextClick: (() -> Unit)?,
) {
    val linkColor = MaterialTheme.colorScheme.primary
    MarkdownText(
        text = parseInlineMarkdown(block.text, linkColor, linkDefinitions),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        onLinkClick = onLinkClick,
        onTextClick = onTextClick,
    )
}

@Composable
private fun MarkdownBulletItem(
    block: MarkdownBlock.BulletItem,
    linkDefinitions: Map<String, String>,
    onLinkClick: ((String) -> Unit)?,
    onTextClick: (() -> Unit)?,
) {
    val linkColor = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = markdownListStartPadding(block.indent)),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "\u2022",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.Bold,
        )
        MarkdownText(
            text = parseInlineMarkdown(block.text, linkColor, linkDefinitions),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            onLinkClick = onLinkClick,
            onTextClick = onTextClick,
        )
    }
}

@Composable
private fun MarkdownNumberedItem(
    block: MarkdownBlock.NumberedItem,
    linkDefinitions: Map<String, String>,
    onLinkClick: ((String) -> Unit)?,
    onTextClick: (() -> Unit)?,
) {
    val linkColor = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = markdownListStartPadding(block.indent)),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "${block.number}.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.SemiBold,
        )
        MarkdownText(
            text = parseInlineMarkdown(block.text, linkColor, linkDefinitions),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            onLinkClick = onLinkClick,
            onTextClick = onTextClick,
        )
    }
}

private val taskBracketFieldPattern = Regex("\\[(due|remind|who|click|completed):\\s*[^\\]]*\\]", RegexOption.IGNORE_CASE)
private val taskInlineFieldPattern = Regex("\\b(due|remind|who|click)::\\s*.*?(?=(\\s+\\b(due|remind|who|click)::)|$)", RegexOption.IGNORE_CASE)
private val taskRemindTagPattern = Regex("(^|\\s)#remind\\b", RegexOption.IGNORE_CASE)

private fun stripTaskFields(text: String): String {
    return text
        .replace(taskBracketFieldPattern, " ")
        .replace(taskInlineFieldPattern, " ")
        .replace(taskRemindTagPattern, " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}

private fun extractTaskField(text: String, field: String): String? {
    val bracketMatch = Regex("\\[$field:\\s*([^\\]]*)\\]", RegexOption.IGNORE_CASE).find(text)
    if (bracketMatch != null) return bracketMatch.groupValues[1].trim().takeIf(String::isNotBlank)
    val inlineMatch = Regex("\\b$field::\\s*(\\S+)", RegexOption.IGNORE_CASE).find(text)
    return inlineMatch?.groupValues?.get(1)?.trim()?.takeIf(String::isNotBlank)
}

@Composable
private fun MarkdownTaskItem(
    block: MarkdownBlock.TaskItem,
    linkDefinitions: Map<String, String>,
    onLinkClick: ((String) -> Unit)?,
    onTextClick: (() -> Unit)?,
    onToggle: ((lineIndex: Int, checked: Boolean) -> Unit)? = null,
    onDueDateClick: ((lineIndex: Int, currentDue: String?) -> Unit)? = null,
    onReminderClick: ((lineIndex: Int, currentRemind: String?) -> Unit)? = null,
) {
    val linkColor = MaterialTheme.colorScheme.primary
    val displayText = remember(block.text) { stripTaskFields(block.text) }
    val dueDate = remember(block.text) { extractTaskField(block.text, "due") }
    val reminder = remember(block.text) { extractTaskField(block.text, "remind") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = markdownListStartPadding(block.indent)),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                if (block.checked) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = if (block.checked) "Done" else "Todo",
                tint = if (block.checked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline,
                modifier = if (onToggle != null) {
                    Modifier.clickable { onToggle(block.lineIndex, !block.checked) }
                } else {
                    Modifier
                },
            )
            MarkdownText(
                text = parseInlineMarkdown(
                    if (block.checked) "~~$displayText~~" else displayText,
                    linkColor,
                    linkDefinitions,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = if (block.checked) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.weight(1f),
                onLinkClick = onLinkClick,
                onTextClick = onTextClick,
            )
        }
        if (dueDate != null || reminder != null) {
            Row(
                modifier = Modifier.padding(start = 32.dp, top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (dueDate != null) {
                    AssistChip(
                        onClick = { onDueDateClick?.invoke(block.lineIndex, dueDate) },
                        label = { Text("Due $dueDate", style = MaterialTheme.typography.labelSmall) },
                    )
                }
                if (reminder != null) {
                    AssistChip(
                        onClick = { onReminderClick?.invoke(block.lineIndex, reminder) },
                        label = { Text("\u23F0 $reminder", style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }
        }
    }
}

@Composable
private fun MarkdownBlockQuote(
    block: MarkdownBlock.BlockQuote,
    currentPagePath: String,
    settings: AppSettings,
    linkDefinitions: Map<String, String>,
    onLinkClick: ((String) -> Unit)?,
    onTextClick: (() -> Unit)?,
    onImageClick: ((MarkdownImageTarget) -> Unit)?,
) {
    val callout = remember(block.text) { parseMarkdownCallout(block.text) }
    val containerColor = when (callout?.type) {
        MarkdownCalloutType.Note -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        MarkdownCalloutType.Tip -> Color(0x1A2E7D32)
        MarkdownCalloutType.Important -> Color(0x1A1565C0)
        MarkdownCalloutType.Warning -> Color(0x1AF57C00)
        MarkdownCalloutType.Caution -> Color(0x1AB71C1C)
        null -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f)
    }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
        ),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = callout?.type?.label ?: "Quote",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = callout?.type?.accentColor ?: MaterialTheme.colorScheme.secondary,
            )
            MarkdownContent(
                markdown = callout?.body ?: block.text,
                currentPagePath = currentPagePath,
                settings = settings,
                modifier = Modifier.fillMaxWidth(),
                linkDefinitions = linkDefinitions,
                onLinkClick = onLinkClick,
                onTextClick = onTextClick,
                onImageClick = onImageClick,
            )
        }
    }
}

@Composable
private fun MarkdownCodeFence(block: MarkdownBlock.CodeFence) {
    var expanded by remember(block.language, block.text) { mutableStateOf(false) }
    val previewText = remember(block.text) {
        val trimmed = block.text.trimEnd()
        if (trimmed.isBlank()) {
            "Empty code block"
        } else {
            trimmed
        }
    }
    val lineCount = remember(block.text) {
        block.text.lines().size.coerceAtLeast(1)
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    if (block.language.isNotBlank()) {
                        Text(
                            text = block.language,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Text(
                        text = if (expanded) {
                            "$lineCount lines"
                        } else {
                            "$lineCount lines · collapsed"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (block.text.isNotBlank()) {
                    TextButton(onClick = { expanded = !expanded }) {
                        Text(if (expanded) "Hide code" else "Show code")
                    }
                }
            }
            if (expanded) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState())
                        .horizontalScroll(rememberScrollState()),
                    text = previewText,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface,
                    softWrap = false,
                )
            } else {
                Text(
                    text = previewText,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun MarkdownTableBlock(
    block: MarkdownBlock.Table,
    linkDefinitions: Map<String, String>,
    onLinkClick: ((String) -> Unit)?,
    onTextClick: (() -> Unit)?,
) {
    val linkColor = MaterialTheme.colorScheme.primary
    val scrollState = rememberScrollState()
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
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (columnCount > 2 || scrollState.maxValue > 0) {
                Text(
                    text = "Swipe for more columns ->",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(
                modifier = Modifier.horizontalScroll(scrollState),
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
                                text = parseInlineMarkdown(
                                    row.getOrElse(index) { "" }.ifBlank { "\u2014" },
                                    linkColor,
                                    linkDefinitions,
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .width(160.dp)
                                    .padding(horizontal = 8.dp),
                                onLinkClick = onLinkClick,
                                onTextClick = onTextClick,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MarkdownDetailsBlock(
    block: MarkdownBlock.Details,
    currentPagePath: String,
    settings: AppSettings,
    linkDefinitions: Map<String, String>,
    onLinkClick: ((String) -> Unit)?,
    onTextClick: (() -> Unit)?,
    onImageClick: ((MarkdownImageTarget) -> Unit)?,
) {
    var expanded by remember(block.summary, block.body) { mutableStateOf(false) }
    val linkColor = MaterialTheme.colorScheme.primary
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MarkdownText(
                    text = parseInlineMarkdown(block.summary, linkColor, linkDefinitions),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    onLinkClick = onLinkClick,
                    onTextClick = onTextClick,
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse details" else "Expand details",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (expanded) {
                MarkdownContent(
                    markdown = block.body,
                    currentPagePath = currentPagePath,
                    settings = settings,
                    modifier = Modifier.fillMaxWidth(),
                    linkDefinitions = linkDefinitions,
                    onLinkClick = onLinkClick,
                    onTextClick = onTextClick,
                    onImageClick = onImageClick,
                )
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
private fun MarkdownFootnoteDefinitions(
    block: MarkdownBlock.FootnoteDefinitions,
    linkDefinitions: Map<String, String>,
    onLinkClick: ((String) -> Unit)?,
    onTextClick: (() -> Unit)?,
) {
    val linkColor = MaterialTheme.colorScheme.primary
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Footnotes",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.secondary,
            )
            block.items.forEach { item ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "[${item.label}]",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    MarkdownText(
                        text = parseInlineMarkdown(item.text, linkColor, linkDefinitions),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                        onLinkClick = onLinkClick,
                        onTextClick = onTextClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun MarkdownDefinitionList(
    title: String,
    block: MarkdownBlock.DefinitionList,
    linkDefinitions: Map<String, String>,
    onLinkClick: ((String) -> Unit)?,
    onTextClick: (() -> Unit)?,
) {
    val linkColor = MaterialTheme.colorScheme.primary
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.secondary,
            )
            block.items.forEach { item ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    MarkdownText(
                        text = parseInlineMarkdown(item.term, linkColor, linkDefinitions),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        onLinkClick = onLinkClick,
                        onTextClick = onTextClick,
                    )
                    item.definitions.forEach { definition ->
                        MarkdownText(
                            text = parseInlineMarkdown(definition, linkColor, linkDefinitions),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 12.dp),
                            onLinkClick = onLinkClick,
                            onTextClick = onTextClick,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MarkdownAbbreviationList(
    block: MarkdownBlock.AbbreviationList,
    linkDefinitions: Map<String, String>,
    onLinkClick: ((String) -> Unit)?,
    onTextClick: (() -> Unit)?,
) {
    MarkdownDefinitionList(
        title = "Glossary",
        block = MarkdownBlock.DefinitionList(
            items = block.items.map { item ->
                MarkdownDefinitionListItem(
                    term = item.abbreviation,
                    definitions = listOf(item.expansion),
                )
            },
        ),
        linkDefinitions = linkDefinitions,
        onLinkClick = onLinkClick,
        onTextClick = onTextClick,
    )
}

@Composable
internal fun MarkdownText(
    text: AnnotatedString,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight? = null,
    onLinkClick: ((String) -> Unit)? = null,
    onTextClick: (() -> Unit)? = null,
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

    if ((hasLinks && onLinkClick != null) || onTextClick != null) {
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
                onLinkClick?.invoke(annotation.item)
            } ?: onTextClick?.invoke()
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
    data class BulletItem(val text: String, val indent: String = "") : MarkdownBlock
    data class NumberedItem(val number: Int, val text: String, val indent: String = "") : MarkdownBlock
    data class TaskItem(val checked: Boolean, val text: String, val indent: String = "", val lineIndex: Int = 0) : MarkdownBlock
    data class BlockQuote(val text: String) : MarkdownBlock
    data class CodeFence(val text: String, val language: String = "") : MarkdownBlock
    data class Table(val headers: List<String>, val rows: List<List<String>>) : MarkdownBlock
    data class Image(val alt: String, val target: String) : MarkdownBlock
    data class Details(val summary: String, val body: String) : MarkdownBlock
    data class FootnoteDefinitions(val items: List<MarkdownFootnoteDefinition>) : MarkdownBlock
    data class DefinitionList(val items: List<MarkdownDefinitionListItem>) : MarkdownBlock
    data class AbbreviationList(val items: List<MarkdownAbbreviationDefinition>) : MarkdownBlock
}

private data class ParsedTableBlock(
    val headers: List<String>,
    val rows: List<List<String>>,
    val endLineIndex: Int,
)

private data class ParsedIndentedCodeBlock(
    val text: String,
    val endLineIndex: Int,
)

private data class ParsedDetailsBlock(
    val summary: String,
    val body: String,
    val endLineIndex: Int,
)

private data class MarkdownTaskMatch(
    val checked: Boolean,
    val text: String,
    val indent: String,
)

private data class MarkdownBulletMatch(
    val text: String,
    val indent: String,
)

private data class MarkdownNumberedMatch(
    val number: Int,
    val text: String,
    val indent: String,
)

internal data class ImageRequestSpec(
    val url: String,
    val includeAuthorization: Boolean,
)

private data class MarkdownFootnoteDefinition(
    val label: String,
    val text: String,
)

private data class MarkdownDefinitionListItem(
    val term: String,
    val definitions: List<String>,
)

private data class MarkdownAbbreviationDefinition(
    val abbreviation: String,
    val expansion: String,
)

private enum class MarkdownCalloutType(
    val label: String,
    val accentColor: Color,
) {
    Note("Note", Color(0xFF1565C0)),
    Tip("Tip", Color(0xFF2E7D32)),
    Important("Important", Color(0xFF512DA8)),
    Warning("Warning", Color(0xFFF57C00)),
    Caution("Caution", Color(0xFFC62828)),
}

private data class MarkdownCallout(
    val type: MarkdownCalloutType,
    val body: String,
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
        parseFootnoteDefinitions(text)?.takeIf { it.isNotEmpty() }?.let { items ->
            blocks += MarkdownBlock.FootnoteDefinitions(items)
            return
        }
        parseMarkdownDefinitionList(text)?.takeIf { it.isNotEmpty() }?.let { items ->
            blocks += MarkdownBlock.DefinitionList(items)
            return
        }
        parseHtmlDefinitionList(text)?.takeIf { it.isNotEmpty() }?.let { items ->
            blocks += MarkdownBlock.DefinitionList(items)
            return
        }
        parseMarkdownAbbreviationDefinitions(text)?.takeIf { it.isNotEmpty() }?.let { items ->
            blocks += MarkdownBlock.AbbreviationList(items)
            return
        }
        if (isMarkdownReferenceDefinitionParagraph(text)) {
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

        val detailsBlock = markdownDetailsBlockAt(lines, index)
        if (detailsBlock != null) {
            flushParagraph()
            flushBlockQuote()
            blocks += MarkdownBlock.Details(
                summary = detailsBlock.summary,
                body = detailsBlock.body,
            )
            index = detailsBlock.endLineIndex + 1
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

        val standaloneImage = standaloneImageMatch(trimmedLine)
        if (standaloneImage != null) {
            flushParagraph()
            flushBlockQuote()
            blocks += MarkdownBlock.Image(
                alt = standaloneImage.alt,
                target = standaloneImage.target,
            )
            index += 1
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

        taskMatch(rawLine)?.let { task ->
            flushParagraph()
            blocks += MarkdownBlock.TaskItem(
                checked = task.checked,
                text = task.text,
                indent = task.indent,
                lineIndex = index + 1,
            )
            index += 1
            continue
        }

        bulletMatch(rawLine)?.let { bullet ->
            flushParagraph()
            blocks += MarkdownBlock.BulletItem(
                text = bullet.text,
                indent = bullet.indent,
            )
            index += 1
            continue
        }

        numberedMatch(rawLine)?.let { numbered ->
            flushParagraph()
            blocks += MarkdownBlock.NumberedItem(
                number = numbered.number,
                text = numbered.text,
                indent = numbered.indent,
            )
            index += 1
            continue
        }

        val indentedCode = markdownIndentedCodeBlockAt(lines, index)
        if (indentedCode != null) {
            flushParagraph()
            blocks += MarkdownBlock.CodeFence(
                text = indentedCode.text,
                language = "",
            )
            index = indentedCode.endLineIndex + 1
            continue
        }

        paragraph += rawLine.trimStart()
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

private fun taskMatch(line: String): MarkdownTaskMatch? {
    val match = Regex("""^([ \t]*)[-*+]\s+\[([ xX])]\s+(.*)$""").matchEntire(line) ?: return null
    return MarkdownTaskMatch(
        checked = match.groupValues[2].equals("x", ignoreCase = true),
        text = match.groupValues[3].trim(),
        indent = match.groupValues[1],
    )
}

private fun bulletMatch(line: String): MarkdownBulletMatch? {
    val match = Regex("""^([ \t]*)[-*+]\s+(.*)$""").matchEntire(line) ?: return null
    return MarkdownBulletMatch(
        text = match.groupValues[2].trim(),
        indent = match.groupValues[1],
    )
}

private fun numberedMatch(line: String): MarkdownNumberedMatch? {
    val match = Regex("""^([ \t]*)(\d+)\.\s+(.*)$""").matchEntire(line) ?: return null
    return MarkdownNumberedMatch(
        number = match.groupValues[2].toIntOrNull() ?: return null,
        text = match.groupValues[3].trim(),
        indent = match.groupValues[1],
    )
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

private fun markdownIndentedCodeBlockAt(lines: List<String>, startLineIndex: Int): ParsedIndentedCodeBlock? {
    if (startLineIndex !in lines.indices || !looksLikeIndentedCodeLine(lines[startLineIndex])) {
        return null
    }

    val content = mutableListOf<String>()
    var index = startLineIndex
    var endLineIndex = startLineIndex
    while (index < lines.size) {
        val rawLine = lines[index].trimEnd('\r')
        when {
            rawLine.isBlank() -> {
                content += ""
                endLineIndex = index
                index += 1
            }
            looksLikeIndentedCodeLine(rawLine) -> {
                content += stripIndentedCodePrefix(rawLine)
                endLineIndex = index
                index += 1
            }
            else -> break
        }
    }

    while (content.isNotEmpty() && content.last().isBlank()) {
        content.removeLast()
    }

    return ParsedIndentedCodeBlock(
        text = content.joinToString("\n"),
        endLineIndex = endLineIndex,
    )
}

private fun markdownDetailsBlockAt(lines: List<String>, startLineIndex: Int): ParsedDetailsBlock? {
    if (startLineIndex !in lines.indices) return null
    val opening = lines[startLineIndex].trim()
    if (!Regex("""^<details(?:\s+open)?\s*>$""", RegexOption.IGNORE_CASE).matches(opening)) {
        return null
    }

    var endLineIndex = -1
    var index = startLineIndex + 1
    while (index < lines.size) {
        if (lines[index].trim().equals("</details>", ignoreCase = true)) {
            endLineIndex = index
            break
        }
        index += 1
    }
    if (endLineIndex == -1) return null

    val innerLines = lines.subList(startLineIndex + 1, endLineIndex).toMutableList()
    var summary = "Details"
    if (innerLines.isNotEmpty()) {
        val summaryMatch = Regex("""(?is)^\s*<summary>\s*(.*?)\s*</summary>\s*$""")
            .matchEntire(innerLines.first().trim())
        if (summaryMatch != null) {
            summary = stripSimpleHtml(summaryMatch.groupValues[1]).ifBlank { "Details" }
            innerLines.removeAt(0)
        }
    }

    val body = innerLines
        .dropWhile(String::isBlank)
        .dropLastWhile(String::isBlank)
        .joinToString("\n")

    return ParsedDetailsBlock(
        summary = summary,
        body = body,
        endLineIndex = endLineIndex,
    )
}

private fun looksLikeIndentedCodeLine(line: String): Boolean {
    return line.startsWith("\t") || line.startsWith("    ")
}

private fun stripIndentedCodePrefix(line: String): String {
    return when {
        line.startsWith("\t") -> line.removePrefix("\t")
        line.startsWith("    ") -> line.drop(4)
        else -> line
    }
}

private fun parseFootnoteDefinitions(text: String): List<MarkdownFootnoteDefinition>? {
    val lines = text
        .lineSequence()
        .map(String::trim)
        .filter(String::isNotBlank)
        .toList()
    if (lines.isEmpty()) return null

    val pattern = Regex("""^\[\^([^\]]+)]:\s+(.+)$""")
    val items = lines.map { line ->
        val match = pattern.matchEntire(line) ?: return null
        MarkdownFootnoteDefinition(
            label = match.groupValues[1].trim(),
            text = match.groupValues[2].trim(),
        )
    }
    return items
}

private fun parseMarkdownDefinitionList(text: String): List<MarkdownDefinitionListItem>? {
    val lines = text
        .lineSequence()
        .map(String::trim)
        .filter(String::isNotBlank)
        .toList()
    if (lines.size < 2) return null

    val items = mutableListOf<MarkdownDefinitionListItem>()
    var index = 0
    while (index < lines.size) {
        val term = lines[index]
        if (term.startsWith(":")) return null
        index += 1
        val definitions = mutableListOf<String>()
        while (index < lines.size && lines[index].startsWith(":")) {
            definitions += lines[index].removePrefix(":").trim()
            index += 1
        }
        if (definitions.isEmpty()) return null
        items += MarkdownDefinitionListItem(term = term, definitions = definitions)
    }

    return items
}

private fun parseHtmlDefinitionList(text: String): List<MarkdownDefinitionListItem>? {
    val body = Regex("""(?is)^\s*<dl>\s*(.*?)\s*</dl>\s*$""").matchEntire(text)?.groupValues?.get(1)
        ?: return null
    val entries = Regex("""(?is)<dt>\s*(.*?)\s*</dt>\s*<dd>\s*(.*?)\s*</dd>""")
        .findAll(body)
        .map { match ->
            MarkdownDefinitionListItem(
                term = stripSimpleHtml(match.groupValues[1]),
                definitions = listOf(stripSimpleHtml(match.groupValues[2])),
            )
        }
        .toList()
    return entries.takeIf { it.isNotEmpty() }
}

private fun parseMarkdownAbbreviationDefinitions(text: String): List<MarkdownAbbreviationDefinition>? {
    val lines = text
        .lineSequence()
        .map(String::trim)
        .filter(String::isNotBlank)
        .toList()
    if (lines.isEmpty()) return null

    val pattern = Regex("""^\*\[([^\]]+)]:\s+(.+)$""")
    val items = lines.map { line ->
        val match = pattern.matchEntire(line) ?: return null
        MarkdownAbbreviationDefinition(
            abbreviation = match.groupValues[1].trim(),
            expansion = match.groupValues[2].trim(),
        )
    }
    return items
}

private fun stripSimpleHtml(text: String): String {
    return text.replace(Regex("""<[^>]+>"""), "").trim()
}

private fun parseMarkdownCallout(text: String): MarkdownCallout? {
    val lines = text.replace("\r\n", "\n").split('\n')
    if (lines.isEmpty()) return null
    val match = Regex("""^\[!(NOTE|TIP|IMPORTANT|WARNING|CAUTION)]\s*(.*)$""", RegexOption.IGNORE_CASE)
        .matchEntire(lines.first().trim())
        ?: return null
    val type = when (match.groupValues[1].uppercase()) {
        "NOTE" -> MarkdownCalloutType.Note
        "TIP" -> MarkdownCalloutType.Tip
        "IMPORTANT" -> MarkdownCalloutType.Important
        "WARNING" -> MarkdownCalloutType.Warning
        "CAUTION" -> MarkdownCalloutType.Caution
        else -> return null
    }
    val inlineBody = match.groupValues[2].trim()
    val remaining = lines.drop(1).joinToString("\n").trim()
    val body = listOf(inlineBody, remaining)
        .filter(String::isNotBlank)
        .joinToString("\n")
        .ifBlank { type.label }
    return MarkdownCallout(type = type, body = body)
}

private val markdownReferenceDefinitionLinePattern = Regex(
    """^\s{0,3}\[([^\]]+)]:\s*(<[^>]+>|[^\s]+)(?:\s+(?:"[^"]*"|'[^']*'|\([^)]*\)))?\s*$""",
)

private fun normalizeMarkdownReferenceKey(key: String): String {
    return key.trim().lowercase().replace(Regex("""\s+"""), " ")
}

private fun isMarkdownReferenceDefinitionParagraph(text: String): Boolean {
    val lines = text
        .lineSequence()
        .map(String::trim)
        .filter(String::isNotBlank)
        .toList()
    return lines.isNotEmpty() && lines.all(markdownReferenceDefinitionLinePattern::matches)
}

internal fun extractMarkdownReferenceDefinitions(markdown: String): Map<String, String> {
    val definitions = linkedMapOf<String, String>()
    val lines = markdown.replace("\r\n", "\n").split('\n')
    var inCodeFence = false
    var codeFenceMarker = "```"

    lines.forEach { rawLine ->
        val trimmedLine = rawLine.trim()
        if (inCodeFence) {
            if (Regex("^${Regex.escape(codeFenceMarker)}\\s*$").matches(trimmedLine)) {
                inCodeFence = false
            }
            return@forEach
        }

        val codeFenceMatch = Regex("^(```+)(.*)$").matchEntire(trimmedLine)
        if (codeFenceMatch != null) {
            inCodeFence = true
            codeFenceMarker = codeFenceMatch.groupValues[1]
            return@forEach
        }

        val definitionMatch = markdownReferenceDefinitionLinePattern.matchEntire(rawLine) ?: return@forEach
        val target = definitionMatch.groupValues[2]
            .trim()
            .removePrefix("<")
            .removeSuffix(">")
        if (target.isNotBlank()) {
            definitions[normalizeMarkdownReferenceKey(definitionMatch.groupValues[1])] = target
        }
    }

    return definitions
}

internal fun markdownListIndentLevel(indent: String): Int {
    val width = indent.fold(0) { acc, char -> acc + if (char == '\t') 4 else 1 }
    return (width / 2).coerceAtLeast(0)
}

private fun markdownListStartPadding(indent: String) = (markdownListIndentLevel(indent) * 18).dp

internal fun parseInlineMarkdown(
    text: String,
    linkColor: Color,
    linkDefinitions: Map<String, String> = emptyMap(),
): AnnotatedString {
    return buildAnnotatedString {
        appendInlineMarkdown(text, linkColor, linkDefinitions)
    }
}

private data class InlineCodeSpan(
    val text: String,
    val nextIndex: Int,
)

private val markdownEmojiShortcodes = mapOf(
    "rocket" to "\uD83D\uDE80",
    "tada" to "\uD83C\uDF89",
    "white_check_mark" to "\u2705",
    "warning" to "\u26A0\uFE0F",
    "x" to "\u274C",
)

private fun isMarkdownEscapable(char: Char): Boolean {
    return char in "\\`*_{}[]()#+-.!|:<>~"
}

private fun findUnescapedMarker(text: String, marker: String, startIndex: Int): Int {
    var searchIndex = startIndex
    while (searchIndex < text.length) {
        val candidate = text.indexOf(marker, startIndex = searchIndex)
        if (candidate == -1) {
            return -1
        }
        if (candidate == 0 || text[candidate - 1] != '\\') {
            return candidate
        }
        searchIndex = candidate + 1
    }
    return -1
}

private fun parseInlineCodeSpanAt(text: String, startIndex: Int): InlineCodeSpan? {
    if (startIndex !in text.indices || text[startIndex] != '`') return null
    val runLength = text.substring(startIndex).takeWhile { it == '`' }.length
    if (runLength == 0) return null
    val delimiter = "`".repeat(runLength)
    val end = text.indexOf(delimiter, startIndex = startIndex + runLength)
    if (end <= startIndex + runLength - 1) return null
    val content = text
        .substring(startIndex + runLength, end)
        .replace('\n', ' ')
    return InlineCodeSpan(
        text = content,
        nextIndex = end + runLength,
    )
}

private fun parseEmojiShortcodeAt(text: String, startIndex: Int): Pair<String, Int>? {
    if (startIndex !in text.indices || text[startIndex] != ':') return null
    val end = text.indexOf(':', startIndex + 1)
    if (end == -1) return null
    val shortcode = text.substring(startIndex + 1, end)
    if (!Regex("""[a-z0-9_+\-]+""", RegexOption.IGNORE_CASE).matches(shortcode)) {
        return null
    }
    val emoji = markdownEmojiShortcodes[shortcode.lowercase()] ?: return null
    return emoji to (end + 1)
}

private fun AnnotatedString.Builder.appendInlineMarkdown(
    text: String,
    linkColor: Color,
    linkDefinitions: Map<String, String>,
) {
    var index = 0

    while (index < text.length) {
        when {
            text[index] == '\\' && index + 1 < text.length && isMarkdownEscapable(text[index + 1]) -> {
                val escaped = text[index + 1]
                append(escaped)
                index += 2
                if ((escaped == '*' || escaped == '_' || escaped == '~') &&
                    index < text.length &&
                    text[index] == escaped
                ) {
                    append(text[index])
                    index += 1
                }
            }
            parseInlineCodeSpanAt(text, index) != null -> {
                val codeSpan = parseInlineCodeSpanAt(text, index)!!
                pushStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = Color(0x1A7A6F66),
                    ),
                )
                append(codeSpan.text)
                pop()
                index = codeSpan.nextIndex
            }
            text.startsWith("***", index) || text.startsWith("___", index) -> {
                val marker = text.substring(index, index + 3)
                val end = findUnescapedMarker(text, marker, startIndex = index + 3)
                if (end > index + 3) {
                    pushStyle(
                        SpanStyle(
                            fontWeight = FontWeight.Bold,
                            fontStyle = FontStyle.Italic,
                        ),
                    )
                    appendInlineMarkdown(text.substring(index + 3, end), linkColor, linkDefinitions)
                    pop()
                    index = end + 3
                } else {
                    append(text[index])
                    index += 1
                }
            }
            text.regionMatches(index, "<sub>", 0, 5, ignoreCase = true) -> {
                val end = text.indexOf("</sub>", startIndex = index + 5, ignoreCase = true)
                if (end > index + 5) {
                    pushStyle(
                        SpanStyle(
                            baselineShift = BaselineShift.Subscript,
                            fontSize = 0.8.em,
                        ),
                    )
                    appendInlineMarkdown(text.substring(index + 5, end), linkColor, linkDefinitions)
                    pop()
                    index = end + 6
                } else {
                    append(text[index])
                    index += 1
                }
            }
            text.regionMatches(index, "<sup>", 0, 5, ignoreCase = true) -> {
                val end = text.indexOf("</sup>", startIndex = index + 5, ignoreCase = true)
                if (end > index + 5) {
                    pushStyle(
                        SpanStyle(
                            baselineShift = BaselineShift.Superscript,
                            fontSize = 0.8.em,
                        ),
                    )
                    appendInlineMarkdown(text.substring(index + 5, end), linkColor, linkDefinitions)
                    pop()
                    index = end + 6
                } else {
                    append(text[index])
                    index += 1
                }
            }
            text.regionMatches(index, "<kbd>", 0, 5, ignoreCase = true) -> {
                val end = text.indexOf("</kbd>", startIndex = index + 5, ignoreCase = true)
                if (end > index + 5) {
                    pushStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = Color(0x1A5F6368),
                        ),
                    )
                    append(text.substring(index + 5, end))
                    pop()
                    index = end + 6
                } else {
                    append(text[index])
                    index += 1
                }
            }
            text.regionMatches(index, "<mark>", 0, 6, ignoreCase = true) -> {
                val end = text.indexOf("</mark>", startIndex = index + 6, ignoreCase = true)
                if (end > index + 6) {
                    pushStyle(SpanStyle(background = Color(0x40FFD54F)))
                    appendInlineMarkdown(text.substring(index + 6, end), linkColor, linkDefinitions)
                    pop()
                    index = end + 7
                } else {
                    append(text[index])
                    index += 1
                }
            }
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
                val inlineDestination = if (close > index + 1 && openParen == close + 1) {
                    parseInlineLinkDestination(text, openParen)
                } else {
                    null
                }
                if (close > index + 1 && inlineDestination != null) {
                    val alt = text.substring(index + 2, close).trim()
                    appendLink(
                        label = "[Image: ${alt.ifBlank { pageTitleFromPath(inlineDestination.target) }}]",
                        target = inlineDestination.target,
                        linkColor = linkColor,
                    )
                    index = inlineDestination.nextIndex
                } else {
                    append(text[index])
                    index += 1
                }
            }
            text.startsWith("**", index) || text.startsWith("__", index) -> {
                val marker = text.substring(index, index + 2)
                val end = findUnescapedMarker(text, marker, startIndex = index + 2)
                if (end > index + 2) {
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    appendInlineMarkdown(text.substring(index + 2, end), linkColor, linkDefinitions)
                    pop()
                    index = end + 2
                } else {
                    append(text[index])
                    index += 1
                }
            }
            text.startsWith("~~", index) -> {
                val end = findUnescapedMarker(text, "~~", startIndex = index + 2)
                if (end > index + 2) {
                    pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                    appendInlineMarkdown(text.substring(index + 2, end), linkColor, linkDefinitions)
                    pop()
                    index = end + 2
                } else {
                    append(text[index])
                    index += 1
                }
            }
            text.startsWith("*", index) || text.startsWith("_", index) -> {
                val marker = text[index]
                if (text.getOrNull(index - 1) == marker || text.getOrNull(index + 1) == marker) {
                    append(text[index])
                    index += 1
                    continue
                }
                val end = findUnescapedMarker(text, marker.toString(), startIndex = index + 1)
                if (end > index + 1) {
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    appendInlineMarkdown(text.substring(index + 1, end), linkColor, linkDefinitions)
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
            text.startsWith("[^", index) -> {
                val end = text.indexOf("]", startIndex = index + 2)
                if (end > index + 2) {
                    pushStyle(
                        SpanStyle(
                            baselineShift = BaselineShift.Superscript,
                            fontSize = 0.75.em,
                            color = linkColor,
                        ),
                    )
                    append("[${text.substring(index + 2, end).trim()}]")
                    pop()
                    index = end + 1
                } else {
                    append(text[index])
                    index += 1
                }
            }
            parseEmojiShortcodeAt(text, index) != null -> {
                val (emoji, nextIndex) = parseEmojiShortcodeAt(text, index)!!
                append(emoji)
                index = nextIndex
            }
            text.startsWith("[", index) -> {
                val close = text.indexOf("]", startIndex = index + 1)
                val openParen = if (close != -1) text.indexOf("(", startIndex = close + 1) else -1
                val inlineDestination = if (close > index + 1 && openParen == close + 1) {
                    parseInlineLinkDestination(text, openParen)
                } else {
                    null
                }
                if (close > index + 1 && inlineDestination != null) {
                    val label = text.substring(index + 1, close)
                    appendMarkdownLink(
                        label = label,
                        target = inlineDestination.target,
                        linkColor = linkColor,
                        linkDefinitions = linkDefinitions,
                    )
                    index = inlineDestination.nextIndex
                } else if (close > index + 1) {
                    val label = text.substring(index + 1, close)
                    val explicitReference = resolveExplicitReferenceLink(text, close, label, linkDefinitions)
                    val shortcutReference = linkDefinitions[normalizeMarkdownReferenceKey(label)]
                    when {
                        explicitReference != null -> {
                            appendMarkdownLink(
                                label = label,
                                target = explicitReference.target,
                                linkColor = linkColor,
                                linkDefinitions = linkDefinitions,
                            )
                            index = explicitReference.nextIndex
                        }
                        shortcutReference != null -> {
                            appendMarkdownLink(
                                label = label,
                                target = shortcutReference,
                                linkColor = linkColor,
                                linkDefinitions = linkDefinitions,
                            )
                            index = close + 1
                        }
                        else -> {
                            append(text[index])
                            index += 1
                        }
                    }
                } else {
                    append(text[index])
                    index += 1
                }
            }
            text[index] == '<' -> {
                val end = text.indexOf(">", startIndex = index + 1)
                if (end > index + 1) {
                    val body = text.substring(index + 1, end).trim()
                    val target = when {
                        body.startsWith("http://", ignoreCase = true) ||
                            body.startsWith("https://", ignoreCase = true) ||
                            body.startsWith("mailto:", ignoreCase = true) -> body
                        looksLikeAutolinkEmail(body) -> "mailto:$body"
                        else -> null
                    }
                    if (target != null) {
                        appendLink(body, target, linkColor)
                        index = end + 1
                    } else {
                        append(text[index])
                        index += 1
                    }
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

private data class InlineLinkDestination(
    val target: String,
    val nextIndex: Int,
)

private data class ExplicitReferenceLink(
    val target: String,
    val nextIndex: Int,
)

private fun parseInlineLinkDestination(text: String, openParenIndex: Int): InlineLinkDestination? {
    var index = openParenIndex + 1
    var nestedParens = 0
    while (index < text.length) {
        when (text[index]) {
            '(' -> nestedParens += 1
            ')' -> {
                if (nestedParens == 0) {
                    val rawDestination = text.substring(openParenIndex + 1, index).trim()
                    val target = when {
                        rawDestination.startsWith("<") && rawDestination.contains(">") -> {
                            rawDestination.substringAfter('<').substringBefore('>').trim()
                        }
                        ' ' in rawDestination -> rawDestination.substringBefore(' ').trim()
                        else -> rawDestination
                    }
                    return target.takeIf(String::isNotBlank)?.let {
                        InlineLinkDestination(target = it, nextIndex = index + 1)
                    }
                }
                nestedParens -= 1
            }
        }
        index += 1
    }
    return null
}

private fun resolveExplicitReferenceLink(
    text: String,
    closeBracketIndex: Int,
    label: String,
    linkDefinitions: Map<String, String>,
): ExplicitReferenceLink? {
    if (closeBracketIndex + 1 >= text.length || text[closeBracketIndex + 1] != '[') {
        return null
    }
    val referenceClose = text.indexOf("]", startIndex = closeBracketIndex + 2)
    if (referenceClose == -1) {
        return null
    }
    val rawReference = text.substring(closeBracketIndex + 2, referenceClose)
    val target = linkDefinitions[normalizeMarkdownReferenceKey(rawReference.ifBlank { label })] ?: return null
    return ExplicitReferenceLink(target = target, nextIndex = referenceClose + 1)
}

private fun looksLikeAutolinkEmail(value: String): Boolean {
    return Regex("""^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$""", RegexOption.IGNORE_CASE).matches(value)
}

private fun AnnotatedString.Builder.appendMarkdownLink(
    label: String,
    target: String,
    linkColor: Color,
    linkDefinitions: Map<String, String>,
) {
    pushStringAnnotation(tag = MarkdownLinkAnnotation, annotation = target)
    pushStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline))
    appendInlineMarkdown(label, linkColor, linkDefinitions)
    pop()
    pop()
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
