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

fun createDescriptors(document: Document, indents: LineIndents, onlyErrors: Boolean): List<IndentDescriptor> {
    val (lines, indentSize, continuationSize) = indents
    var prevLine = LineIndent(0, false)
    val descriptors = mutableListOf<IndentDescriptor>()
    var continuationNum = 0
    for (i in lines.indices) {
        val curLine = lines[i]
        if (curLine.level == -1) {
            descriptors += createErrorDescriptor(document, i)
            prevLine = LineIndent(0, false)
            continue
        }

        for (level in prevLine.level until curLine.level) {
            var lineEnd = i + 1
            while (lineEnd < lines.size && lines[lineEnd].level > level) {
                ++lineEnd
            }
            --lineEnd  // inclusive
            if (!onlyErrors) {
                descriptors += createDescriptor(document, i, lineEnd, level, indentSize)
            }
        }

        if (curLine.isContinuation) {
            continuationNum++
            var lineEnd = i + 1
            while (lineEnd < lines.size && lines[lineEnd].level == curLine.level && lines[lineEnd].isContinuation) {
                lineEnd++
            }
            --lineEnd  // inclusive

            if (!onlyErrors) {
                descriptors += createDescriptor(document, i, lineEnd, curLine.level - 1, indentSize, continuationSize, continuationNum)
            }
        } else {
            continuationNum = 0
        }

        prevLine = curLine
    }
    return descriptors
}

fun createDescriptor(document: Document, lineStart: Int, lineEnd: Int, level: Int, indentSize: Int): IndentDescriptor {
    val lineStartOffset = document.getLineStartOffset(lineStart)
    val lineEndOffset = document.getLineStartOffset(lineEnd)
    val startOffset = lineStartOffset + indentSize * level
    val endOffset = lineEndOffset + indentSize * (level + 1)
    return IndentDescriptor(startOffset, endOffset, level, indentSize)
}
fun createDescriptor(document: Document, lineStart: Int, lineEnd: Int, level: Int, indentSize: Int, continuationSize: Int, continuationNum: Int): IndentDescriptor {
    val lineStartOffset = document.getLineStartOffset(lineStart)
    val lineEndOffset = document.getLineStartOffset(lineEnd)
    val startOffset = lineStartOffset + indentSize * (level + 1) + continuationSize * (continuationNum - 1)
    val endOffset = lineEndOffset + indentSize * (level + 1) + continuationSize * continuationNum
    return IndentDescriptor(startOffset, endOffset, level, indentSize)
}

fun createErrorDescriptor(document: Document, line: Int): IndentDescriptor {
    val startOffset = document.getLineStartOffset(line)
    val endOffset = CharArrayUtil.shiftForward(document.charsSequence, startOffset, " \t")
    return IndentDescriptor(startOffset, endOffset, -1, -1)
}
