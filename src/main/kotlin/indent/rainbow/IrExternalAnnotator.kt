package indent.rainbow

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import indent.rainbow.settings.IrConfig

@Suppress("RedundantUnitReturnType", "RedundantUnitExpression")
class IrExternalAnnotator : ExternalAnnotator<Unit, Unit>(), DumbAware {

    override fun collectInformation(file: PsiFile): Unit {
        return Unit
    }

    override fun collectInformation(file: PsiFile, editor: Editor, hasErrors: Boolean): Unit {
        return collectInformation(file)
    }

    override fun doAnnotate(collectedInfo: Unit): Unit {
        return Unit
    }

    override fun apply(file: PsiFile, annotationResult: Unit, holder: AnnotationHolder) {
        if (!config.enabled || !config.useFormatterBasedAnnotator) return

        LOG.info("[Indent Rainbow] IrExternalAnnotator::apply")

        val project = file.project
        val document = PsiDocumentManager.getInstance(project).getDocument(file) ?: return

        val indentHelper = IrIndentHelper.getInstance(file) ?: return

        IrExternalAnnotatorImpl(file, document, holder, indentHelper).apply()
    }

    companion object {
        val instance: IrExternalAnnotator = IrExternalAnnotator()
        private val LOG: Logger = Logger.getInstance(IrExternalAnnotator::class.java)
        private val config = IrConfig.instance
    }
}

private class IrExternalAnnotatorImpl(
    val file: PsiFile,
    val document: Document,
    val holder: AnnotationHolder,
    val indentHelper: IrIndentHelper
) {
    val useTabs: Boolean = indentHelper.indentOptions.USE_TAB_CHARACTER
    val tabSize: Int = indentHelper.indentOptions.TAB_SIZE
    val indentSize: Int = indentHelper.indentOptions.INDENT_SIZE

    fun apply() {
        for (line in 0 until document.lineCount) {
            val offset = document.getLineStartOffset(line)
            val element = file.findElementAt(offset) ?: continue
            // we can't check `element is PsiWhiteSpace`, because e.g. in Yaml custom LeafPsiElement is used
            if (!element.text.isBlank()) continue
            // unfortunately spaces in doc comments (at beginning of lines) are PsiWhiteSpace
            if (PsiTreeUtil.getParentOfType(element, PsiComment::class.java, false) != null) continue

            val (indent, alignment) = indentHelper.getIndentAndAlignment(offset) ?: continue
            highlight(line, indent, alignment)
        }
    }

    private fun highlight(line: Int, indentSpaces: Int, alignment: Int) {
        val lineStartOffset = document.getLineStartOffset(line)
        val lineEndOffset = document.getLineEndOffset(line)
        val lineText = document.getText(TextRange(lineStartOffset, lineEndOffset))
        if (lineText.isEmpty()) return

        val indent = if (useTabs) indentSpaces / tabSize else indentSpaces
        val prefixExpected = if (useTabs) {
            assert(indentSpaces % tabSize == 0) { "indentSpaces: $indentSpaces, tabSize: $tabSize" }
            "\t".repeat(indent) + " ".repeat(alignment)
        } else {
            " ".repeat(indentSpaces + alignment)
        }
        if (prefixExpected.isEmpty()) return

        val prefixActual = lineText.takeWhile { it == ' ' || it == '\t' }

        if (prefixActual == prefixExpected) {
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
}
