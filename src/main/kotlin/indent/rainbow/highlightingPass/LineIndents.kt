package indent.rainbow.highlightingPass

import com.intellij.application.options.CodeStyle
import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiFile
import com.intellij.util.text.CharArrayUtil
import indent.rainbow.ifNotPositive
import indent.rainbow.mapToIntArray
import indent.rainbow.settings.IrConfig
import indent.rainbow.settings.cachedData
import java.util.regex.Pattern
import kotlin.math.min

@Suppress("ArrayInDataClass")
data class LineIndents(val levels: IntArray, val indentSize: Int)

class LineIndentsCalculator(private val file: PsiFile, private val document: Document) {

    private val ignoreLinesStartingWith: Pattern = IrConfig.INSTANCE.cachedData.ignoreLinesStartingWith

    fun compute(): LineIndents {
        val tabsAndSpaces = getTabsAndSpaces()
        val indents = getExplicitIndents(tabsAndSpaces)
        fillIndentsOnEmptyLines(indents.levels)
        return indents
    }

    private fun getExplicitIndents(tabsAndSpaces: Array<LineIndentInfo>): LineIndents {
        val indentOptions = CodeStyle.getIndentOptions(file)
        val useTabs = indentOptions.USE_TAB_CHARACTER
        val indentSize = indentOptions.INDENT_SIZE.ifNotPositive { 4 }
        return if (useTabs) {
            val indents = tabsAndSpaces.mapToIntArray { it.tabs }
            LineIndents(indents, 1)
        } else {
            val fileText = document.charsSequence
            val indents = tabsAndSpaces.mapToIntArray {
                if (it.tabs != 0 || it.spaces != it.totalIndent) {
                    -1  // line with mixed indent
                } else if (it.spaces % indentSize == 0 || shouldIgnoreLine(it, fileText)) {
                    it.spaces / indentSize
                } else {
                    -1  // line with incorrect indent
                }
            }
            LineIndents(indents, indentSize)
        }
    }

    private fun shouldIgnoreLine(info: LineIndentInfo, fileText: CharSequence): Boolean {
        val lineText = fileText.subSequence(info.startOffset + info.totalIndent, fileText.length)
        return ignoreLinesStartingWith.matcher(lineText).lookingAt()
    }

    private fun fillIndentsOnEmptyLines(indents: IntArray) {
        fun isEmptyLine(line: Int): Boolean =
            indents[line] == 0 && document.getLineStartOffset(line) == document.getLineEndOffset(line)

        for (lineStart in 1..indents.size - 2) {
            val prev = indents[lineStart - 1]
            if (!isEmptyLine(lineStart) || prev <= 0) continue
            var lineEnd = lineStart + 1
            while (lineEnd < indents.size && isEmptyLine(lineEnd)) {
                ++lineEnd
            }
            val next = indents.getOrElse(lineEnd) { 0 }
            if (next > 0) {
                for (line in lineStart until lineEnd) {
                    indents[line] = min(prev, next)
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
