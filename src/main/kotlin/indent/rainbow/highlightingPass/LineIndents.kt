package indent.rainbow.highlightingPass

import com.intellij.application.options.CodeStyle
import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiFile
import indent.rainbow.ifNotPositive
import indent.rainbow.mapToIntArray
import kotlin.math.min

@Suppress("ArrayInDataClass")
data class LineIndents(val levels: IntArray, val indentSize: Int)

class LineIndentsCalculator(private val file: PsiFile, private val document: Document) {
    fun compute(): LineIndents {
        val tabsAndSpaces = getTabsAndSpaces()
        val indents = getExplicitIndents(tabsAndSpaces)
        fillIndentsOnEmptyLines(indents.levels)
        return indents
    }

    private fun getExplicitIndents(tabsAndSpaces: Array<TabsAndSpaces>): LineIndents {
        val indentOptions = CodeStyle.getIndentOptions(file)
        val useTabs = indentOptions.USE_TAB_CHARACTER
        val indentSize = indentOptions.INDENT_SIZE.ifNotPositive { 4 }
        return if (useTabs) {
            val indents = tabsAndSpaces.mapToIntArray { it.tabs }
            LineIndents(indents, 1)
        } else {
            val indents = tabsAndSpaces.mapToIntArray {
                if (it.tabs == 0 && it.spaces % indentSize == 0) {
                    it.spaces / indentSize
                } else {
                    -1  // line with incorrect indent
                }
            }
            LineIndents(indents, indentSize)
        }
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

    private fun getTabsAndSpaces(): Array<TabsAndSpaces> =
        Array(document.lineCount) { line -> document.getNumberTabsAndSpaces(line) }
}

data class TabsAndSpaces(val tabs: Int, val spaces: Int)

fun Document.getNumberTabsAndSpaces(line: Int): TabsAndSpaces {
    val lineStart = getLineStartOffset(line)
    val fileText = charsSequence

    val numberTabs = fileText.getNumberCharsFrom(lineStart, '\t')
    val numberSpaces = fileText.getNumberCharsFrom(lineStart + numberTabs, ' ')
    return TabsAndSpaces(numberTabs, numberSpaces)
}

private fun CharSequence.getNumberCharsFrom(start: Int, char: Char): Int {
    var offset = start
    while (offset < length && this[offset] == char) {
        ++offset
    }
    return offset - start
}
