package indent.rainbow.highlightingPass

import com.intellij.application.options.CodeStyle
import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiFile
import com.intellij.util.text.CharArrayUtil
import indent.rainbow.ifNotPositive
import indent.rainbow.mapToIndentArray
import indent.rainbow.settings.IrConfig
import indent.rainbow.settings.cachedData
import java.util.regex.Pattern
import kotlin.math.min

@Suppress("ArrayInDataClass")
data class LineIndents(val lines: Array<LineIndent>, val indentSize: Int, val continuationSize: Int)

data class LineIndent(var level: Int, val isContinuation: Boolean)

class LineIndentsCalculator(private val file: PsiFile, private val document: Document) {

    private val config = IrConfig.INSTANCE
    private val ignoreLinesStartingWith: Pattern = config.cachedData.ignoreLinesStartingWith
    private val disableErrorHighlighting: Boolean = config.cachedData.disableErrorHighlightingLanguageFilter(file.language)

    fun compute(): LineIndents {
        val tabsAndSpaces = getTabsAndSpaces()
        val indents = getExplicitIndents(tabsAndSpaces)
        if (config.highlightEmptyLines) {
            fillIndentsOnEmptyLines(indents.lines)
        }
        return indents
    }

    private fun getExplicitIndents(tabsAndSpaces: Array<LineIndentInfo>): LineIndents {
        val indentOptions = CodeStyle.getIndentOptions(file)
        val useTabs = indentOptions.USE_TAB_CHARACTER
        val indentSize = indentOptions.INDENT_SIZE.ifNotPositive { 4 }
        val continuationIndentSize = indentOptions.CONTINUATION_INDENT_SIZE.ifNotPositive { 2 }
        return if (useTabs) {
            val indents = tabsAndSpaces.mapToIndentArray { cur, prev ->
                if (cur.tabs + cur.spaces == cur.totalIndent) {
                    LineIndent(cur.tabs, false)
                } else {
                    LineIndent(-1, false)  // incorrect mixed tabs and spaces
                }
            }
            LineIndents(indents, 1, 1)
        } else {
            val fileText = document.charsSequence
            val indents = Array(tabsAndSpaces.size) { LineIndent(0, false) }
            for (i in tabsAndSpaces.indices) {
                val prevIndent = if (i > 0) indents[i - 1] else LineIndent(0, false)
                val cur = tabsAndSpaces[i]
                val prev = if (i > 0) tabsAndSpaces[i - 1] else cur

                val isContinuation = cur.spaces - prev.spaces == continuationIndentSize && prevIndent.level != -1
                val isCorrectLine = cur.tabs == 0 && cur.spaces == cur.totalIndent
                        && (cur.spaces % indentSize == 0 || isContinuation || shouldIgnoreLine(cur, fileText))
                if (isCorrectLine || disableErrorHighlighting) {
                    indents[i] = LineIndent(if (isContinuation) prevIndent.level else cur.spaces / indentSize, isContinuation)
                } else {
                    indents[i] = LineIndent(-1, false)
                }
            }
            LineIndents(indents, indentSize, continuationIndentSize)
        }
    }

    private fun shouldIgnoreLine(info: LineIndentInfo, fileText: CharSequence): Boolean {
        val lineText = fileText.subSequence(info.startOffset + info.totalIndent, fileText.length)
        return ignoreLinesStartingWith.matcher(lineText).lookingAt()
    }

    private fun fillIndentsOnEmptyLines(indents: Array<LineIndent>) {
        fun isEmptyLine(line: Int): Boolean =
            indents[line].level == 0 && document.getLineStartOffset(line) == document.getLineEndOffset(line)

        for (lineStart in 1..indents.size - 2) {
            val prev = indents[lineStart - 1]
            if (!isEmptyLine(lineStart) || prev.level <= 0) continue
            var lineEnd = lineStart + 1
            while (lineEnd < indents.size && isEmptyLine(lineEnd)) {
                ++lineEnd
            }
            val next = indents.getOrElse(lineEnd) { LineIndent(0, false) }
            if (next.level > 0) {
                for (line in lineStart until lineEnd) {
                    indents[line].level = min(prev.level, next.level)
                }
            }
        }
    }

    private fun getTabsAndSpaces(): Array<LineIndentInfo> =
        Array(document.lineCount) { line -> document.getNumberTabsAndSpaces(line) }
}

/** [totalIndent] can be greater then [tabs] + [spaces] if line contains mixed tabs/spaces */
private data class LineIndentInfo(
    val startOffset: Int,
    val tabs: Int,
    val spaces: Int,
    val totalIndent: Int,
)

private fun Document.getNumberTabsAndSpaces(line: Int): LineIndentInfo {
    val lineStart = getLineStartOffset(line)
    val fileText = charsSequence

    val numberTabs = fileText.getNumberCharsFrom(lineStart, '\t')
    val numberSpaces = fileText.getNumberCharsFrom(lineStart + numberTabs, ' ')
    val totalIndent = CharArrayUtil.shiftForward(fileText, lineStart + numberTabs + numberSpaces, " \t") - lineStart
    return LineIndentInfo(lineStart, numberTabs, numberSpaces, totalIndent)
}

private fun CharSequence.getNumberCharsFrom(start: Int, char: Char): Int {
    var offset = start
    while (offset < length && this[offset] == char) {
        ++offset
    }
    return offset - start
}
