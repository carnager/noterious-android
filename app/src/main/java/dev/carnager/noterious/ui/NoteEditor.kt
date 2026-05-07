package dev.carnager.noterious.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete

internal enum class NoteEditorMode { Preview, Edit, Raw }

internal enum class NoteSlashCommand {
    Paragraph,
    Heading1,
    Heading2,
    Heading3,
    Task,
    Bullet,
    Numbered,
    Quote,
    Code,
    Table,
    Image,
}

internal sealed interface NoteEditorBlock {
    data class Heading(val level: Int, val text: String) : NoteEditorBlock
    data class Paragraph(val text: String) : NoteEditorBlock
    data class BulletItem(val text: String, val indent: String = "") : NoteEditorBlock
    data class NumberedItem(val number: Int, val text: String, val indent: String = "") : NoteEditorBlock
    data class TaskItem(val checked: Boolean, val text: String, val indent: String = "") : NoteEditorBlock
    data class BlockQuote(val text: String) : NoteEditorBlock
    data class CodeFence(val text: String, val language: String = "") : NoteEditorBlock
    data class Table(val headers: List<String>, val rows: List<List<String>>) : NoteEditorBlock
    data class Image(val alt: String, val target: String) : NoteEditorBlock
}

internal data class MarkdownFrontmatterSplit(
    val frontmatter: String,
    val body: String,
)

internal data class ParsedNoteEditorBlock(
    val block: NoteEditorBlock,
    val startLine: Int,
    val endLine: Int,
)

private data class EditorIndentedCodeBlock(
    val text: String,
    val endLineIndex: Int,
)

private data class EditorHtmlBlock(
    val text: String,
    val endLineIndex: Int,
)

internal fun splitMarkdownFrontmatter(markdown: String): MarkdownFrontmatterSplit {
    val source = markdown.replace("\r\n", "\n")
    if (!source.startsWith("---\n")) {
        return MarkdownFrontmatterSplit(frontmatter = "", body = source)
    }
    val closing = source.indexOf("\n---\n", 4)
    if (closing == -1) {
        return MarkdownFrontmatterSplit(frontmatter = "", body = source)
    }
    return MarkdownFrontmatterSplit(
        frontmatter = source.substring(0, closing + 5).trimEnd('\n'),
        body = source.substring(closing + 5).trimStart('\n'),
    )
}

internal fun combineMarkdownDocument(frontmatter: String, body: String): String {
    val normalizedFrontmatter = frontmatter.trimEnd('\n')
    val normalizedBody = body.trimEnd('\n')
    return when {
        normalizedFrontmatter.isBlank() -> {
            if (normalizedBody.isBlank()) "" else "$normalizedBody\n"
        }
        normalizedBody.isBlank() -> "$normalizedFrontmatter\n"
        else -> "$normalizedFrontmatter\n\n$normalizedBody\n"
    }
}

internal fun parseNoteEditorBlocks(markdown: String): List<NoteEditorBlock> {
    return parseNoteEditorBlocksWithLines(markdown).map { it.block }
}

internal fun parseNoteEditorBlocksWithLines(markdown: String): List<ParsedNoteEditorBlock> {
    val body = splitMarkdownFrontmatter(markdown).body
    val lines = body.replace("\r\n", "\n").split('\n')
    val blocks = mutableListOf<ParsedNoteEditorBlock>()
    val paragraph = mutableListOf<String>()
    val blockQuote = mutableListOf<String>()
    val codeFence = mutableListOf<String>()
    var inCodeFence = false
    var codeFenceMarker = "```"
    var codeFenceInfo = ""
    var paragraphStartLine = -1
    var blockQuoteStartLine = -1
    var codeFenceStartLine = -1

    fun flushParagraph() {
        if (paragraph.isEmpty()) return
        val text = paragraph.joinToString("\n").trimEnd()
        val startLine = paragraphStartLine
        val endLine = (startLine + paragraph.size - 1).coerceAtLeast(startLine)
        paragraph.clear()
        paragraphStartLine = -1
        standaloneEditorImageMatch(text)?.let { image ->
            blocks += ParsedNoteEditorBlock(
                block = NoteEditorBlock.Image(alt = image.alt, target = image.target),
                startLine = startLine,
                endLine = endLine,
            )
            return
        }
        blocks += ParsedNoteEditorBlock(
            block = NoteEditorBlock.Paragraph(text),
            startLine = startLine,
            endLine = endLine,
        )
    }

    fun flushBlockQuote() {
        if (blockQuote.isEmpty()) return
        val startLine = blockQuoteStartLine
        val endLine = (startLine + blockQuote.size - 1).coerceAtLeast(startLine)
        blocks += ParsedNoteEditorBlock(
            block = NoteEditorBlock.BlockQuote(blockQuote.joinToString("\n").trimEnd()),
            startLine = startLine,
            endLine = endLine,
        )
        blockQuote.clear()
        blockQuoteStartLine = -1
    }

    var index = 0
    while (index < lines.size) {
        val rawLine = lines[index].trimEnd()
        val trimmedLine = rawLine.trim()

        if (inCodeFence) {
            val fencePattern = Regex("^${Regex.escape(codeFenceMarker)}\\s*$")
            if (fencePattern.matches(trimmedLine)) {
                blocks += ParsedNoteEditorBlock(
                    block = NoteEditorBlock.CodeFence(
                        text = codeFence.joinToString("\n").trimEnd(),
                        language = codeFenceInfo.substringBefore(' ').trim(),
                    ),
                    startLine = codeFenceStartLine,
                    endLine = index + 1,
                )
                codeFence.clear()
                inCodeFence = false
                codeFenceInfo = ""
                codeFenceStartLine = -1
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
            codeFenceStartLine = index + 1
            index += 1
            continue
        }

        val detailsBlock = editorDetailsBlockAt(lines, index)
        if (detailsBlock != null) {
            flushParagraph()
            flushBlockQuote()
            blocks += ParsedNoteEditorBlock(
                block = NoteEditorBlock.Paragraph(detailsBlock.text),
                startLine = index + 1,
                endLine = detailsBlock.endLineIndex + 1,
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

        val tableBlock = editorTableBlockAt(lines, index)
        if (tableBlock != null) {
            flushParagraph()
            flushBlockQuote()
            blocks += ParsedNoteEditorBlock(
                block = NoteEditorBlock.Table(headers = tableBlock.headers, rows = tableBlock.rows),
                startLine = index + 1,
                endLine = tableBlock.endLineIndex + 1,
            )
            index = tableBlock.endLineIndex + 1
            continue
        }

        val standaloneImage = standaloneEditorImageMatch(trimmedLine)
        if (standaloneImage != null) {
            flushParagraph()
            flushBlockQuote()
            blocks += ParsedNoteEditorBlock(
                block = NoteEditorBlock.Image(
                    alt = standaloneImage.alt,
                    target = standaloneImage.target,
                ),
                startLine = index + 1,
                endLine = index + 1,
            )
            index += 1
            continue
        }

        if (trimmedLine.startsWith(">")) {
            flushParagraph()
            if (blockQuoteStartLine == -1) {
                blockQuoteStartLine = index + 1
            }
            blockQuote += trimmedLine.removePrefix(">").trimStart()
            index += 1
            continue
        }
        flushBlockQuote()

        val heading = editorHeadingMatch(trimmedLine)
        if (heading != null) {
            flushParagraph()
            blocks += ParsedNoteEditorBlock(
                block = NoteEditorBlock.Heading(level = heading.first, text = heading.second),
                startLine = index + 1,
                endLine = index + 1,
            )
            index += 1
            continue
        }

        val task = editorTaskMatch(rawLine)
        if (task != null) {
            flushParagraph()
            blocks += ParsedNoteEditorBlock(
                block = NoteEditorBlock.TaskItem(
                    checked = task.checked,
                    text = task.text,
                    indent = task.indent,
                ),
                startLine = index + 1,
                endLine = index + 1,
            )
            index += 1
            continue
        }

        val bullet = editorBulletMatch(rawLine)
        if (bullet != null) {
            flushParagraph()
            blocks += ParsedNoteEditorBlock(
                block = NoteEditorBlock.BulletItem(
                    text = bullet.text,
                    indent = bullet.indent,
                ),
                startLine = index + 1,
                endLine = index + 1,
            )
            index += 1
            continue
        }

        val numbered = editorNumberedMatch(rawLine)
        if (numbered != null) {
            flushParagraph()
            blocks += ParsedNoteEditorBlock(
                block = NoteEditorBlock.NumberedItem(
                    number = numbered.number,
                    text = numbered.text,
                    indent = numbered.indent,
                ),
                startLine = index + 1,
                endLine = index + 1,
            )
            index += 1
            continue
        }

        val indentedCode = editorIndentedCodeBlockAt(lines, index)
        if (indentedCode != null) {
            flushParagraph()
            blocks += ParsedNoteEditorBlock(
                block = NoteEditorBlock.CodeFence(
                    text = indentedCode.text,
                    language = "",
                ),
                startLine = index + 1,
                endLine = indentedCode.endLineIndex + 1,
            )
            index = indentedCode.endLineIndex + 1
            continue
        }

        if (paragraphStartLine == -1) {
            paragraphStartLine = index + 1
        }
        paragraph += rawLine.trimStart()
        index += 1
    }

    flushParagraph()
    flushBlockQuote()

    if (codeFence.isNotEmpty()) {
        blocks += ParsedNoteEditorBlock(
            block = NoteEditorBlock.CodeFence(
                text = codeFence.joinToString("\n").trimEnd(),
                language = codeFenceInfo.substringBefore(' ').trim(),
            ),
            startLine = codeFenceStartLine.takeIf { it > 0 } ?: lines.size.coerceAtLeast(1),
            endLine = lines.size.coerceAtLeast(codeFenceStartLine),
        )
    }

    return blocks
}

internal fun serializeNoteEditorBlocks(blocks: List<NoteEditorBlock>): String {
    if (blocks.isEmpty()) return ""

    val builder = StringBuilder()
    blocks.forEachIndexed { index, block ->
        if (index > 0) {
            val previous = blocks[index - 1]
            builder.append(if (requiresBlankLineBetween(previous, block)) "\n\n" else "\n")
        }
        builder.append(blockToMarkdown(block))
    }

    return builder.toString().trimEnd('\n')
}

internal fun newBlockForCommand(command: NoteSlashCommand): NoteEditorBlock {
    return when (command) {
        NoteSlashCommand.Paragraph -> NoteEditorBlock.Paragraph("")
        NoteSlashCommand.Heading1 -> NoteEditorBlock.Heading(level = 1, text = "")
        NoteSlashCommand.Heading2 -> NoteEditorBlock.Heading(level = 2, text = "")
        NoteSlashCommand.Heading3 -> NoteEditorBlock.Heading(level = 3, text = "")
        NoteSlashCommand.Task -> NoteEditorBlock.TaskItem(checked = false, text = "")
        NoteSlashCommand.Bullet -> NoteEditorBlock.BulletItem("")
        NoteSlashCommand.Numbered -> NoteEditorBlock.NumberedItem(number = 1, text = "")
        NoteSlashCommand.Quote -> NoteEditorBlock.BlockQuote("")
        NoteSlashCommand.Code -> NoteEditorBlock.CodeFence(text = "", language = "")
        NoteSlashCommand.Table -> NoteEditorBlock.Table(
            headers = listOf("Column", "Value"),
            rows = listOf(listOf("", "")),
        )
        NoteSlashCommand.Image -> NoteEditorBlock.Image(alt = "", target = "")
    }
}

internal fun insertEditorBlock(
    blocks: List<NoteEditorBlock>,
    selectedIndex: Int,
    block: NoteEditorBlock,
): List<NoteEditorBlock> {
    val next = blocks.toMutableList()
    val insertIndex = if (selectedIndex in next.indices) selectedIndex + 1 else next.size
    next.add(insertIndex, block)
    return next
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun GuiNoteEditor(
    blocks: List<NoteEditorBlock>,
    selectedBlockIndex: Int,
    modifier: Modifier = Modifier,
    onSelectedBlockChange: (Int) -> Unit,
    onBlocksChange: (List<NoteEditorBlock>) -> Unit,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier.verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (blocks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No blocks yet. Use the slash menu to insert one.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        blocks.forEachIndexed { index, block ->
            val selected = index == selectedBlockIndex
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (selected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    },
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectedBlockChange(index) },
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            AssistChip(onClick = { onSelectedBlockChange(index) }, label = {
                                Text(blockLabel(block))
                            })
                            if (selected) {
                                AssistChip(onClick = { }, label = { Text("Insert Here") })
                            }
                        }
                        IconButton(
                            onClick = {
                                val next = blocks.toMutableList()
                                next.removeAt(index)
                                onBlocksChange(next)
                                onSelectedBlockChange((index - 1).coerceAtLeast(0))
                            },
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Block loeschen")
                        }
                    }

                    when (block) {
                        is NoteEditorBlock.Heading -> {
                            OutlinedTextField(
                                value = block.text,
                                onValueChange = { value ->
                                    onBlocksChange(blocks.updated(index, block.copy(text = value)))
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Heading") },
                                singleLine = true,
                            )
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(1, 2, 3).forEach { level ->
                                    AssistChip(
                                        onClick = {
                                            onBlocksChange(blocks.updated(index, block.copy(level = level)))
                                        },
                                        label = { Text("H$level") },
                                    )
                                }
                            }
                        }
                        is NoteEditorBlock.Paragraph -> {
                            OutlinedTextField(
                                value = block.text,
                                onValueChange = { value ->
                                    onBlocksChange(blocks.updated(index, block.copy(text = value)))
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Paragraph") },
                            )
                        }
                        is NoteEditorBlock.BulletItem -> {
                            OutlinedTextField(
                                value = block.text,
                                onValueChange = { value ->
                                    onBlocksChange(blocks.updated(index, block.copy(text = value)))
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Bullet Item") },
                                singleLine = true,
                            )
                        }
                        is NoteEditorBlock.NumberedItem -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = block.number.toString(),
                                    onValueChange = { value ->
                                        onBlocksChange(
                                            blocks.updated(
                                                index,
                                                block.copy(number = value.toIntOrNull() ?: block.number),
                                            ),
                                        )
                                    },
                                    modifier = Modifier.width(96.dp),
                                    label = { Text("No.") },
                                    singleLine = true,
                                )
                                OutlinedTextField(
                                    value = block.text,
                                    onValueChange = { value ->
                                        onBlocksChange(blocks.updated(index, block.copy(text = value)))
                                    },
                                    modifier = Modifier.weight(1f),
                                    label = { Text("Item") },
                                    singleLine = true,
                                )
                            }
                        }
                        is NoteEditorBlock.TaskItem -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                AssistChip(
                                    onClick = {
                                        onBlocksChange(blocks.updated(index, block.copy(checked = !block.checked)))
                                    },
                                    label = { Text(if (block.checked) "Done" else "Todo") },
                                )
                                OutlinedTextField(
                                    value = block.text,
                                    onValueChange = { value ->
                                        onBlocksChange(blocks.updated(index, block.copy(text = value)))
                                    },
                                    modifier = Modifier.weight(1f),
                                    label = { Text("Task") },
                                    singleLine = true,
                                )
                            }
                        }
                        is NoteEditorBlock.BlockQuote -> {
                            OutlinedTextField(
                                value = block.text,
                                onValueChange = { value ->
                                    onBlocksChange(blocks.updated(index, block.copy(text = value)))
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Quote") },
                            )
                        }
                        is NoteEditorBlock.CodeFence -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = block.language,
                                    onValueChange = { value ->
                                        onBlocksChange(blocks.updated(index, block.copy(language = value.trim())))
                                    },
                                    modifier = Modifier.width(140.dp),
                                    label = { Text("Language") },
                                    singleLine = true,
                                )
                            }
                            OutlinedTextField(
                                value = block.text,
                                onValueChange = { value ->
                                    onBlocksChange(blocks.updated(index, block.copy(text = value)))
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Code") },
                                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                            )
                        }
                        is NoteEditorBlock.Table -> {
                            TableBlockEditor(
                                table = block,
                                onChange = { table ->
                                    onBlocksChange(blocks.updated(index, table))
                                },
                            )
                        }
                        is NoteEditorBlock.Image -> {
                            OutlinedTextField(
                                value = block.alt,
                                onValueChange = { value ->
                                    onBlocksChange(blocks.updated(index, block.copy(alt = value)))
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Alt / Caption") },
                            )
                            OutlinedTextField(
                                value = block.target,
                                onValueChange = { value ->
                                    onBlocksChange(blocks.updated(index, block.copy(target = value)))
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Image URL or Path") },
                                singleLine = true,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TableBlockEditor(
    table: NoteEditorBlock.Table,
    onChange: (NoteEditorBlock.Table) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = {
                onChange(
                    table.copy(
                        headers = table.headers + "Column ${table.headers.size + 1}",
                        rows = table.rows.map { row -> row + "" },
                    ),
                )
            }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("Column")
            }
            TextButton(onClick = {
                onChange(
                    table.copy(
                        rows = table.rows + listOf(List(table.headers.size) { "" }),
                    ),
                )
            }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("Row")
            }
        }

        Column(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                table.headers.forEachIndexed { columnIndex, header ->
                    OutlinedTextField(
                        value = header,
                        onValueChange = { value ->
                            val nextHeaders = table.headers.toMutableList()
                            nextHeaders[columnIndex] = value
                            onChange(table.copy(headers = nextHeaders))
                        },
                        modifier = Modifier.width(160.dp),
                        label = { Text("Header ${columnIndex + 1}") },
                        singleLine = true,
                    )
                }
            }

            table.rows.forEachIndexed { rowIndex, row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                    row.forEachIndexed { columnIndex, cell ->
                        OutlinedTextField(
                            value = cell,
                            onValueChange = { value ->
                                val nextRows = table.rows.mapIndexed { currentRowIndex, currentRow ->
                                    if (currentRowIndex != rowIndex) currentRow
                                    else currentRow.mapIndexed { currentColumnIndex, currentCell ->
                                        if (currentColumnIndex == columnIndex) value else currentCell
                                    }
                                }
                                onChange(table.copy(rows = nextRows))
                            },
                            modifier = Modifier.width(160.dp),
                            label = { Text("R${rowIndex + 1}C${columnIndex + 1}") },
                        )
                    }
                    IconButton(
                        onClick = {
                            val nextRows = table.rows.toMutableList()
                            nextRows.removeAt(rowIndex)
                            onChange(table.copy(rows = nextRows.ifEmpty { listOf(List(table.headers.size) { "" }) }))
                        },
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Zeile loeschen")
                    }
                }
            }
        }
    }
}

private data class EditorTableBlock(
    val headers: List<String>,
    val rows: List<List<String>>,
    val endLineIndex: Int,
)

private data class EditorTaskMatch(
    val checked: Boolean,
    val text: String,
    val indent: String,
)

private data class EditorBulletMatch(
    val text: String,
    val indent: String,
)

private data class EditorNumberedMatch(
    val number: Int,
    val text: String,
    val indent: String,
)

private data class EditorImageMatch(
    val alt: String,
    val target: String,
)

private fun editorDetailsBlockAt(lines: List<String>, startLineIndex: Int): EditorHtmlBlock? {
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
    if (endLineIndex == -1) {
        return null
    }

    return EditorHtmlBlock(
        text = lines.subList(startLineIndex, endLineIndex + 1).joinToString("\n").trimEnd(),
        endLineIndex = endLineIndex,
    )
}

private fun editorIndentedCodeBlockAt(lines: List<String>, startLineIndex: Int): EditorIndentedCodeBlock? {
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

    return EditorIndentedCodeBlock(
        text = content.joinToString("\n"),
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

private fun editorHeadingMatch(line: String): Pair<Int, String>? {
    val hashes = line.takeWhile { it == '#' }
    if (hashes.isEmpty() || hashes.length > 6) return null
    val text = line.drop(hashes.length).trim()
    if (text.isBlank()) return null
    return hashes.length to text
}

private fun editorTaskMatch(line: String): EditorTaskMatch? {
    val match = Regex("""^([ \t]*)[-*+]\s+\[([ xX])]\s+(.*)$""").matchEntire(line) ?: return null
    return EditorTaskMatch(
        checked = match.groupValues[2].equals("x", ignoreCase = true),
        text = match.groupValues[3].trim(),
        indent = match.groupValues[1],
    )
}

private fun editorBulletMatch(line: String): EditorBulletMatch? {
    val match = Regex("""^([ \t]*)[-*+]\s+(.*)$""").matchEntire(line) ?: return null
    return EditorBulletMatch(
        text = match.groupValues[2].trim(),
        indent = match.groupValues[1],
    )
}

private fun editorNumberedMatch(line: String): EditorNumberedMatch? {
    val match = Regex("""^([ \t]*)(\d+)\.\s+(.*)$""").matchEntire(line) ?: return null
    return EditorNumberedMatch(
        number = match.groupValues[2].toIntOrNull() ?: return null,
        text = match.groupValues[3].trim(),
        indent = match.groupValues[1],
    )
}

private fun standaloneEditorImageMatch(text: String): EditorImageMatch? {
    val trimmed = text.trim()
    val wikiMatch = Regex("""^!\[\[([^\]|]+)(?:\|([^\]]+))?]]$""").matchEntire(trimmed)
    if (wikiMatch != null) {
        val target = wikiMatch.groupValues[1].trim()
        val label = wikiMatch.groupValues.getOrNull(2).orEmpty().trim()
        return EditorImageMatch(
            alt = label.ifBlank { pageTitleFromPath(target) },
            target = target,
        )
    }

    val markdownMatch = Regex("""^!\[([^\]]*)]\(([^)\s]+)\)$""").matchEntire(trimmed)
    if (markdownMatch != null) {
        val alt = markdownMatch.groupValues[1].trim()
        val target = markdownMatch.groupValues[2].trim()
        return EditorImageMatch(
            alt = alt,
            target = target,
        )
    }

    return null
}

private fun splitMarkdownTableRow(line: String): List<String> {
    return line
        .trim()
        .removePrefix("|")
        .removeSuffix("|")
        .split("|")
        .map(String::trim)
}

private fun isMarkdownTableSeparator(line: String): Boolean {
    val cells = splitMarkdownTableRow(line)
    return cells.isNotEmpty() && cells.all { cell ->
        Regex("^:?-{3,}:?$").matches(cell)
    }
}

private fun looksLikeMarkdownTableRow(line: String): Boolean {
    return line.contains('|') && splitMarkdownTableRow(line).size >= 2
}

private fun editorTableBlockAt(lines: List<String>, startLineIndex: Int): EditorTableBlock? {
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
        if (!looksLikeMarkdownTableRow(candidate)) break
        rows += splitMarkdownTableRow(candidate)
        endLineIndex = index
        index += 1
    }

    return EditorTableBlock(headers = headers, rows = rows, endLineIndex = endLineIndex)
}

private fun requiresBlankLineBetween(previous: NoteEditorBlock, current: NoteEditorBlock): Boolean {
    val previousListLike = previous is NoteEditorBlock.BulletItem ||
        previous is NoteEditorBlock.NumberedItem ||
        previous is NoteEditorBlock.TaskItem
    val currentListLike = current is NoteEditorBlock.BulletItem ||
        current is NoteEditorBlock.NumberedItem ||
        current is NoteEditorBlock.TaskItem
    if (previousListLike && currentListLike) return false
    if (previous is NoteEditorBlock.BlockQuote && current is NoteEditorBlock.BlockQuote) return false
    return true
}

internal fun blockToMarkdown(block: NoteEditorBlock): String {
    return when (block) {
        is NoteEditorBlock.Heading -> "${"#".repeat(block.level.coerceIn(1, 6))} ${block.text.trim()}"
        is NoteEditorBlock.Paragraph -> block.text.trimEnd()
        is NoteEditorBlock.BulletItem -> "${block.indent}- ${block.text.trimEnd()}"
        is NoteEditorBlock.NumberedItem -> "${block.indent}${block.number}. ${block.text.trimEnd()}"
        is NoteEditorBlock.TaskItem -> "${block.indent}- [${if (block.checked) "x" else " "}] ${block.text.trimEnd()}"
        is NoteEditorBlock.BlockQuote -> block.text
            .replace("\r\n", "\n")
            .split('\n')
            .joinToString("\n") { line -> "> ${line.trimEnd()}" }
        is NoteEditorBlock.CodeFence -> buildString {
            append("```")
            append(block.language.trim())
            append('\n')
            append(block.text.trimEnd())
            append('\n')
            append("```")
        }
        is NoteEditorBlock.Table -> buildString {
            val headers = if (block.headers.isEmpty()) listOf("Column", "Value") else block.headers
            append("| ")
            append(headers.joinToString(" | ") { it.trim() })
            append(" |\n| ")
            append(List(headers.size) { "---" }.joinToString(" | "))
            append(" |")
            val normalizedRows = if (block.rows.isEmpty()) listOf(List(headers.size) { "" }) else block.rows
            normalizedRows.forEach { row ->
                append("\n| ")
                append(headers.indices.joinToString(" | ") { columnIndex ->
                    row.getOrElse(columnIndex) { "" }.trimEnd()
                })
                append(" |")
            }
        }
        is NoteEditorBlock.Image -> "![${block.alt.trim()}](${block.target.trim()})"
    }
}

private fun blockLabel(block: NoteEditorBlock): String {
    return when (block) {
        is NoteEditorBlock.Heading -> "Heading ${block.level}"
        is NoteEditorBlock.Paragraph -> "Paragraph"
        is NoteEditorBlock.BulletItem -> "Bullet"
        is NoteEditorBlock.NumberedItem -> "Numbered"
        is NoteEditorBlock.TaskItem -> "Task"
        is NoteEditorBlock.BlockQuote -> "Quote"
        is NoteEditorBlock.CodeFence -> if (block.language.isBlank()) "Code" else "Code: ${block.language}"
        is NoteEditorBlock.Table -> "Table"
        is NoteEditorBlock.Image -> "Image"
    }
}

internal fun List<NoteEditorBlock>.updated(index: Int, block: NoteEditorBlock): List<NoteEditorBlock> {
    val next = toMutableList()
    next[index] = block
    return next
}
