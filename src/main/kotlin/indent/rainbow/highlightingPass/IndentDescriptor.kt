package indent.rainbow.highlightingPass

import com.intellij.openapi.editor.Document
import com.intellij.util.text.CharArrayUtil

/**
 * Consider code:
 * ```kotlin
 * fun main() {
 *     if (true) {
 *     XXXXfoo1()
 *     YYYYfoo2()
 *     ZZZZfoo3()
 *     }
 * }
 * ```
 *
 * Here X, Y and Z are spaces and denotes indents of same level on different lines.
 * One [IndentDescriptor] corresponds to union of such adjacent indents of same level.
 *
 * Symbols | denote start and end offsets of the descriptor:
 * ```kotlin
 * fun main() {
 *     if (true) {
 *     |+++foo1()
 *     ++++foo2()
 *     +++|foo3()
 *     }
 * }
 * ```
 */
class IndentDescriptor(
    val startOffset: Int,
    val endOffset: Int,
    val level: Int,
    val indentSize: Int,
) {
    override fun toString(): String {
        val level = if (level == -1) " e" else "#$level"
        return "$startOffset-$endOffset $level"
    }
}

fun createDescriptors(document: Document, indents: LineIndents): List<IndentDescriptor> {
    val (levels, indentSize) = indents
    var previousLevel = 0
    val descriptors = mutableListOf<IndentDescriptor>()
    for (line in levels.indices) {
        val currentLevel = levels[line]
        if (currentLevel == -1) {
            descriptors += createErrorDescriptor(document, line)
            previousLevel = 0
            continue
        }

        for (level in previousLevel until currentLevel) {
            var lineEnd = line + 1
            while (lineEnd < levels.size && levels[lineEnd] > level) {
                ++lineEnd
            }
            --lineEnd  // inclusive
            descriptors += createDescriptor(document, line, lineEnd, level, indentSize)
        }
        previousLevel = currentLevel
    }
    return descriptors
}

fun createDescriptor(document: Document, lineStart: Int, lineEnd: Int, level: Int, indentSize: Int): IndentDescriptor {
    val lineStartOffset = document.getLineStartOffset(lineStart)
    val lineEndOffset = document.getLineStartOffset(lineEnd)
    val startOffset = lineStartOffset + level * indentSize
    val endOffset = lineEndOffset + (level + 1) * indentSize
    return IndentDescriptor(startOffset, endOffset, level, indentSize)
}

fun createErrorDescriptor(document: Document, line: Int): IndentDescriptor {
    val startOffset = document.getLineStartOffset(line)
    val endOffset = CharArrayUtil.shiftForward(document.charsSequence, startOffset, " \t")
    return IndentDescriptor(startOffset, endOffset, -1, -1)
}
