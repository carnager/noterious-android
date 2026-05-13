package dev.carnager.noterious.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.automirrored.filled.FormatIndentDecrease
import androidx.compose.material.icons.automirrored.filled.FormatIndentIncrease
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

/**
 * Returns the start (inclusive) and end (exclusive) character offsets of the line
 * containing [cursorPosition] within [text]. Use with [String.substring] as
 * `text.substring(range.first, range.last)`.
 */
internal data class LineRange(val start: Int, val endExclusive: Int)

internal fun currentLineRange(text: String, cursorPosition: Int): LineRange {
    val clamped = cursorPosition.coerceIn(0, text.length)
    val start = text.lastIndexOf('\n', clamped - 1).let { if (it == -1) 0 else it + 1 }
    val end = text.indexOf('\n', clamped).let { if (it == -1) text.length else it }
    return LineRange(start, end)
}

internal fun String.lineContent(range: LineRange): String = substring(range.start, range.endExclusive)

@Composable
internal fun MarkdownToolbar(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onShowLinkDialog: () -> Unit,
    onShowDatePicker: () -> Unit,
    onShowTimePicker: () -> Unit,
    onUploadFile: () -> Unit,
    onAttachDocument: () -> Unit,
    isUploading: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val text = value.text
    val cursor = value.selection.start

    fun replaceLinePrefix(
        oldPrefix: String,
        newPrefix: String,
    ) {
        val range = currentLineRange(text, cursor)
        val line = text.lineContent(range)
        val newLine = newPrefix + line.removePrefix(oldPrefix)
        val prefixDelta = newPrefix.length - oldPrefix.length
        val newText = text.replaceRange(range.start, range.endExclusive, newLine)
        val newCursor = (cursor + prefixDelta).coerceIn(range.start, range.start + newLine.length)
        onValueChange(
            TextFieldValue(
                text = newText,
                selection = TextRange(newCursor),
            )
        )
    }

    fun currentLine(): String {
        val range = currentLineRange(text, cursor)
        return text.lineContent(range)
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
            // 1. Heading cycle
            FilledTonalIconButton(
                onClick = {
                    val line = currentLine()
                    val range = currentLineRange(text, cursor)
                    when {
                        line.startsWith("### ") -> {
                            val stripped = line.removePrefix("### ")
                            val newText = text.replaceRange(range.start, range.endExclusive, stripped)
                            val newCursor = (cursor - 4).coerceAtLeast(range.start)
                            onValueChange(TextFieldValue(newText, TextRange(newCursor)))
                        }
                        line.startsWith("## ") -> replaceLinePrefix("## ", "### ")
                        line.startsWith("# ") -> replaceLinePrefix("# ", "## ")
                        else -> replaceLinePrefix("", "# ")
                    }
                },
                content = { Icon(Icons.Default.Title, contentDescription = "Heading") },
            )

            FilledTonalIconButton(
                onClick = {
                    val line = currentLine()
                    when {
                        line.startsWith("- [x] ") -> replaceLinePrefix("- [x] ", "")
                        line.startsWith("- [ ] ") -> replaceLinePrefix("- [ ] ", "- [x] ")
                        else -> replaceLinePrefix("", "- [ ] ")
                    }
                },
                content = { Icon(Icons.Default.CheckBox, contentDescription = "Task") },
            )

            FilledTonalIconButton(
                onClick = {
                    val line = currentLine()
                    when {
                        line.startsWith("- ") -> replaceLinePrefix("- ", "")
                        line.startsWith("1. ") -> replaceLinePrefix("1. ", "- ")
                        else -> replaceLinePrefix("", "- ")
                    }
                },
                content = { Icon(Icons.AutoMirrored.Filled.FormatListBulleted, contentDescription = "Bullet list") },
            )

            FilledTonalIconButton(
                onClick = {
                    val line = currentLine()
                    when {
                        line.startsWith("1. ") -> replaceLinePrefix("1. ", "")
                        line.startsWith("- ") -> replaceLinePrefix("- ", "1. ")
                        else -> replaceLinePrefix("", "1. ")
                    }
                },
                content = { Icon(Icons.Default.FormatListNumbered, contentDescription = "Numbered list") },
            )

            FilledTonalIconButton(
                onClick = {
                    val sel = value.selection
                    if (sel.collapsed) {
                        val newText = text.substring(0, cursor) + "****" + text.substring(cursor)
                        onValueChange(
                            TextFieldValue(newText, TextRange(cursor + 2))
                        )
                    } else {
                        val selected = text.substring(sel.min, sel.max)
                        val wrapped = "**$selected**"
                        val newText = text.replaceRange(sel.min, sel.max, wrapped)
                        onValueChange(
                            TextFieldValue(newText, TextRange(sel.min + 2, sel.max + 2))
                        )
                    }
                },
                content = { Icon(Icons.Default.FormatBold, contentDescription = "Bold") },
            )

            FilledTonalIconButton(
                onClick = {
                    val sel = value.selection
                    if (sel.collapsed) {
                        val newText = text.substring(0, cursor) + "__" + text.substring(cursor)
                        onValueChange(
                            TextFieldValue(newText, TextRange(cursor + 1))
                        )
                    } else {
                        val selected = text.substring(sel.min, sel.max)
                        val wrapped = "_${selected}_"
                        val newText = text.replaceRange(sel.min, sel.max, wrapped)
                        onValueChange(
                            TextFieldValue(newText, TextRange(sel.min + 1, sel.max + 1))
                        )
                    }
                },
                content = { Icon(Icons.Default.FormatItalic, contentDescription = "Italic") },
            )

            FilledTonalIconButton(
                onClick = onShowLinkDialog,
                content = { Icon(Icons.Default.Link, contentDescription = "Link") },
            )

            FilledTonalIconButton(
                onClick = onShowDatePicker,
                content = { Icon(Icons.Default.Event, contentDescription = "Due date") },
            )

            FilledTonalIconButton(
                onClick = onShowTimePicker,
                content = { Icon(Icons.Default.Schedule, contentDescription = "Reminder") },
            )

            FilledTonalIconButton(
                onClick = {
                    val range = currentLineRange(text, cursor)
                    val newText = text.replaceRange(range.start, range.start, "    ")
                    onValueChange(
                        TextFieldValue(newText, TextRange(cursor + 4))
                    )
                },
                content = { Icon(Icons.AutoMirrored.Filled.FormatIndentIncrease, contentDescription = "Indent") },
            )

            FilledTonalIconButton(
                onClick = {
                    val range = currentLineRange(text, cursor)
                    val line = text.lineContent(range)
                    val (stripped, removed) = when {
                        line.startsWith("    ") -> line.removePrefix("    ") to 4
                        line.startsWith("\t") -> line.removePrefix("\t") to 1
                        line.startsWith("   ") -> line.removePrefix("   ") to 3
                        line.startsWith("  ") -> line.removePrefix("  ") to 2
                        line.startsWith(" ") -> line.removePrefix(" ") to 1
                        else -> line to 0
                    }
                    if (removed > 0) {
                        val newText = text.replaceRange(range.start, range.endExclusive, stripped)
                        val newCursor = (cursor - removed).coerceAtLeast(range.start)
                        onValueChange(
                            TextFieldValue(newText, TextRange(newCursor))
                        )
                    }
                },
                content = { Icon(Icons.AutoMirrored.Filled.FormatIndentDecrease, contentDescription = "Outdent") },
            )

            FilledTonalIconButton(
                onClick = onUploadFile,
                enabled = !isUploading,
                content = {
                    if (isUploading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(Icons.Default.Upload, contentDescription = "Upload file")
                    }
                },
            )

            FilledTonalIconButton(
                onClick = onAttachDocument,
                content = { Icon(Icons.Default.AttachFile, contentDescription = "Attach document") },
            )
        }
}
