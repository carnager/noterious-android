package dev.carnager.noterious.ui

internal enum class NoteEditorMode { Rendered, Edit }

internal data class MarkdownFrontmatterSplit(
    val frontmatter: String,
    val body: String,
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
