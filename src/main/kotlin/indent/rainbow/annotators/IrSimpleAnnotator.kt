package indent.rainbow.annotators

import com.intellij.application.options.CodeStyle
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import indent.rainbow.IrColors
import indent.rainbow.settings.IrConfig

class IrSimpleAnnotator : Annotator, DumbAware {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (!config.enabled || config.useFormatterBasedAnnotator) return
        if (element !is PsiWhiteSpace) return

        val project = element.project
        val file = element.containingFile
        val document = PsiDocumentManager.getInstance(project).getDocument(file) ?: return

        val indentOptions = CodeStyle.getIndentOptions(file)
        val useTabs = indentOptions.USE_TAB_CHARACTER
        val tabSize = indentOptions.TAB_SIZE

        val range = element.textRange
        var startLine = document.getLineNumber(range.startOffset)
        val endLine = document.getLineNumber(range.endOffset)
        if (document.getLineStartOffset(startLine) != range.startOffset) {
            ++startLine
        }

        for (line in startLine..endLine) {
            val highlightStartOffset = document.getLineStartOffset(line)
            var highlightEndOffset = if (line < endLine) {
                document.getLineEndOffset(line)
            } else {
                range.endOffset
            }
            if (highlightStartOffset == highlightEndOffset) continue

            val highlightText = document.getText(TextRange(highlightStartOffset, highlightEndOffset))
            val okSpaces = !useTabs
                    && highlightText.chars().allMatch { it == ' '.toInt() }
                    && (highlightEndOffset - highlightStartOffset) % tabSize == 0
            val okTabs = useTabs && highlightText.chars().allMatch { it == '\t'.toInt() }
            if (okSpaces || okTabs || config.disableErrorHighlighting) {
                if (config.disableErrorHighlighting && !useTabs) {
                    highlightEndOffset -= (highlightEndOffset - highlightStartOffset) % tabSize
                }

                val step = if (useTabs) 1 else tabSize
                for (i in highlightStartOffset until highlightEndOffset step step) {
                    val textAttributes = IrColors.getTextAttributes((i - highlightStartOffset) / step)
                    highlight(holder, i, i + step, textAttributes)
                }
            } else {
                highlight(holder, highlightStartOffset, highlightEndOffset, IrColors.getErrorTextAttributes())
            }
        }
    }

    private fun highlight(holder: AnnotationHolder, start: Int, end: Int, textAttributes: TextAttributesKey) {
        val highlightRange = TextRange(start, end)
        val annotation = holder.createInfoAnnotation(highlightRange, null)
        annotation.textAttributes = textAttributes
    }

    companion object {
        val instance: IrSimpleAnnotator = IrSimpleAnnotator()
        private val config = IrConfig.instance
    }
}
