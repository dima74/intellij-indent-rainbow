package indent.rainbow

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import indent.rainbow.annotators.isAnnotatorEnabled
import indent.rainbow.settings.IrConfig
import kotlin.math.min

class IrFormatterAnnotatorImpl private constructor(
    private val file: PsiFile,
    private val document: Document,
    private val holder: AnnotationHolder,
    private val indentHelper: IrIndentHelper
) {
    private val useTabs: Boolean = indentHelper.indentOptions.USE_TAB_CHARACTER
    private val tabSize: Int = indentHelper.indentOptions.TAB_SIZE
    private val indentSize: Int = indentHelper.indentOptions.INDENT_SIZE

    fun runForAllLines() {
        val lines = 0 until document.lineCount
        runForLines(lines)
    }

    fun runForLines(lines: IntRange) {
        for (line in lines) {
            runForLine(line)
        }
    }

    private fun runForLine(line: Int) {
        val offset = document.getLineStartOffset(line)
        val element = file.findElementAt(offset) ?: return
        if (!config.isAnnotatorEnabled(element)) return
        // unfortunately spaces in doc comments (at beginning of lines) are PsiWhiteSpace
        if (PsiTreeUtil.getParentOfType(element, PsiComment::class.java, false) != null) return

        val (indent, alignment) = indentHelper.getIndentAndAlignment(offset) ?: return
        debug("line $line:  $indent $alignment")

        highlight(line, indent, alignment)
    }

    private fun highlight(line: Int, indentSpaces: Int, alignment: Int) {
        val lineStartOffset = document.getLineStartOffset(line)
        val lineEndOffset = document.getLineEndOffset(line)
        val lineText = document.getText(TextRange(lineStartOffset, lineEndOffset))
        if (lineText.isEmpty()) return

        var indent = if (useTabs) indentSpaces / tabSize else indentSpaces
        val prefixExpected = if (useTabs) {
            assert(indentSpaces % tabSize == 0) { "indentSpaces: $indentSpaces, tabSize: $tabSize" }
            "\t".repeat(indent) + " ".repeat(alignment)
        } else {
            " ".repeat(indentSpaces + alignment)
        }
        if (prefixExpected.isEmpty()) return

        val prefixActual = lineText.takeWhile { it == ' ' || it == '\t' }

        if (prefixActual == prefixExpected || config.disableErrorHighlighting) {
            if (config.disableErrorHighlighting) {
                var indentSpacesActual = prefixActual.replace("\t", "    ").length
                indentSpacesActual -= indentSpacesActual % tabSize
                val indentActual = if (useTabs) indentSpacesActual / tabSize else indentSpacesActual
                indent = min(indent, indentActual)
            }

            val step = if (useTabs) 1 else indentSize
            for (offset in 0 until indent step step) {
                val start = lineStartOffset + offset
                val end = start + step
                annotate(holder, start, end, offset / step)
            }
        } else {
            val end = lineStartOffset + prefixActual.length
            annotate(holder, lineStartOffset, end, null)
        }
    }

    private fun annotate(holder: AnnotationHolder, start: Int, end: Int, colorIndex: Int?) {
        val textAttributes = if (colorIndex != null) {
            IrColors.getTextAttributes(colorIndex)
        } else {
            IrColors.getErrorTextAttributes()
        }

        val highlightRange = TextRange(start, end)
        val annotation = holder.createInfoAnnotation(highlightRange, null)
        annotation.textAttributes = textAttributes
    }

    companion object {
        private val config = IrConfig.INSTANCE

        fun getInstance(file: PsiFile, document: Document, holder: AnnotationHolder): IrFormatterAnnotatorImpl? {
            val indentHelper = IrIndentHelper.getInstance(file) ?: return null
            return IrFormatterAnnotatorImpl(file, document, holder, indentHelper)
        }
    }
}
