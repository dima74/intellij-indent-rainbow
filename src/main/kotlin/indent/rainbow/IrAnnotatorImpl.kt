package indent.rainbow

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import indent.rainbow.annotators.isCommentOrInjectedHost
import indent.rainbow.annotators.isWhiteSpace
import indent.rainbow.settings.IrConfig
import kotlin.math.min

class IrAnnotatorImpl private constructor(
    private val file: PsiFile,
    private val document: Document,
    private val holder: AnnotationHolder,
    private val indentHelper: IrIndentHelper,
    private val useFormatterIndentHelper: Boolean,
) {
    private val useTabs: Boolean = indentHelper.indentOptions.USE_TAB_CHARACTER
    private val tabSize: Int = indentHelper.indentOptions.TAB_SIZE
    private val indentSize: Int = indentHelper.indentOptions.INDENT_SIZE

    fun runForElement(element: PsiElement) {
        val lines = getElementLinesRange(element, document)
        when {
            element.isWhiteSpace() -> runForLines(lines)
            element.isCommentOrInjectedHost() -> runForCommentOrInjectedHost(lines)
        }
    }

    fun runForTextRange(range: TextRange) {
        val lineStart = document.getLineNumber(range.startOffset)
        val lineEnd = document.getLineNumber(range.endOffset)
        runForLines(lineStart..lineEnd)
    }

    fun runForAllLines() {
        val lines = 0 until document.lineCount
        runForLines(lines)
    }

    private fun runForLines(lines: IntRange) {
        for (line in lines) {
            val (indent, alignment) = getLineIndentAndAlignment(line) ?: continue
            highlight(line, indent, alignment)
        }
    }

    private fun runForCommentOrInjectedHost(lines: IntRange) {
        val indentAndAlignment = getLineIndentAndAlignment(lines.first) ?: return
        for (line in lines) {
            val (indent, alignment) = indentAndAlignment
            highlight(line, indent, alignment, forceDisableErrorHighlighting = line > lines.first)
        }
    }

    private fun getLineIndentAndAlignment(line: Int): Pair<Int, Int>? {
        val offset = document.getLineStartOffset(line)
        val element = file.findElementAt(offset) ?: return null
        if (isInsideLanguageInjection(element)) return null
        if (useFormatterIndentHelper && !element.isWhiteSpace()) return null
        // unfortunately spaces in doc comments (at beginning of lines) are PsiWhiteSpace
        if (PsiTreeUtil.getParentOfType(element, PsiComment::class.java, false) != null) return null

        return indentHelper.getIndentAndAlignment(line)
    }

    // https://www.jetbrains.com/help/idea/using-language-injections.html
    private fun isInsideLanguageInjection(element: PsiElement): Boolean {
        val injectionManager = InjectedLanguageManager.getInstance(element.project)
        val injectionHost = injectionManager.getInjectionHost(element)
        return injectionHost != null
    }

    private fun highlight(line: Int, indentSpaces: Int, alignment: Int, forceDisableErrorHighlighting: Boolean = false) {
        val lineStartOffset = document.getLineStartOffset(line)
        val lineEndOffset = document.getLineEndOffset(line)
        val lineText = document.getText(TextRange(lineStartOffset, lineEndOffset))
        if (lineText.isEmpty()) return

        var indent = if (useTabs) indentSpaces / tabSize else indentSpaces
        val prefixExpected = if (useTabs) {
            if (useFormatterIndentHelper && indentSpaces % tabSize != 0) {
                // TODO: Log error with file attached
                debug { "Unexpected indent value: $indentSpaces, tabSize: $tabSize, alignment: $alignment" }
            }

            "\t".repeat(indent) + " ".repeat(alignment)
        } else {
            " ".repeat(indentSpaces + alignment)
        }
        if (prefixExpected.isEmpty()) return

        val prefixActual = lineText.takeWhile { it == ' ' || it == '\t' }

        val disableErrorHighlighting = config.disableErrorHighlighting
                || !useFormatterIndentHelper
                // todo this is actually a workaround,
                //  ideally we should retrieve continuationIndentSize from formatter
                || useTabs && indentSpaces % tabSize != 0
                || forceDisableErrorHighlighting
        val isCorrectIndent = prefixActual == prefixExpected
        if (isCorrectIndent && config.highlightOnlyIncorrectIndent) return
        if (isCorrectIndent || disableErrorHighlighting) {
            if (disableErrorHighlighting) {
                var indentSpacesActual = prefixActual.replace("\t", " ".repeat(tabSize)).length
                indentSpacesActual -= indentSpacesActual % tabSize
                val indentActual = if (useTabs) indentSpacesActual / tabSize else indentSpacesActual
                indent = min(indent, indentActual)
            }

            val step = if (useTabs) 1 else indentSize.also { if (it <= 0) return }
            for (offset in 0 until (indent - indent % step) step step) {
                val start = lineStartOffset + offset
                val end = start + step
                annotate(holder, start, end, offset / step)
            }

            // this can happen for example if indentSize=4 and continuationIndentSize=2  (or if !useFormatterIndentHelper)
            // ideally we should extract such information from formatter
            val lastTabSize = indent % step
            if (lastTabSize != 0) {
                val offset = indent - lastTabSize
                val start = lineStartOffset + offset
                val end = start + lastTabSize
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
        // 2018.2
        @Suppress("DEPRECATION") val annotation = holder.createInfoAnnotation(highlightRange, null)
        annotation.textAttributes = textAttributes
    }

    companion object {
        private val config = IrConfig.INSTANCE

        fun getInstance(file: PsiFile, document: Document, holder: AnnotationHolder, useFormatterIndentHelper: Boolean): IrAnnotatorImpl? {
            val indentHelper = (if (useFormatterIndentHelper) {
                IrFormatterIndentHelper.getInstance(file)
            } else {
                IrSimpleIndentHelper(file, document)
            }) ?: return null
            return IrAnnotatorImpl(file, document, holder, indentHelper, useFormatterIndentHelper)
        }
    }
}

private fun getElementLinesRange(element: PsiElement, document: Document): IntRange {
    val range = element.textRange
    var startOffset = range.startOffset
    val endOffset = range.endOffset
    val charsSequence = document.charsSequence
    while (startOffset < endOffset && charsSequence[startOffset] == '\n') {
        ++startOffset
    }

    val lineStart = document.getLineNumber(startOffset)
    val lineEnd = document.getLineNumber(endOffset)
    return lineStart..lineEnd
}
